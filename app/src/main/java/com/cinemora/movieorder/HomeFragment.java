package com.cinemora.movieorder;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView rvFeaturedMovies, rvMovies;
    private FrameLayout searchBarContainer;
    
    // Header views
    private TextView tvGreeting, btnSignInHeader, btnProfileHeader;

    private FirebaseManager firebaseManager;
    private boolean isLoaded = false;

    public HomeFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        firebaseManager = new FirebaseManager();

        // Bind views
        rvFeaturedMovies = view.findViewById(R.id.rvFeaturedMovies);
        rvMovies = view.findViewById(R.id.rvMovies);
        searchBarContainer = view.findViewById(R.id.searchBarContainer);
        
        tvGreeting = view.findViewById(R.id.tv_greeting);
        btnSignInHeader = view.findViewById(R.id.btn_sign_in_header);
        btnProfileHeader = view.findViewById(R.id.btn_profile_header);

        // UI Setup
        setupRecyclerViews();

        // Search Bar Click
        searchBarContainer.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SearchActivity.class);
            startActivity(intent);
        });

        // Sign In Button Click
        btnSignInHeader.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), LoginActivity.class));
        });
        
        // Profile Button Click
        btnProfileHeader.setOnClickListener(v -> {
            // Since we are using a BottomNavigationView in MainActivity, 
            // a better way to navigate to the Profile tab is to trigger a selection there.
            // For now, we'll assume the user wants to go to the Profile tab.
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(R.id.nav_profile);
            }
        });

        // Load Data only if not loaded yet
        loadDataIfNeeded();

        return view;
    }

    public void loadDataIfNeeded() {
        if (!isLoaded && rvFeaturedMovies != null) {
            loadMovies();
            isLoaded = true;
        }
    }

    private void setupHeader() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Logged In State: Hide both buttons for a cleaner look
            btnSignInHeader.setVisibility(View.GONE);
            btnProfileHeader.setVisibility(View.GONE);

            // Fetch extra profile data (username)
            firebaseManager.getUserData(currentUser.getUid(), new FirebaseManager.OnUserDataLoadedListener() {
                @Override
                public void onLoaded(User user) {
                    if (user != null && isAdded()) {
                        tvGreeting.setText("Hi, " + user.getUsername());
                    }
                }

                @Override
                public void onError(String message) {
                    if (isAdded()) tvGreeting.setText("Hi, User");
                }
            });
        } else {
            // Logged Out State: Show Sign In button
            tvGreeting.setText("Hi, Guest");
            btnSignInHeader.setVisibility(View.VISIBLE);
            btnProfileHeader.setVisibility(View.GONE);
        }
    }

    private void setupRecyclerViews() {
        rvFeaturedMovies.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );

        rvMovies.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false)
        );
    }

    private void loadMovies() {
        // 1. Load Bestselling Movies
        firebaseManager.getBestSellingMovies(new FirebaseManager.OnMoviesLoadedListener() {
            @Override
            public void onLoaded(List<Movie> movies) {
                if (isAdded()) {
                    MovieAdapter adapter = new MovieAdapter(getContext(), movies, true);
                    rvFeaturedMovies.setAdapter(adapter);
                }
            }

            @Override
            public void onError(String message) {}
        });

        // 2. Load All/Recommended Movies
        firebaseManager.getAllMovies(new FirebaseManager.OnMoviesLoadedListener() {
            @Override
            public void onLoaded(List<Movie> movies) {
                if (isAdded()) {
                    MovieAdapter adapter = new MovieAdapter(getContext(), movies, false);
                    rvMovies.setAdapter(adapter);
                }
            }

            @Override
            public void onError(String message) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        setupHeader();
    }
}
