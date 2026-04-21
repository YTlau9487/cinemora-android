package com.cinemora.movieorder;

import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Activity for displaying movie details and playing trailer previews.
 * Handles Task 2: YouTube Video ID extraction from full URLs.
 */
public class MovieDetailActivity extends AppCompatActivity {

    private static final String TAG = "MovieDetailActivity";
    private FirebaseFirestore db;
    private String movieId;
    private boolean isPurchased = false;
    private boolean dialogShown = false;
    private Movie currentMovie;

    // UI Elements
    private YouTubePlayerView youtubePlayerView;
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

        movieId = getIntent().getStringExtra("MOVIE_ID");
        Log.d(TAG, "Received Movie ID: " + movieId);

        if (movieId == null || movieId.isEmpty()) {
            Toast.makeText(this, "Error: Movie ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        checkPurchaseStatus(); 
        fetchMovieData();      
        fetchMovieVideo();     

        if (btnAddToCart != null) {
            btnAddToCart.setOnClickListener(v -> addToCart());
        }
    }

    private void initViews() {
        youtubePlayerView = findViewById(R.id.youtubePlayerView);
        if (youtubePlayerView != null) {
            getLifecycle().addObserver(youtubePlayerView);
        } else {
            Log.e(TAG, "YouTubePlayerView is null!");
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

        db.collection("orders")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (isFinishing()) return;
                    for (DocumentSnapshot orderDoc : queryDocumentSnapshots) {
                        orderDoc.getReference().collection("orderItems")
                                .whereEqualTo("movieId", movieId)
                                .get()
                                .addOnSuccessListener(itemSnapshots -> {
                                    if (!itemSnapshots.isEmpty()) {
                                        isPurchased = true;
                                        Log.d(TAG, "✓ Purchase Verified for Movie: " + movieId);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking purchase status", e));
    }

    private void fetchMovieData() {
        db.collection("movies").document(movieId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isFinishing()) return;
                    if (documentSnapshot.exists()) {
                        currentMovie = documentSnapshot.toObject(Movie.class);
                        if (currentMovie != null) {
                            bindMovieData(currentMovie);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching movie data", e));
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
                            Log.e(TAG, "Could not extract videoId or youtubePlayerView is null");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching movie video", e));
    }

    private void setupYoutubePlayer(String videoId) {
        youtubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                youTubePlayer.loadVideo(videoId, 0);
            }

            @Override
            public void onCurrentSecond(@NonNull YouTubePlayer youTubePlayer, float second) {
                // If not purchased, stop at 10 seconds for preview
                if (!isPurchased && second >= 10 && !dialogShown) {
                    dialogShown = true;
                    youTubePlayer.pause();
                    showPreviewEndedDialog();
                }
            }
        });
    }

    private void showPreviewEndedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Preview Ended")
                .setMessage("If you want to see the full movie, please purchase it to enjoy!")
                .setCancelable(false)
                .setPositiveButton("Add to Cart", (dialog, which) -> {
                    addToCart();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void bindMovieData(Movie movie) {
        if (tvFilmName != null) tvFilmName.setText(movie.getMovieName());
        if (tvRating != null) tvRating.setText(String.valueOf(movie.getRating()));
        if (tvCategory != null) tvCategory.setText(movie.getGenresString());
        if (tvDuration != null) tvDuration.setText(String.format(Locale.getDefault(), "%dm", movie.getDuration()));
        if (tvOverviewDetails != null) tvOverviewDetails.setText(movie.getOverview());
        if (tvDirectorName != null) tvDirectorName.setText(movie.getDirector());
        if (tvCastName != null) tvCastName.setText(movie.getCastString());
        if (tvLanguage != null) tvLanguage.setText(movie.getLanguage());
        if (tvSubtitle != null) tvSubtitle.setText(movie.getSubtitlesString());
        if (tvResolution != null) tvResolution.setText(movie.getResolution());
        if (tvPrice != null) tvPrice.setText(String.format(Locale.getDefault(), "HKD %d", movie.getCost()));
    }

    private void addToCart() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentMovie == null) return;

        String uid = user.getUid();
        long currentTime = System.currentTimeMillis() / 1000;

        // Construct cart item map as used in SeedDataHelper
        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("movieId", currentMovie.getId());
        cartItem.put("movieName", currentMovie.getMovieName());
        cartItem.put("cost", currentMovie.getCost());
        cartItem.put("quantity", 1);
        cartItem.put("createdAt", currentTime);

        db.collection("carts").document(uid)
                .update("items", FieldValue.arrayUnion(cartItem), "updatedAt", currentTime)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Added to cart!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    // Create cart if not exists
                    Map<String, Object> newCart = new HashMap<>();
                    newCart.put("userId", uid);
                    newCart.put("createdAt", currentTime);
                    newCart.put("updatedAt", currentTime);
                    List<Map<String, Object>> items = new ArrayList<>();
                    items.add(cartItem);
                    newCart.put("items", items);
                    db.collection("carts").document(uid).set(newCart)
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Added to cart!", Toast.LENGTH_SHORT).show());
                });
    }

    /**
     * Helper method to extract the 11-character YouTube Video ID from standard YouTube URLs.
     * Fixed Regex: Removed variable-length lookbehind which is not supported in Android's Regex engine.
     */
    private String extractYoutubeId(String url) {
        if (url == null || url.trim().isEmpty()) return null;
        
        // Revised regex to support various YouTube URL formats without lookbehind issues
        String pattern = "(?:youtube(?:-nocookie)?\\.com/(?:[^/\\n\\s]+/.+/|(?:v|e(?:mbed)?)/|.*[?&]v=)|youtu\\.be/)([a-zA-Z0-9_-]{11})";
        Pattern compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = compiledPattern.matcher(url);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
