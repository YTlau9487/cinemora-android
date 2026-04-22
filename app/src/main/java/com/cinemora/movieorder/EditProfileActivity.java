package com.cinemora.movieorder;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";
    private EditText etFullName, etEmail;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Button btnSaveChanges;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initializeViews();
        fetchUserData();
    }

    private void initializeViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);

        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        Button btnCancel = findViewById(R.id.btnCancel);

        if (btnSaveChanges != null) {
            btnSaveChanges.setOnClickListener(v -> {
                // Task 4: Custom Confirmation Dialog
                DialogHelper.showConfirmationDialog(
                        this,
                        "Save Changes",
                        "Are you sure you want to update your profile?",
                        "Save",
                        "Cancel",
                        this::saveProfileChanges,
                        null
                );
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }
    }

    private void fetchUserData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");
                        if (etFullName != null) etFullName.setText(name);
                        if (etEmail != null) etEmail.setText(email);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    private void saveProfileChanges() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String newName = etFullName.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();

        if (newName.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Task 4: Debouncing
        btnSaveChanges.setEnabled(false);
        btnSaveChanges.setText("Saving...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("email", newEmail);
        updates.put("updatedAt", System.currentTimeMillis() / 1000);

        db.collection("users").document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSaveChanges.setEnabled(true);
                    btnSaveChanges.setText("Save Changes");
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
