package com.cinemora.movieorder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for showing order details after checkout.
 * ISSUE 4: Fix navigation for "View My Movies" button.
 */
public class CheckoutSummaryActivity extends AppCompatActivity {

    private FirebaseManager firebaseManager;
    private OrderAdapter orderAdapter;

    private RecyclerView rvPurchasedItems;
    private TextView tvOrderNumValue;
    private TextView tvDateValue;
    private TextView tvSubtotalValue;
    private TextView tvDiscountValue;
    private TextView tvTotalValue;
    private TextView tvCreditsUsedValue;
    private TextView tvRemainingCreditsValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_checkout_summary);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseManager = new FirebaseManager();

        initializeViews();
        
        String orderId = getIntent().getStringExtra("orderId");
        if (orderId != null) {
            loadOrderDetails(orderId);
        } else {
            // Fallback: load all orders if specific ID not provided
            loadOrders();
        }
    }

    private void initializeViews() {
        rvPurchasedItems = findViewById(R.id.rvPurchasedItems);
        tvOrderNumValue = findViewById(R.id.OrderNumValue);
        tvDateValue = findViewById(R.id.DateValue);
        tvSubtotalValue = findViewById(R.id.SubtotalValue);
        tvDiscountValue = findViewById(R.id.DiscountValue);
        tvTotalValue = findViewById(R.id.TotalValue);
        tvCreditsUsedValue = findViewById(R.id.CreditsUsedValue);
        tvRemainingCreditsValue = findViewById(R.id.RemainingCreditsValue);

        MaterialButton btnViewMyMovies = findViewById(R.id.btnViewMyMovies);
        MaterialButton btnBackToHome = findViewById(R.id.btnBackToHome);

        // --- ISSUE 4: Navigate to OrderFragment ---
        btnViewMyMovies.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("ACTION", "OPEN_ORDERS");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        btnBackToHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        // Setup RecyclerView
        rvPurchasedItems.setLayoutManager(new LinearLayoutManager(this));
        orderAdapter = new OrderAdapter(this, new ArrayList<>());
        rvPurchasedItems.setAdapter(orderAdapter);
    }

    private void loadOrderDetails(String orderId) {
        firebaseManager.getOrderDetails(orderId, new FirebaseManager.OnOrderDetailLoadedListener() {
            @Override
            public void onLoaded(Order order) {
                updateUI(order);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(CheckoutSummaryActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(Order order) {
        if (order == null) return;

        tvOrderNumValue.setText("#" + order.getOrderId().substring(0, 8).toUpperCase());
        // FIX: Use formatOrderDate instead of formatDate
        tvDateValue.setText(DateUtils.formatOrderDate(order.getOrderDate()));
        tvSubtotalValue.setText(DateUtils.formatCurrency(order.getSubtotal()));
        tvDiscountValue.setText("- " + DateUtils.formatCurrency(order.getDiscount()));
        tvTotalValue.setText(DateUtils.formatCurrency(order.getTotalCost()));
        tvCreditsUsedValue.setText(order.getCreditsUsed() + " pts");
        tvRemainingCreditsValue.setText(order.getCreditsAfter() + " pts");

        // For summary, we just show this one order in the list
        List<Order> list = new ArrayList<>();
        list.add(order);
        updateOrdersList(list);
    }

    private void loadOrders() {
        firebaseManager.getUserOrders(firebaseManager.getCurrentUserId(), new FirebaseManager.OnOrdersLoadedListener() {
            @Override
            public void onLoaded(List<Order> orders) {
                updateOrdersList(orders);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(CheckoutSummaryActivity.this, "Error loading orders: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateOrdersList(List<Order> orders) {
        orderAdapter = new OrderAdapter(this, orders);
        rvPurchasedItems.setAdapter(orderAdapter);
    }
}