package com.cinemora.movieorder;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

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
    private boolean isPurchaseCheckDone = false;
    private boolean dialogShown = false;
    private Movie currentMovie;

    private PlayerView playerView;
    private WebView vimeoWebView;
    private ExoPlayer player;
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
        playerView = findViewById(R.id.playerView);
        vimeoWebView = findViewById(R.id.vimeoWebView);
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

        if (btnAddToCart != null) {
            btnAddToCart.setEnabled(false);
            btnAddToCart.setText("Checking...");
            btnAddToCart.setAlpha(0.3f);
        }
        
        setupWebView();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        if (vimeoWebView != null) {
            WebSettings webSettings = vimeoWebView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setMediaPlaybackRequiresUserGesture(false);
            vimeoWebView.setWebViewClient(new WebViewClient());
        }
    }

    private void setupToolbar() {
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void checkPurchaseStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            isPurchaseCheckDone = true;
            updateAddToCartButton();
            return;
        }

        firebaseManager.checkUserOwnsMovie(user.getUid(), movieId, new FirebaseManager.OnPurchaseVerifyListener() {
            @Override
            public void onResult(boolean isOwned) {
                isPurchased = isOwned;
                isPurchaseCheckDone = true;
                updateAddToCartButton();
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error checking purchase status: " + message);
                isPurchaseCheckDone = true;
                updateAddToCartButton();
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

        if (!isPurchaseCheckDone) {
            btnAddToCart.setEnabled(false);
            btnAddToCart.setText("Checking...");
            btnAddToCart.setAlpha(0.3f);
            return;
        }

        if (isPurchased) {
            btnAddToCart.setEnabled(false);
            btnAddToCart.setText("Already Owned");
            btnAddToCart.setAlpha(0.3f);
        } else if (isInCart) {
            btnAddToCart.setEnabled(false);
            btnAddToCart.setText("Already in Cart");
            btnAddToCart.setAlpha(0.3f);
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
                        String videoUrl = snapshots.getDocuments().get(0).getString("movieUrl");
                        if (videoUrl != null && !videoUrl.isEmpty()) {
                            initializePlayer(videoUrl);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Firestore error: " + e.getMessage()));
    }

    private void initializePlayer(String videoUrl) {
        if (videoUrl.contains("vimeo.com") || videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be")) {
            showWebViewVideo(videoUrl);
            return;
        }

        // Direct video link (mp4, m3u8, etc)
        playerView.setVisibility(View.VISIBLE);
        vimeoWebView.setVisibility(View.GONE);

        if (player == null) {
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);
            
            player.addListener(new Player.Listener() {
                @Override
                public void onPlayerError(androidx.media3.common.PlaybackException error) {
                    Log.e(TAG, "ExoPlayer Error: " + error.getMessage(), error);
                    Toast.makeText(MovieDetailActivity.this, "Playback error: " + error.getErrorCodeName(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
                    checkPreviewLimit();
                }

                @OptIn(markerClass = UnstableApi.class)
                @Override
                public void onEvents(Player player, Player.Events events) {
                    if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) || events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                        checkPreviewLimit();
                    }
                }
            });
        }

        MediaItem.Builder mediaItemBuilder = new MediaItem.Builder().setUri(videoUrl);
        if (videoUrl.contains(".m3u8")) {
            mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8);
        } else if (videoUrl.contains(".mpd")) {
            mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD);
        }

        player.setMediaItem(mediaItemBuilder.build());
        player.prepare();
        player.play();
    }

    private void showWebViewVideo(String videoUrl) {
        playerView.setVisibility(View.GONE);
        vimeoWebView.setVisibility(View.VISIBLE);
        
        String embedUrl = videoUrl;
        
        if (videoUrl.contains("vimeo.com")) {
            String vimeoId = extractVimeoId(videoUrl);
            if (vimeoId != null) {
                embedUrl = "https://player.vimeo.com/video/" + vimeoId + "?autoplay=1";
            }
        } else if (videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be")) {
            String youtubeId = extractYoutubeId(videoUrl);
            if (youtubeId != null) {
                embedUrl = "https://www.youtube.com/embed/" + youtubeId + "?autoplay=1";
            }
        }
        
        vimeoWebView.loadUrl(embedUrl);
    }

    private String extractVimeoId(String vimeoUrl) {
        Pattern pattern = Pattern.compile("vimeo\\.com/(?:channels/(?:\\w+/)?|groups/(?:\\w+/)?|album/\\d+/video/|video/|)(\\d+)(?:$|/|\\?)");
        Matcher matcher = pattern.matcher(vimeoUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractYoutubeId(String youtubeUrl) {
        String videoId = null;
        String regex = "^(?:https?:\\/\\/)?(?:www\\.)?(?:m\\.)?(?:youtu\\.be\\/|youtube\\.com\\/(?:(?:watch\\?v=|embed\\/|v\\/|shorts\\/)|(?:[\\w-]+\\/)+(?:[\\w-]+\\/)?))([\\w-]{11})(?:\\S+)?$";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(youtubeUrl);
        if (matcher.find()) {
            videoId = matcher.group(1);
        }
        return videoId;
    }

    private void checkPreviewLimit() {
        if (player == null) return;
        
        long currentPosition = player.getCurrentPosition();
        if (!isPurchased && currentPosition >= 10000 && !dialogShown) { // 10 seconds
            dialogShown = true;
            player.pause();
            showPreviewEndedDialog();
        }
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

    @Override
    protected void onResume() {
        super.onResume();
        checkCartStatus();
        if (vimeoWebView != null) {
            vimeoWebView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
        if (vimeoWebView != null) {
            vimeoWebView.onPause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
        if (vimeoWebView != null) {
            vimeoWebView.destroy();
        }
    }
}
