package com.cinemora.movieorder;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class OrderFragment extends Fragment {

    private FirebaseManager firebaseManager;
    private FirebaseAuth mAuth;
    private OrderAdapter orderAdapter;

    private RecyclerView rvOrders;
    private LinearLayout layoutEmptyOrders;
    private SwipeRefreshLayout swipeRefreshLayout;

    private boolean isLoaded = false;

    public OrderFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order, container, false);

        firebaseManager = new FirebaseManager();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        rvOrders = view.findViewById(R.id.rvOrders);
        layoutEmptyOrders = view.findViewById(R.id.layoutEmptyOrders);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        // Setup RecyclerView
        rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        orderAdapter = new OrderAdapter(getContext(), new ArrayList<>());
        rvOrders.setAdapter(orderAdapter);

        // Task: Refresh function in order fragment
        swipeRefreshLayout.setOnRefreshListener(() -> {
            isLoaded = false;
            loadDataIfNeeded();
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.blue_primary);

        loadDataIfNeeded();
        return view;
    }

    public void loadDataIfNeeded() {
        if (getView() == null) return;
        
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            firebaseManager.getUserOrders(currentUser.getUid(), new FirebaseManager.OnOrdersLoadedListener() {
                @Override
                public void onLoaded(List<Order> orders) {
                    if (isAdded()) {
                        updateOrdersList(orders);
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }

                @Override
                public void onError(String message) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to load orders: " + message, Toast.LENGTH_SHORT).show();
                        showEmptyState();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }
            });
            isLoaded = true;
        } else {
            showEmptyState();
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void updateOrdersList(List<Order> orders) {
        if (!isAdded()) return;

        if (orders == null || orders.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
            orderAdapter = new OrderAdapter(getContext(), orders);
            rvOrders.setAdapter(orderAdapter);
        }
    }

    private void showEmptyState() {
        if (rvOrders != null) rvOrders.setVisibility(View.GONE);
        if (layoutEmptyOrders != null) layoutEmptyOrders.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        if (rvOrders != null) rvOrders.setVisibility(View.VISIBLE);
        if (layoutEmptyOrders != null) layoutEmptyOrders.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Task: Auto refresh after user buys things
        isLoaded = false;
        loadDataIfNeeded();
    }
}
