package com.cinemora.movieorder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.List;

/**
 * Activity that displays the results of a movie search.
 * It handles shared element transitions for a seamless search experience.
 */
public class SearchResultActivity extends AppCompatActivity {

    // View components
    private TextView tvSearchQuery;
    private RecyclerView rvSearchResults;
    private FrameLayout searchBarContainer;
    private View layoutResultsHeader;
    private TextView tvResultsCount;
    private LinearLayout layoutEmptyState;

    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);

        firebaseManager = new FirebaseManager();

        // Initialize UI components
        tvSearchQuery = findViewById(R.id.tvSearchQuery);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        searchBarContainer = findViewById(R.id.searchBarContainer);
        layoutResultsHeader = findViewById(R.id.layoutResultsHeader);
        tvResultsCount = findViewById(R.id.tvResultsCount);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        ImageView ivBack = findViewById(R.id.ivBack);

        // Handle back button click with transition support
        ivBack.setOnClickListener(v -> {
            supportFinishAfterTransition();
        });

        // Handle system back gesture using modern OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                supportFinishAfterTransition();
            }
        });

        // Set up the search bar to navigate back to SearchActivity with a shared element animation
        searchBarContainer.setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this, searchBarContainer, "search_bar_transform");
            
            startActivity(intent, options.toBundle());
        });

        // Retrieve search parameters
        String query = getIntent().getStringExtra("QUERY");
        String genre = getIntent().getStringExtra("GENRE");
        String year = getIntent().getStringExtra("YEAR");
        String director = getIntent().getStringExtra("DIRECTOR");

        if (query != null && !query.isEmpty()) {
            tvSearchQuery.setText(query);
        } else if (genre != null && !genre.isEmpty()) {
            tvSearchQuery.setText(genre);
        } else {
            tvSearchQuery.setText("Search Results");
        }

        // Initialize RecyclerView
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));

        // Perform Search
        performFirestoreSearch(query, genre, year, director);
    }

    private void performFirestoreSearch(String query, String genre, String year, String director) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Query firestoreQuery = db.collection("movies");

        // Simple filtering (Firestore has limitations on multiple inequality filters)
        if (genre != null && !genre.isEmpty()) {
            firestoreQuery = firestoreQuery.whereEqualTo("genre", genre);
        }
        
        // Note: Full-text search (query) and complex filters usually require Algolia/ElasticSearch
        // For this simple app, we'll fetch results and filter basic matches
        firestoreQuery.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Movie> results = queryDocumentSnapshots.toObjects(Movie.class);
            
            // Client-side filtering for text query if present
            if (query != null && !query.isEmpty()) {
                results.removeIf(movie -> !movie.getMovieName().toLowerCase().contains(query.toLowerCase()));
            }

            updateUI(results);
        }).addOnFailureListener(e -> {
            updateUI(null);
        });
    }

    private void updateUI(List<Movie> results) {
        if (results != null && !results.isEmpty()) {
            layoutResultsHeader.setVisibility(View.VISIBLE);
            rvSearchResults.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
            tvResultsCount.setText("Showing " + results.size() + " results");
            
            MovieAdapter adapter = new MovieAdapter(this, results, false);
            rvSearchResults.setAdapter(adapter);
        } else {
            layoutResultsHeader.setVisibility(View.GONE);
            rvSearchResults.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        }
    }
}