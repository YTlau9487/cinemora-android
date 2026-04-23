package com.cinemora.movieorder;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";
    private EditText etFullName, etEmail;
    private TextInputLayout tilFullName, tilEmail;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private MaterialButton btnSaveChanges;
    
    private String initialName = "";
    private String initialEmail = "";

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
        tilFullName = findViewById(R.id.tilFullName);
        tilEmail = findViewById(R.id.tilEmail);

        View avatarContainer = findViewById(R.id.avatarContainer);
        if (avatarContainer != null) {
            avatarContainer.setOnClickListener(v -> 
                Toast.makeText(this, "Feature not available yet, coming soon.", Toast.LENGTH_SHORT).show()
            );
        }

        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        Button btnCancel = findViewById(R.id.btnCancel);

        updateSaveButtonState(false);

        TextWatcher filterWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilFullName.setError(null);
                tilEmail.setError(null);
                checkIfModified();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etFullName.addTextChangedListener(filterWatcher);
        etEmail.addTextChangedListener(filterWatcher);

        if (btnSaveChanges != null) {
            btnSaveChanges.setOnClickListener(v -> {
                if (validateInputs()) {
                    DialogHelper.showConfirmationDialog(
                            this,
                            "Save Changes",
                            "Are you sure you want to update your profile?",
                            "Save",
                            "Cancel",
                            this::saveProfileChanges,
                            null
                    );
                }
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }
    }

    private boolean validateInputs() {
        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        boolean isValid = true;

        if (TextUtils.isEmpty(name)) {
            tilFullName.setError("Name cannot be empty");
            isValid = false;
        }

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email cannot be empty");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Invalid email format");
            isValid = false;
        }

        return isValid;
    }

    private void checkIfModified() {
        String currentName = etFullName.getText().toString().trim();
        String currentEmail = etEmail.getText().toString().trim();

        boolean isChanged = !currentName.equals(initialName) || !currentEmail.equals(initialEmail);
        boolean isNotEmpty = !currentName.isEmpty() && !currentEmail.isEmpty();

        updateSaveButtonState(isChanged && isNotEmpty);
    }

    private void updateSaveButtonState(boolean enabled) {
        if (btnSaveChanges == null) return;
        
        btnSaveChanges.setEnabled(enabled);
        if (enabled) {
            btnSaveChanges.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#12BFE6")));
            btnSaveChanges.setAlpha(1.0f);
        } else {
            btnSaveChanges.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#BBBBBB")));
            btnSaveChanges.setAlpha(0.6f);
        }
    }

    private void fetchUserData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        initialName = documentSnapshot.getString("name");
                        initialEmail = documentSnapshot.getString("email");
                        
                        if (initialName == null) initialName = "";
                        if (initialEmail == null) initialEmail = "";

                        etFullName.setText(initialName);
                        etEmail.setText(initialEmail);
                        
                        updateSaveButtonState(false);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    private void saveProfileChanges() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String newName = etFullName.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();

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
                    checkIfModified();
                    btnSaveChanges.setText("Save Changes");
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
