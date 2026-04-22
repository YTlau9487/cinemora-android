package com.cinemora.movieorder;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private TextView tvProfileName, tvProfileEmail, tvProfileCredit, tvProfileOrders;
    private MaterialButton btnEditProfile, btnLogout;
    private FirebaseManager firebaseManager;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isLoaded = false;

    public ProfileFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        firebaseManager = new FirebaseManager();

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfileCredit = view.findViewById(R.id.tvProfileCredit);
        tvProfileOrders = view.findViewById(R.id.tvProfileOrders);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnLogout = view.findViewById(R.id.btnLogout);

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), EditProfileActivity.class);
                startActivity(intent);
            });
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                DialogHelper.showConfirmationDialog(
                        getContext(),
                        "Logout",
                        "Are you sure you want to log out?",
                        "Logout",
                        "Stay",
                        () -> {
                            mAuth.signOut();
                            Toast.makeText(getActivity(), "Logged out", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(getActivity(), LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        },
                        null
                );
            });
        }

        loadDataIfNeeded();
        return view;
    }

    public void loadDataIfNeeded() {
        if (tvProfileName != null) {
            setupUI();
        }
    }

    private void setupUI() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            tvProfileEmail.setText(currentUser.getEmail());
            
            firebaseManager.getUserData(currentUser.getUid(), new FirebaseManager.OnUserDataLoadedListener() {
                @Override
                public void onLoaded(User user) {
                    if (user != null && isAdded()) {
                        tvProfileName.setText(user.getName());
                        tvProfileCredit.setText(user.getEarnedCredit() + " pts");
                    }
                }

                @Override
                public void onError(String message) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error loading profile: " + message, Toast.LENGTH_SHORT).show();
                    }
                }
            });

            // Task 3: Profile Stats - Count actual orders
            db.collection("orders")
                    .whereEqualTo("userId", currentUser.getUid())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (isAdded()) {
                            int count = queryDocumentSnapshots.size();
                            tvProfileOrders.setText(String.valueOf(count));
                        }
                    });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setupUI();
    }
}
