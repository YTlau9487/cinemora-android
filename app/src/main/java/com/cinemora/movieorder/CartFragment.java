package com.cinemora.movieorder;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
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

public class CartFragment extends Fragment implements CartItemAdapter.OnCartItemActionListener {

    private FirebaseManager firebaseManager;
    private FirebaseAuth mAuth;
    private Cart currentCart;
    private CartItemAdapter cartAdapter;

    private RecyclerView rvCartItems;
    private LinearLayout layoutEmptyCart;
    private CheckBox cbUseCredits;
    private TextView tvCreditsAvailable;
    private TextView tvCreditsAvailableLabel;
    private TextView tvCartTotal;
    private MaterialButton btnCheckout;

    private boolean isLoaded = false;

    public CartFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);
        
        firebaseManager = new FirebaseManager();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        rvCartItems = view.findViewById(R.id.rvCartItems);
        layoutEmptyCart = view.findViewById(R.id.layoutEmptyCart);
        cbUseCredits = view.findViewById(R.id.cbUseCredits);
        tvCreditsAvailable = view.findViewById(R.id.tvCreditsAvailable);
        tvCreditsAvailableLabel = view.findViewById(R.id.tvCreditsAvailableLabel);
        tvCartTotal = view.findViewById(R.id.tvCartTotal);
        btnCheckout = view.findViewById(R.id.btnCheckout);

        // Setup RecyclerView
        rvCartItems.setLayoutManager(new LinearLayoutManager(getContext()));
        cartAdapter = new CartItemAdapter(getContext(), new java.util.ArrayList<>(), this);
        rvCartItems.setAdapter(cartAdapter);

        // Checkout button
        if (btnCheckout != null) {
            btnCheckout.setOnClickListener(v -> proceedToCheckout());
        }

        loadDataIfNeeded();
        return view;
    }

    public void loadDataIfNeeded() {
        if (!isLoaded && getView() != null) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                firebaseManager.getUserCart(currentUser.getUid(), new FirebaseManager.OnCartLoadedListener() {
                    @Override
                    public void onLoaded(Cart cart) {
                        currentCart = cart;
                        updateUI();
                    }

                    @Override
                    public void onError(String message) {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Failed to load cart: " + message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                // Load user's credits
                firebaseManager.getUserData(currentUser.getUid(), new FirebaseManager.OnUserDataLoadedListener() {
                    @Override
                    public void onLoaded(User user) {
                        if (user != null && tvCreditsAvailable != null && isAdded()) {
                            tvCreditsAvailable.setText(DateUtils.formatCurrency(user.getEarnedCredit()));
                        }
                    }

                    @Override
                    public void onError(String message) {
                        // Silently handle error
                    }
                });
            }
            isLoaded = true;
        }
    }

    /**
     * ISSUE 2: Clean Visibility Handling.
     */
    private void updateUI() {
        if (!isAdded()) return;

        boolean isCartEmpty = (currentCart == null || currentCart.isEmpty());

        if (isCartEmpty) {
            rvCartItems.setVisibility(View.GONE);
            layoutEmptyCart.setVisibility(View.VISIBLE);
            
            if (cbUseCredits != null) cbUseCredits.setVisibility(View.GONE);
            if (tvCreditsAvailable != null) tvCreditsAvailable.setVisibility(View.GONE);
            if (tvCreditsAvailableLabel != null) tvCreditsAvailableLabel.setVisibility(View.GONE);
            
            if (tvCartTotal != null) {
                tvCartTotal.setText(DateUtils.formatCurrency(0));
            }
            if (btnCheckout != null) {
                btnCheckout.setEnabled(false);
                btnCheckout.setAlpha(0.5f);
            }
        } else {
            rvCartItems.setVisibility(View.VISIBLE);
            layoutEmptyCart.setVisibility(View.GONE);
            
            if (cbUseCredits != null) cbUseCredits.setVisibility(View.VISIBLE);
            if (tvCreditsAvailable != null) tvCreditsAvailable.setVisibility(View.VISIBLE);
            if (tvCreditsAvailableLabel != null) tvCreditsAvailableLabel.setVisibility(View.VISIBLE);

            // Update adapter
            cartAdapter = new CartItemAdapter(getContext(), currentCart.getItems(), this);
            rvCartItems.setAdapter(cartAdapter);

            // Update total
            if (tvCartTotal != null) {
                tvCartTotal.setText(DateUtils.formatCurrency(currentCart.getCartTotal()));
            }
            if (btnCheckout != null) {
                btnCheckout.setEnabled(true);
                btnCheckout.setAlpha(1.0f);
            }
        }
    }

    /**
     * ISSUE 3: Call checkoutCart to process the order.
     */
    private void proceedToCheckout() {
        if (currentCart == null || currentCart.isEmpty()) {
            Toast.makeText(getContext(), "Your cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCheckout.setEnabled(false);
        btnCheckout.setText("Processing...");

        firebaseManager.checkoutCart(currentUser.getUid(), cbUseCredits.isChecked(), new FirebaseManager.OnOrderDetailLoadedListener() {
            @Override
            public void onLoaded(Order order) {
                if (isAdded()) {
                    Intent intent = new Intent(getActivity(), CheckoutSummaryActivity.class);
                    intent.putExtra("orderId", order.getOrderId());
                    startActivity(intent);
                    
                    // Reset load flag to refresh cart next time
                    isLoaded = false;
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    btnCheckout.setEnabled(true);
                    btnCheckout.setText("Checkout");
                    Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onRemoveItem(String movieId) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            firebaseManager.removeFromCart(currentUser.getUid(), movieId, new FirebaseManager.OnOperationCompleteListener() {
                @Override
                public void onSuccess(String message) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        // Reload cart
                        isLoaded = false;
                        loadDataIfNeeded();
                    }
                }

                @Override
                public void onFailure(String message) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    @Override
    public void onQuantityChanged(String movieId, int newQuantity) {
        // Implement if needed for future quantity updates
    }
}