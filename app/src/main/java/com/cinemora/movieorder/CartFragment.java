package com.cinemora.movieorder;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

public class CartFragment extends Fragment {

    private boolean isLoaded = false;
    private MaterialButton btnCheckout;

    public CartFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);
        
        btnCheckout = view.findViewById(R.id.btnCheckout);
        if (btnCheckout != null) {
            btnCheckout.setOnClickListener(v -> {
                DialogHelper.showConfirmationDialog(
                        getContext(),
                        "Confirm Order",
                        "Are you sure you want to place this order?",
                        "Confirm",
                        "Cancel",
                        () -> {
                            // Implement actual order logic here
                            Toast.makeText(getContext(), "Order placed successfully!", Toast.LENGTH_SHORT).show();
                        },
                        null
                );
            });
        }

        loadDataIfNeeded();
        return view;
    }

    public void loadDataIfNeeded() {
        if (!isLoaded && getView() != null) {
            // Load cart data from Firebase here
            isLoaded = true;
        }
    }
}