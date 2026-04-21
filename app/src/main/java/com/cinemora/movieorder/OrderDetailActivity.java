package com.cinemora.movieorder;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for displaying detailed information about a specific order.
 * Shows a list of order items using item_order_detail.xml layout.
 */
public class OrderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "EXTRA_ORDER_ID";
    private static final String TAG = "OrderDetailActivity";
    private String orderId;
    private OrderDetailItemAdapter itemAdapter;
    private MaterialToolbar toolbar;
    private RecyclerView rvItems;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        db = FirebaseFirestore.getInstance();
        
        // Fix for TASK 1: Use consistent key
        orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);

        initializeViews();
        
        if (orderId != null) {
            loadOrderItems();
        } else {
            Toast.makeText(this, "Order ID not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvItems = findViewById(R.id.rvItems);
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        itemAdapter = new OrderDetailItemAdapter();
        rvItems.setAdapter(itemAdapter);
    }

    private void loadOrderItems() {
        db.collection("orders").document(orderId).collection("orderItems")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<CartItem> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        CartItem item = doc.toObject(CartItem.class);
                        items.add(item);
                    }
                    itemAdapter.setItems(items);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading order items", e);
                    Toast.makeText(OrderDetailActivity.this, "Failed to load items", Toast.LENGTH_SHORT).show();
                });
    }
}
