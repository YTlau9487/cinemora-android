package com.cinemora.movieorder;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

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
    private TextView tvGreeting;
    private MaterialButton btnSignInHeader, btnProfileHeader;

    // ISSUE: Uncommented Seed Button
    private MaterialButton btnRunSeed;

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

        // ISSUE: Uncommented Seed Button
        btnRunSeed = view.findViewById(R.id.btn_run_seed);
        setupSeedButton();

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
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(R.id.nav_profile);
            }
        });

        // Load Data only if not loaded yet
        loadDataIfNeeded();

        return view;
    }

    private void setupSeedButton() {
        if (btnRunSeed == null) return;

        btnRunSeed.setOnClickListener(v -> {
            btnRunSeed.setEnabled(false);
            btnRunSeed.setText("Seeding...");

            SeedDataHelper.runReseed(getContext(), new SeedDataHelper.OnSeedCompleteListener() {
                @Override
                public void onSuccess(String message) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Success: " + message, Toast.LENGTH_LONG).show();
                        btnRunSeed.setText("Seed Completed (v3)");
                        
                        // Force Sign Out so user isn't stuck as Bob
                        FirebaseAuth.getInstance().signOut();
                        
                        // Refresh Home data
                        isLoaded = false;
                        loadDataIfNeeded();
                        setupHeader();
                    }
                }

                @Override
                public void onFailure(String message) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_LONG).show();
                        btnRunSeed.setEnabled(true);
                        btnRunSeed.setText("Run Seed (v3)");
                    }
                }
            });
        });
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
            btnSignInHeader.setVisibility(View.GONE);
            btnProfileHeader.setVisibility(View.VISIBLE);

            firebaseManager.getUserData(currentUser.getUid(), new FirebaseManager.OnUserDataLoadedListener() {
                @Override
                public void onLoaded(User user) {
                    if (user != null && isAdded()) {
                        tvGreeting.setText("Hi, " + user.getName());
                    }
                }

                @Override
                public void onError(String message) {
                    if (isAdded()) tvGreeting.setText("Hi, User");
                }
            });
        } else {
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