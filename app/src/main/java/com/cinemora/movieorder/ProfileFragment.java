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

/**
 * Fragment representing the user's profile page.
 * Handles display of user information and navigation to account settings.
 */
public class ProfileFragment extends Fragment {

    private TextView tvProfileName, tvProfileEmail, tvProfileCredit;
    private MaterialButton btnEditProfile, btnLogout;
    private FirebaseManager firebaseManager;
    private FirebaseAuth mAuth;
    private boolean isLoaded = false;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        firebaseManager = new FirebaseManager();

        // Initialize UI components
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfileCredit = view.findViewById(R.id.tvProfileCredit);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnLogout = view.findViewById(R.id.btnLogout);

        // Handle navigation to EditProfileActivity
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), EditProfileActivity.class);
                startActivity(intent);
            });
        }

        // Handle Logout
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
                            // Navigate to LoginActivity and clear backstack
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
        if (!isLoaded && tvProfileName != null) {
            setupUI();
            isLoaded = true;
        }
    }

    private void setupUI() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            tvProfileEmail.setText(currentUser.getEmail());
            
            // Load more data from Firestore
            firebaseManager.getUserData(currentUser.getUid(), new FirebaseManager.OnUserDataLoadedListener() {
                @Override
                public void onLoaded(User user) {
                    if (user != null && isAdded()) {
                        tvProfileName.setText(user.getUsername());
                        tvProfileCredit.setText(user.getCredits() + " pts");
                    }
                }

                @Override
                public void onError(String message) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error loading profile: " + message, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}