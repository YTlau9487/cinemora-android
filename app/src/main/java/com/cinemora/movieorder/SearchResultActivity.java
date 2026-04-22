package com.cinemora.movieorder;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SearchResultActivity extends AppCompatActivity {

    private static final String TAG = "SearchResultActivity";
    private TextView tvSearchQuery, tvResultsCount, tvSortBy;
    private RecyclerView rvSearchResults;
    private FrameLayout searchBarContainer;
    private View layoutResultsHeader;
    private LinearLayout layoutEmptyState;

    private List<Movie> movieResults = new ArrayList<>();
    private String currentSortMode = "Relevant";
    
    private FirebaseManager firebaseManager;
    private Set<String> ownedMovieIds = new HashSet<>();
    private Set<String> cartMovieIds = new HashSet<>();

    private String lastQuery, lastGenre, lastYear, lastDirector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);

        firebaseManager = new FirebaseManager();
        initViews();
        setupListeners();

        lastQuery = getIntent().getStringExtra("QUERY");
        lastGenre = getIntent().getStringExtra("GENRE");
        lastYear = getIntent().getStringExtra("YEAR");
        lastDirector = getIntent().getStringExtra("DIRECTOR");

        updateHeaderTitle(lastQuery, lastGenre, lastYear, lastDirector);
        
        syncDataAndSearch(lastQuery, lastGenre, lastYear, lastDirector);
    }

    private void initViews() {
        tvSearchQuery = findViewById(R.id.tvSearchQuery);
        tvResultsCount = findViewById(R.id.tvResultsCount);
        tvSortBy = findViewById(R.id.tvSortBy);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        searchBarContainer = findViewById(R.id.searchBarContainer);
        layoutResultsHeader = findViewById(R.id.layoutResultsHeader);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        ImageView ivBack = findViewById(R.id.ivBack);

        ivBack.setOnClickListener(v -> supportFinishAfterTransition());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                supportFinishAfterTransition();
            }
        });

        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupListeners() {
        searchBarContainer.setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this, searchBarContainer, "search_bar_transform");
            startActivity(intent, options.toBundle());
        });

        tvSortBy.setOnClickListener(this::showSortMenu);
    }

    private void syncDataAndSearch(String query, String genre, String year, String director) {
        refreshLocalCartStatus();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            firebaseManager.getUserOwnedMovieIds(user.getUid(), new FirebaseManager.OnOwnedMoviesLoadedListener() {
                @Override
                public void onLoaded(Set<String> movieIds) {
                    ownedMovieIds = movieIds;
                    performDynamicSearch(query, genre, year, director);
                }

                @Override
                public void onError(String message) {
                    performDynamicSearch(query, genre, year, director);
                }
            });
        } else {
            performDynamicSearch(query, genre, year, director);
        }
    }

    private void refreshLocalCartStatus() {
        cartMovieIds.clear();
        List<CartItem> cartItems = CartManager.getInstance(this).getCartItems();
        for (CartItem item : cartItems) {
            cartMovieIds.add(item.getMovieId());
        }
    }

    private void showSortMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add("Relevant");
        popup.getMenu().add("Year (Newest)");
        popup.getMenu().add("Year (Oldest)");
        popup.getMenu().add("Rating");
        popup.getMenu().add("Price (Low-High)");
        popup.getMenu().add("Price (High-Low)");

        popup.setOnMenuItemClickListener(item -> {
            currentSortMode = item.getTitle().toString();
            tvSortBy.setText("Sort by: " + currentSortMode);
            applySorting();
            return true;
        });
        popup.show();
    }

    private void applySorting() {
        if (movieResults.isEmpty()) return;

        switch (currentSortMode) {
            case "Year (Newest)":
                Collections.sort(movieResults, (m1, m2) -> Integer.compare(m2.getReleaseYear(), m1.getReleaseYear()));
                break;
            case "Year (Oldest)":
                Collections.sort(movieResults, (m1, m2) -> Integer.compare(m1.getReleaseYear(), m2.getReleaseYear()));
                break;
            case "Rating":
                Collections.sort(movieResults, (m1, m2) -> Double.compare(m2.getRating(), m1.getRating()));
                break;
            case "Price (Low-High)":
                Collections.sort(movieResults, (m1, m2) -> Integer.compare(m1.getCost(), m2.getCost()));
                break;
            case "Price (High-Low)":
                Collections.sort(movieResults, (m1, m2) -> Integer.compare(m2.getCost(), m1.getCost()));
                break;
            default:
                break;
        }
        refreshList();
    }

    private void updateHeaderTitle(String query, String genre, String year, String director) {
        List<String> tags = new ArrayList<>();
        
        String titleOnly = query;
        if (query != null && query.contains(":")) {
            titleOnly = query.replaceAll("\\b(genre|year|director):\\S+", "").trim().replaceAll("\\s+", " ");
        }

        if (titleOnly != null && !titleOnly.isEmpty()) {
            tags.add(titleOnly);
        }
        if (genre != null && !genre.isEmpty()) {
            tags.add("genre:" + genre.toLowerCase());
        }
        
        if (year != null && !year.isEmpty() && !year.equals("All Years")) {
            String displayYear = year;
            if (year.equals("Before 2020") || year.equals("<2020")) displayYear = "<2020";
            tags.add("year:" + displayYear);
        }
        
        if (director != null && !director.isEmpty()) {
            tags.add("director:" + director.toLowerCase());
        }
        
        if (tags.isEmpty()) {
            tvSearchQuery.setText("All Movies");
        } else {
            tvSearchQuery.setText(String.join(" • ", tags));
        }
    }

    private void performDynamicSearch(String query, String genre, String year, String director) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Query firestoreQuery = db.collection("movies");

        String parsedGenre = genre;
        String parsedYear = year;
        String parsedDirector = director;
        String parsedTitle = query;

        if (query != null && query.contains(":")) {
            String[] parts = query.split("\\s+");
            for (String part : parts) {
                if (part.contains(":")) {
                    String[] kv = part.split(":", 2);
                    if (kv.length == 2) {
                        String key = kv[0].toLowerCase();
                        String val = kv[1];
                        if (key.equals("genre")) parsedGenre = val;
                        else if (key.equals("year")) parsedYear = val;
                        else if (key.equals("director")) parsedDirector = val;
                    }
                }
            }
            parsedTitle = query.replaceAll("\\b(genre|year|director):\\S+", "").trim().replaceAll("\\s+", " ");
        }

        if (parsedGenre != null && !parsedGenre.isEmpty()) {
            firestoreQuery = firestoreQuery.whereArrayContains("genres", 
                    parsedGenre.substring(0, 1).toUpperCase() + parsedGenre.substring(1).toLowerCase());
        }

        if (parsedYear != null && !parsedYear.isEmpty() && !parsedYear.equals("All Years")) {
            if (parsedYear.equals("Before 2020") || parsedYear.equals("<2020")) {
                firestoreQuery = firestoreQuery.whereLessThan("releaseDate", 1577836800L);
            } else {
                try {
                    int y = Integer.parseInt(parsedYear);
                    if (y < 2020) {
                        firestoreQuery = firestoreQuery.whereLessThan("releaseDate", 1577836800L);
                    } else {
                        Calendar cal = Calendar.getInstance();
                        cal.set(y, 0, 1, 0, 0, 0);
                        long start = cal.getTimeInMillis() / 1000;
                        cal.set(y, 11, 31, 23, 59, 59);
                        long end = cal.getTimeInMillis() / 1000;
                        firestoreQuery = firestoreQuery.whereGreaterThanOrEqualTo("releaseDate", start)
                                                       .whereLessThanOrEqualTo("releaseDate", end);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        final String finalTitle = parsedTitle;
        final String finalDirector = parsedDirector;

        firestoreQuery.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Movie> results = queryDocumentSnapshots.toObjects(Movie.class);
            List<Movie> filteredResults = new ArrayList<>();

            for (Movie movie : results) {
                boolean matchesTitle = (finalTitle == null || finalTitle.isEmpty() || 
                        movie.getMovieName().toLowerCase().contains(finalTitle.toLowerCase()));
                
                boolean matchesDirector = (finalDirector == null || finalDirector.isEmpty() || 
                        (movie.getDirector() != null && movie.getDirector().toLowerCase().contains(finalDirector.toLowerCase())));

                if (matchesTitle && matchesDirector) {
                    filteredResults.add(movie);
                }
            }

            movieResults = filteredResults;
            applySorting();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Search failed: " + e.getMessage());
            updateUI();
        });
    }

    private void refreshList() {
        updateUI();
    }

    private void updateUI() {
        if (!movieResults.isEmpty()) {
            layoutResultsHeader.setVisibility(View.VISIBLE);
            rvSearchResults.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
            tvResultsCount.setText(String.format(Locale.getDefault(), "Showing %d results", movieResults.size()));
            
            MovieAdapter adapter = new MovieAdapter(this, movieResults, false);
            adapter.setOwnedMovieIds(ownedMovieIds);
            adapter.setCartMovieIds(cartMovieIds);
            rvSearchResults.setAdapter(adapter);
        } else {
            layoutResultsHeader.setVisibility(View.GONE);
            rvSearchResults.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Task: Refresh purchased and cart status on return
        refreshLocalCartStatus();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            firebaseManager.getUserOwnedMovieIds(user.getUid(), new FirebaseManager.OnOwnedMoviesLoadedListener() {
                @Override
                public void onLoaded(Set<String> movieIds) {
                    ownedMovieIds = movieIds;
                    if (rvSearchResults.getAdapter() instanceof MovieAdapter) {
                        MovieAdapter adapter = (MovieAdapter) rvSearchResults.getAdapter();
                        adapter.setOwnedMovieIds(ownedMovieIds);
                        adapter.setCartMovieIds(cartMovieIds);
                    }
                }
                @Override
                public void onError(String message) {}
            });
        } else if (rvSearchResults.getAdapter() instanceof MovieAdapter) {
            ((MovieAdapter) rvSearchResults.getAdapter()).setCartMovieIds(cartMovieIds);
        }
    }
}
