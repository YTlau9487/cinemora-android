package com.cinemora.movieorder;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private TextView tvEmptyOrders;

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
        tvEmptyOrders = view.findViewById(R.id.tvEmptyOrders);

        // Setup RecyclerView
        rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        orderAdapter = new OrderAdapter(getContext(), new ArrayList<>());
        rvOrders.setAdapter(orderAdapter);

        loadDataIfNeeded();
        return view;
    }

    public void loadDataIfNeeded() {
        if (!isLoaded && getView() != null) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                firebaseManager.getUserOrders(currentUser.getUid(), new FirebaseManager.OnOrdersLoadedListener() {
                    @Override
                    public void onLoaded(List<Order> orders) {
                        updateOrdersList(orders);
                    }

                    @Override
                    public void onError(String message) {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Failed to load orders: " + message, Toast.LENGTH_SHORT).show();
                            showEmptyState();
                        }
                    }
                });
            } else {
                showEmptyState();
            }
            isLoaded = true;
        }
    }

    /**
     * ISSUE 5: Fix empty state visibility logic.
     */
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
        if (tvEmptyOrders != null) {
            tvEmptyOrders.setText("No orders yet\nYour purchase history will appear here");
        }
    }

    private void hideEmptyState() {
        if (rvOrders != null) rvOrders.setVisibility(View.VISIBLE);
        if (layoutEmptyOrders != null) layoutEmptyOrders.setVisibility(View.GONE);
    }
}