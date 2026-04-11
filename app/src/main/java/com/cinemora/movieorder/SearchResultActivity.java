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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);

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
        // This ensures the shared element transition plays in reverse
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                supportFinishAfterTransition();
            }
        });

        // Set up the search bar to navigate back to SearchActivity with a shared element animation
        searchBarContainer.setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchActivity.class);
            // Ensure we don't keep multiple instances of SearchActivity in the stack
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            // Create the scene transition for the search bar container
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this, searchBarContainer, "search_bar_transform");
            
            startActivity(intent, options.toBundle());
        });

        // Retrieve and display the search query from the intent
        String query = getIntent().getStringExtra("QUERY");
        if (query != null) {
            tvSearchQuery.setText(query);
        }

        // Initialize RecyclerView for displaying search results
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));

        // For now, since we haven't implemented the actual search logic yet,
        // we'll default to the empty state.
        updateSearchResults(0);
    }

    /**
     * Updates the UI based on the number of results found.
     * @param count The number of search results.
     */
    private void updateSearchResults(int count) {
        if (count > 0) {
            layoutResultsHeader.setVisibility(View.VISIBLE);
            rvSearchResults.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
            tvResultsCount.setText(getString(R.string.search_results_count, count));
        } else {
            layoutResultsHeader.setVisibility(View.GONE);
            rvSearchResults.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        }
    }
}