package com.cinemora.movieorder;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeFragment extends Fragment {

    private RecyclerView rvFeaturedMovies, rvMovies;
    private FrameLayout searchBarContainer;
    private NestedScrollView homeScrollView;
    private ProgressBar pbHomeLoading;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View layoutHomeError;
    private MaterialButton btnHomeRetry;
    
    private TextView tvGreeting;
    private MaterialButton btnSignInHeader;
    private TextView tvCreditHeader;

    private FirebaseManager firebaseManager;
    private boolean isLoaded = false;
    
    private boolean bestSellingLoaded = false;
    private boolean allMoviesLoaded = false;
    private boolean errorInBestSelling = false;
    private boolean errorInAllMovies = false;

    private MovieAdapter featuredAdapter, mainAdapter;
    private Set<String> ownedMovieIds = new HashSet<>();
    private Set<String> cartMovieIds = new HashSet<>();

    public HomeFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);
        firebaseManager = new FirebaseManager();

        rvFeaturedMovies = view.findViewById(R.id.rvFeaturedMovies);
        rvMovies = view.findViewById(R.id.rvMovies);
        searchBarContainer = view.findViewById(R.id.searchBarContainer);
        tvGreeting = view.findViewById(R.id.tv_greeting);
        btnSignInHeader = view.findViewById(R.id.btn_sign_in_header);
        tvCreditHeader = view.findViewById(R.id.tv_credit_header);
        
        homeScrollView = view.findViewById(R.id.homeScrollView);
        pbHomeLoading = view.findViewById(R.id.pbHomeLoading);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        layoutHomeError = view.findViewById(R.id.layoutHomeError);
        btnHomeRetry = view.findViewById(R.id.btnHomeRetry);

        setupRecyclerViews();

        searchBarContainer.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SearchActivity.class);
            startActivity(intent);
        });

        btnSignInHeader.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), LoginActivity.class));
        });

        btnHomeRetry.setOnClickListener(v -> {
            isLoaded = false;
            loadDataIfNeeded();
        });

        swipeRefreshLayout.setOnRefreshListener(() -> {
            isLoaded = false;
            loadDataIfNeeded();
            setupHeader();
        });

        swipeRefreshLayout.setColorSchemeResources(R.color.blue_primary);

        loadDataIfNeeded();
        return view;
    }

    /**
     * Refreshes data. If not loaded, performs a full fetch. 
     * If already loaded, syncs cart and owned movies to update button states.
     */
    public void loadDataIfNeeded() {
        if (rvFeaturedMovies == null) return;

        if (!isLoaded) {
            showLoadingState();
            fetchOwnedMoviesAndLoad();
            isLoaded = true;
        } else {
            // Task: Auto refresh cart/owned status when switching tabs
            syncCartAndOwnedStatus();
        }
    }

    private void syncCartAndOwnedStatus() {
        // Sync local cart IDs
        cartMovieIds.clear();
        List<CartItem> cartItems = CartManager.getInstance(getContext()).getCartItems();
        for (CartItem item : cartItems) {
            cartMovieIds.add(item.getMovieId());
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            firebaseManager.getUserOwnedMovieIds(user.getUid(), new FirebaseManager.OnOwnedMoviesLoadedListener() {
                @Override
                public void onLoaded(Set<String> movieIds) {
                    ownedMovieIds = movieIds;
                    updateAdapters();
                }

                @Override
                public void onError(String message) {
                    updateAdapters();
                }
            });
        } else {
            updateAdapters();
        }
    }

    private void updateAdapters() {
        if (featuredAdapter != null) {
            featuredAdapter.setOwnedMovieIds(ownedMovieIds);
            featuredAdapter.setCartMovieIds(cartMovieIds);
        }
        if (mainAdapter != null) {
            mainAdapter.setOwnedMovieIds(ownedMovieIds);
            mainAdapter.setCartMovieIds(cartMovieIds);
        }
    }

    private void fetchOwnedMoviesAndLoad() {
        // First sync local cart
        cartMovieIds.clear();
        List<CartItem> cartItems = CartManager.getInstance(getContext()).getCartItems();
        for (CartItem item : cartItems) {
            cartMovieIds.add(item.getMovieId());
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            firebaseManager.getUserOwnedMovieIds(user.getUid(), new FirebaseManager.OnOwnedMoviesLoadedListener() {
                @Override
                public void onLoaded(Set<String> movieIds) {
                    ownedMovieIds = movieIds;
                    loadMovies();
                }

                @Override
                public void onError(String message) {
                    loadMovies(); 
                }
            });
        } else {
            loadMovies();
        }
    }

    private void showLoadingState() {
        if (!swipeRefreshLayout.isRefreshing()) {
            pbHomeLoading.setVisibility(View.VISIBLE);
            homeScrollView.setVisibility(View.GONE);
            layoutHomeError.setVisibility(View.GONE);
        }
    }

    private void checkAllDataLoaded() {
        if (bestSellingLoaded && allMoviesLoaded) {
            pbHomeLoading.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);
            
            if (errorInBestSelling && errorInAllMovies) {
                layoutHomeError.setVisibility(View.VISIBLE);
                homeScrollView.setVisibility(View.GONE);
                isLoaded = false;
            } else {
                layoutHomeError.setVisibility(View.GONE);
                homeScrollView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupHeader() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            btnSignInHeader.setVisibility(View.GONE);
            tvCreditHeader.setVisibility(View.VISIBLE);

            firebaseManager.getUserData(currentUser.getUid(), new FirebaseManager.OnUserDataLoadedListener() {
                @Override
                public void onLoaded(User user) {
                    if (user != null && isAdded()) {
                        tvGreeting.setText("Hi, " + user.getName());
                        tvCreditHeader.setText("Credit: " + user.getEarnedCredit() + " pts");
                        tvCreditHeader.setTextSize(16);
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
            tvCreditHeader.setVisibility(View.GONE);
        }
    }

    private void setupRecyclerViews() {
        rvFeaturedMovies.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvMovies.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
    }

    private void loadMovies() {
        bestSellingLoaded = false;
        allMoviesLoaded = false;
        errorInBestSelling = false;
        errorInAllMovies = false;

        firebaseManager.getBestSellingMovies(new FirebaseManager.OnMoviesLoadedListener() {
            @Override
            public void onLoaded(List<Movie> movies) {
                if (isAdded()) {
                    featuredAdapter = new MovieAdapter(getContext(), movies, true);
                    featuredAdapter.setOwnedMovieIds(ownedMovieIds);
                    featuredAdapter.setCartMovieIds(cartMovieIds);
                    rvFeaturedMovies.setAdapter(featuredAdapter);
                    bestSellingLoaded = true;
                    checkAllDataLoaded();
                }
            }
            @Override
            public void onError(String message) {
                if (isAdded()) {
                    bestSellingLoaded = true;
                    errorInBestSelling = true;
                    checkAllDataLoaded();
                }
            }
        });

        firebaseManager.getAllMovies(new FirebaseManager.OnMoviesLoadedListener() {
            @Override
            public void onLoaded(List<Movie> movies) {
                if (isAdded()) {
                    mainAdapter = new MovieAdapter(getContext(), movies, false);
                    mainAdapter.setOwnedMovieIds(ownedMovieIds);
                    mainAdapter.setCartMovieIds(cartMovieIds);
                    rvMovies.setAdapter(mainAdapter);
                    allMoviesLoaded = true;
                    checkAllDataLoaded();
                }
            }
            @Override
            public void onError(String message) {
                if (isAdded()) {
                    allMoviesLoaded = true;
                    errorInAllMovies = true;
                    checkAllDataLoaded();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        setupHeader();
        // Ensure status is synced on return
        syncCartAndOwnedStatus();
    }
}
