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
import java.util.Locale;

public class OrderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "EXTRA_ORDER_ID";
    private static final String TAG = "OrderDetailActivity";
    private String orderId;
    private OrderDetailItemAdapter itemAdapter;
    private MaterialToolbar toolbar;
    private RecyclerView rvItems;
    private TextView tvSubtotalValue, tvDiscountValue, tvTotalValue;
    private TextView tvCreditsBeforeValue, tvCreditsUsedValue, tvCreditsAfterValue;
    private TextView tvStatusTitle, tvStatusDesc;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        db = FirebaseFirestore.getInstance();
        orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);

        initializeViews();
        
        if (orderId != null) {
            loadOrderData();
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

        tvSubtotalValue = findViewById(R.id.SubtotalValue);
        tvDiscountValue = findViewById(R.id.DiscountValue);
        tvTotalValue = findViewById(R.id.TotalValue);
        tvCreditsBeforeValue = findViewById(R.id.CreditsBeforeValue);
        tvCreditsUsedValue = findViewById(R.id.CreditsUsedValue);
        tvCreditsAfterValue = findViewById(R.id.CreditsAfterValue);
        tvStatusTitle = findViewById(R.id.StatusTitle);
        tvStatusDesc = findViewById(R.id.StatusDesc);
    }

    private void loadOrderData() {
        db.collection("orders").document(orderId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Order order = documentSnapshot.toObject(Order.class);
                    if (order != null) {
                        // Fix for Task 3: Data Binding for #Order Num and Date
                        String dateStr = DateUtils.formatOrderDate(order.getOrderDate());
                        toolbar.setSubtitle("Order #" + orderId.substring(0, 8).toUpperCase() + " • " + dateStr);

                        tvSubtotalValue.setText(String.format(Locale.US, "HKD %d", order.getSubtotal()));
                        tvDiscountValue.setText(String.format(Locale.US, "- HKD %d", order.getDiscount()));
                        tvTotalValue.setText(String.format(Locale.US, "HKD %d", order.getTotalCost()));

                        tvCreditsBeforeValue.setText(order.getCreditsBefore() + " pts");
                        tvCreditsUsedValue.setText("- " + order.getCreditsUsed() + " pts");
                        tvCreditsAfterValue.setText(order.getCreditsAfter() + " pts");

                        tvStatusTitle.setText("Order " + order.getProgress());
                        tvStatusDesc.setText("Your order progress: " + order.getProgress());
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading order data", e));
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
                .addOnFailureListener(e -> Log.e(TAG, "Error loading order items", e));
    }
}
