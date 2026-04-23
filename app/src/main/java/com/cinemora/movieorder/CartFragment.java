package com.cinemora.movieorder;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class CartFragment extends Fragment implements CartItemAdapter.OnCartItemActionListener {

    private FirebaseManager firebaseManager;
    private CartManager cartManager;
    private FirebaseAuth mAuth;
    private List<CartItem> cartItems;
    private CartItemAdapter cartAdapter;

    private RecyclerView rvCartItems;
    private LinearLayout layoutEmptyCart;
    private View cardCreditsSection;
    private MaterialCheckBox cbUseCredits;
    private TextView tvCreditsAvailable;
    private TextView tvCartTotal, tvOriginalPrice, tvAfterPurchaseCredit;
    private MaterialButton btnCheckout;
    private View includeBottomBar;
    
    private int userCredits = 0;
    private boolean isCheckingOut = false;

    public CartFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);
        
        firebaseManager = new FirebaseManager();
        cartManager = CartManager.getInstance(getContext());
        mAuth = FirebaseAuth.getInstance();

        rvCartItems = view.findViewById(R.id.rvCartItems);
        layoutEmptyCart = view.findViewById(R.id.layoutEmptyCart);
        cardCreditsSection = view.findViewById(R.id.cardCreditsSection);
        cbUseCredits = view.findViewById(R.id.cbUseCredits);
        tvCreditsAvailable = view.findViewById(R.id.tvCreditsAvailable);
        
        includeBottomBar = view.findViewById(R.id.includeBottomBar);
        tvCartTotal = view.findViewById(R.id.tvCartTotal);
        tvOriginalPrice = view.findViewById(R.id.tvOriginalPrice);
        tvAfterPurchaseCredit = view.findViewById(R.id.tvAfterPurchaseCredit);
        btnCheckout = view.findViewById(R.id.btnCheckout);

        rvCartItems.setLayoutManager(new LinearLayoutManager(getContext()));

        if (btnCheckout != null) {
            btnCheckout.setOnClickListener(v -> {
                if (isCheckingOut) return;

                if (mAuth.getCurrentUser() != null) {
                    showConfirmationDialog();
                } else {
                    DialogHelper.showConfirmationDialog(
                            getContext(),
                            "Sign In Required",
                            "Please sign in to complete your purchase and earn credits.",
                            "Sign In",
                            "Cancel",
                            () -> {
                                Intent intent = new Intent(getActivity(), LoginActivity.class);
                                startActivity(intent);
                            },
                            null
                    );
                }
            });
        }

        cbUseCredits.addOnCheckedStateChangedListener((checkBox, state) -> calculatePricing());

        loadDataIfNeeded();
        return view;
    }

    private void showConfirmationDialog() {
        Dialog dialog = new Dialog(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_custom_confirmation, null);
        dialog.setContentView(dialogView);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView tvTitle = dialogView.findViewById(R.id.dialog_title);
        TextView tvMessage = dialogView.findViewById(R.id.dialog_message);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);

        tvTitle.setText("Confirm Order");
        tvMessage.setText("Are you sure you want to proceed with the checkout? This will create a permanent order record.");

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            btnConfirm.setEnabled(false);
            dialog.dismiss();
            proceedToCheckout();
        });

        dialog.show();
    }

    public void loadDataIfNeeded() {
        if (getView() == null) return;
        cartItems = cartManager.getCartItems();
        updateUI();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            firebaseManager.getUserData(currentUser.getUid(), new FirebaseManager.OnUserDataLoadedListener() {
                @Override
                public void onLoaded(User user) {
                    if (user != null && isAdded()) {
                        userCredits = user.getEarnedCredit();
                        tvCreditsAvailable.setText(userCredits + " pts");
                        
                        if (cbUseCredits != null) {
                            boolean hasCredits = userCredits > 0;
                            cbUseCredits.setEnabled(hasCredits);
                            if (!hasCredits) {
                                cbUseCredits.setChecked(false);
                            }
                        }
                        
                        calculatePricing();
                    }
                }
                @Override
                public void onError(String message) {}
            });
        }
    }

    private void calculatePricing() {
        if (!isAdded()) return;
        
        int subtotal = cartManager.getCartTotal();
        int finalTotal = subtotal;
        
        int creditGain = (int) Math.ceil(subtotal / 10.0);
        // SMART CREDIT LOGIC: Only use as much as needed to cover the subtotal
        int creditsUsed = (cbUseCredits.isChecked() && userCredits > 0) ? Math.min(userCredits, subtotal) : 0;
        
        if (creditsUsed > 0) {
            finalTotal = subtotal - creditsUsed;
            
            tvOriginalPrice.setVisibility(View.VISIBLE);
            tvOriginalPrice.setText(DateUtils.formatCurrency(subtotal));
            tvOriginalPrice.setPaintFlags(tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            tvOriginalPrice.setVisibility(View.GONE);
        }

        tvCartTotal.setText(DateUtils.formatCurrency(finalTotal));
        
        // Explicit Credit Messaging
        int balanceAfterUse = userCredits - creditsUsed;
        String message = "Credit Balance: " + balanceAfterUse + " pts";
        if (creditGain > 0) {
            message += " (+" + creditGain + " pts earned when bought)";
        }
        tvAfterPurchaseCredit.setText(message);
        
        if (cartItems != null && !cartItems.isEmpty()) {
            if (!isCheckingOut) {
                btnCheckout.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#12CFC2")));
                btnCheckout.setEnabled(true);
                btnCheckout.setAlpha(1.0f);
            }
        } else {
            btnCheckout.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#CCCCCC")));
            btnCheckout.setEnabled(false);
            btnCheckout.setAlpha(0.5f);
        }
    }

    private void updateUI() {
        if (!isAdded()) return;

        boolean isCartEmpty = (cartItems == null || cartItems.isEmpty());
        if (isCartEmpty) {
            rvCartItems.setVisibility(View.GONE);
            layoutEmptyCart.setVisibility(View.VISIBLE);
            if (cardCreditsSection != null) cardCreditsSection.setVisibility(View.GONE);
            if (includeBottomBar != null) includeBottomBar.setVisibility(View.GONE);
        } else {
            rvCartItems.setVisibility(View.VISIBLE);
            layoutEmptyCart.setVisibility(View.GONE);
            if (cardCreditsSection != null) cardCreditsSection.setVisibility(View.VISIBLE);
            if (includeBottomBar != null) includeBottomBar.setVisibility(View.VISIBLE);

            cartAdapter = new CartItemAdapter(getContext(), cartItems, this);
            rvCartItems.setAdapter(cartAdapter);
        }
        calculatePricing();
    }

    private void proceedToCheckout() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        isCheckingOut = true;
        btnCheckout.setEnabled(false);
        btnCheckout.setText("Processing...");
        btnCheckout.setAlpha(0.6f);

        firebaseManager.processCheckout(currentUser.getUid(), cartItems, cbUseCredits.isChecked(), new FirebaseManager.OnOrderDetailLoadedListener() {
            @Override
            public void onLoaded(Order order) {
                if (isAdded()) {
                    isCheckingOut = false;
                    cartManager.clearCart();
                    Intent intent = new Intent(getActivity(), CheckoutSummaryActivity.class);
                    intent.putExtra("orderId", order.getOrderId());
                    startActivity(intent);
                    btnCheckout.setText("Checkout");
                    btnCheckout.setEnabled(true);
                    btnCheckout.setAlpha(1.0f);
                    loadDataIfNeeded();
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    isCheckingOut = false;
                    btnCheckout.setEnabled(true);
                    btnCheckout.setText("Checkout");
                    btnCheckout.setAlpha(1.0f);
                    Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onRemoveItem(String movieId) {
        if (isCheckingOut) return;
        cartManager.removeItem(movieId);
        loadDataIfNeeded();
    }

    @Override
    public void onQuantityChanged(String movieId, int newQuantity) {
    }
}
