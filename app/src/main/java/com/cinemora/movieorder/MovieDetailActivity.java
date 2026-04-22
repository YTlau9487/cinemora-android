package com.cinemora.movieorder;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MovieDetailActivity extends AppCompatActivity {

    private static final String TAG = "MovieDetailActivity";
    private FirebaseFirestore db;
    private FirebaseManager firebaseManager;
    private String movieId;
    private boolean isPurchased = false;
    private boolean isInCart = false;
    private boolean dialogShown = false;
    private Movie currentMovie;

    private YouTubePlayerView youtubePlayerView;
    private YouTubePlayer activeYouTubePlayer;
    private TextView tvFilmName, tvRating, tvCategory, tvDuration, tvOverviewDetails;
    private TextView tvDirectorName, tvCastName, tvLanguage, tvSubtitle, tvResolution, tvPrice;
    private MaterialToolbar toolbar;
    private MaterialButton btnAddToCart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_movie_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        firebaseManager = new FirebaseManager();
        movieId = getIntent().getStringExtra("MOVIE_ID");

        if (movieId == null || movieId.isEmpty()) {
            Toast.makeText(this, "Error: Movie ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        checkPurchaseStatus();
        checkCartStatus();
        fetchMovieData();
        fetchMovieVideo();
    }

    private void initViews() {
        youtubePlayerView = findViewById(R.id.youtubePlayerView);
        if (youtubePlayerView != null) {
            getLifecycle().addObserver(youtubePlayerView);
        }

        tvFilmName = findViewById(R.id.tvFilmName);
        tvRating = findViewById(R.id.tvRating);
        tvCategory = findViewById(R.id.tvCategory);
        tvDuration = findViewById(R.id.tvDuration);
        tvOverviewDetails = findViewById(R.id.tvOverviewDetails);
        tvDirectorName = findViewById(R.id.tvDirectorName);
        tvCastName = findViewById(R.id.tvCastName);
        tvLanguage = findViewById(R.id.tvLanguage);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvResolution = findViewById(R.id.tvResolution);
        tvPrice = findViewById(R.id.tvPrice);
        toolbar = findViewById(R.id.toolbar);
        btnAddToCart = findViewById(R.id.btnAddToCart);
    }

    private void setupToolbar() {
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void checkPurchaseStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        firebaseManager.checkUserOwnsMovie(user.getUid(), movieId, new FirebaseManager.OnPurchaseVerifyListener() {
            @Override
            public void onResult(boolean isOwned) {
                isPurchased = isOwned;
                updateAddToCartButton();
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error checking purchase status: " + message);
            }
        });
    }

    private void checkCartStatus() {
        List<CartItem> cartItems = CartManager.getInstance(this).getCartItems();
        isInCart = false;
        for (CartItem item : cartItems) {
            if (item.getMovieId().equals(movieId)) {
                isInCart = true;
                break;
            }
        }
        updateAddToCartButton();
    }

    private void updateAddToCartButton() {
        if (btnAddToCart == null) return;

        if (isPurchased) {
            btnAddToCart.setEnabled(false);
            btnAddToCart.setText("Already Owned");
            btnAddToCart.setAlpha(0.6f);
        } else if (isInCart) {
            btnAddToCart.setEnabled(false);
            btnAddToCart.setText("Already in Cart");
            btnAddToCart.setAlpha(0.6f);
        } else {
            btnAddToCart.setEnabled(true);
            btnAddToCart.setText("Add to Cart");
            btnAddToCart.setAlpha(1.0f);
            btnAddToCart.setOnClickListener(v -> addToCart());
        }
    }

    private void fetchMovieData() {
        db.collection("movies").document(movieId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isFinishing()) return;
                    if (documentSnapshot.exists()) {
                        currentMovie = documentSnapshot.toObject(Movie.class);
                        if (currentMovie != null) {
                            currentMovie.setId(documentSnapshot.getId());
                            bindMovieData(currentMovie);
                        }
                    }
                });
    }

    private void fetchMovieVideo() {
        db.collection("moviePreviews")
                .whereEqualTo("movieId", movieId)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (isFinishing()) return;
                    if (!snapshots.isEmpty()) {
                        String fullUrl = snapshots.getDocuments().get(0).getString("movieUrl");
                        String videoId = extractYoutubeId(fullUrl);
                        if (videoId != null && youtubePlayerView != null) {
                            setupYoutubePlayer(videoId);
                        } else {
                            Log.e(TAG, "Failed to extract Video ID from: " + fullUrl);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Firestore error: " + e.getMessage()));
    }

    /**
     * Tweak internal WebView to bypass Error 152-4 (Unsupported Browser).
     * This forces a standard mobile User-Agent that YouTube trusts.
     */
    private void findAndTweakWebView(View view) {
        if (view instanceof WebView) {
            WebView webView = (WebView) view;
            WebSettings settings = webView.getSettings();
            // Force a modern Mobile User-Agent to bypass YouTube's WebView blocking
            settings.setUserAgentString("Mozilla/5.0 (Linux; Android 13; SM-S901B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36");
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setMediaPlaybackRequiresUserGesture(false);
            Log.d(TAG, "WebView settings tweaked for YouTube compatibility.");
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                findAndTweakWebView(group.getChildAt(i));
            }
        }
    }

    private void setupYoutubePlayer(String videoId) {
        // Tweak internal WebView BEFORE initialization
        findAndTweakWebView(youtubePlayerView);

        IFramePlayerOptions options = new IFramePlayerOptions.Builder()
                .controls(1)
                .autoplay(0)
                // Use the exact domain with trailing slash for better compatibility
                .origin("https://www.youtube.com/")
                .build();

        youtubePlayerView.initialize(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                activeYouTubePlayer = youTubePlayer;
                youTubePlayer.cueVideo(videoId, 0);
            }

            @Override
            public void onCurrentSecond(@NonNull YouTubePlayer youTubePlayer, float second) {
                if (!isPurchased && second >= 10 && !dialogShown) {
                    dialogShown = true;
                    youTubePlayer.pause();
                    showPreviewEndedDialog();
                }
            }

            @Override
            public void onError(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerError error) {
                super.onError(youTubePlayer, error);
                Log.e(TAG, "YouTube Player Error: " + error.name());
                if (error == PlayerConstants.PlayerError.VIDEO_NOT_FOUND || 
                    error == PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER) {
                    Toast.makeText(MovieDetailActivity.this, "This trailer is currently unavailable.", Toast.LENGTH_SHORT).show();
                }
            }
        }, options);
    }

    private void showPreviewEndedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Preview Ended")
                .setMessage("If you want to see the full movie, please purchase it to enjoy!")
                .setCancelable(false)
                .setPositiveButton("Add to Cart", (dialog, which) -> {
                    if (!isInCart) {
                        addToCart();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void bindMovieData(Movie movie) {
        if (tvFilmName != null) tvFilmName.setText(movie.getMovieName());
        if (tvRating != null) {
            tvRating.setText(String.format(Locale.US, "%.1f", (double) movie.getRating()));
        }
        if (tvCategory != null) tvCategory.setText(movie.getGenresString());
        if (tvDuration != null) {
            tvDuration.setText(formatDuration(movie.getDuration()));
        }
        if (tvOverviewDetails != null) tvOverviewDetails.setText(movie.getOverview());
        if (tvDirectorName != null) tvDirectorName.setText(movie.getDirector());
        if (tvCastName != null) tvCastName.setText(movie.getCastString());
        if (tvLanguage != null) tvLanguage.setText(movie.getLanguage());
        if (tvSubtitle != null) tvSubtitle.setText(movie.getSubtitlesString());
        if (tvResolution != null) tvResolution.setText(movie.getResolution());
        if (tvPrice != null) tvPrice.setText(String.format(Locale.getDefault(), "HKD %d", movie.getCost()));
    }

    private String formatDuration(int totalMinutes) {
        if (totalMinutes <= 0) return "N/A";
        if (totalMinutes < 60) return totalMinutes + "m";
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        if (minutes == 0) return hours + "h";
        return String.format(Locale.getDefault(), "%dh %dm", hours, minutes);
    }

    private void addToCart() {
        if (currentMovie == null || isPurchased || isInCart) {
            return;
        }

        CartItem item = new CartItem(
                currentMovie.getId(),
                currentMovie.getMovieName(),
                currentMovie.getPosterUrl(),
                currentMovie.getCost(),
                1,
                System.currentTimeMillis() / 1000
        );

        CartManager.getInstance(this).addToCart(item);
        Toast.makeText(this, "Added to cart!", Toast.LENGTH_SHORT).show();
        isInCart = true;
        updateAddToCartButton();
    }

    private String extractYoutubeId(String url) {
        if (url == null || url.trim().isEmpty()) return null;
        if (url.length() == 11 && url.matches("[a-zA-Z0-9_-]{11}")) return url;
        String pattern = "(?:youtube(?:-nocookie)?\\.com/(?:[^/\\n\\s]+/.+/|(?:v|e(?:mbed)?)/|.*[?&]v=)|youtu\\.be/)([a-zA-Z0-9_-]{11})";
        Pattern compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = compiledPattern.matcher(url);
        if (matcher.find()) return matcher.group(1);
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkCartStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (youtubePlayerView != null) {
            youtubePlayerView.release();
        }
    }
}
