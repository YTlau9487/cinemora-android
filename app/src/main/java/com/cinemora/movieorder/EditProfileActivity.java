package com.cinemora.movieorder;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Activity for editing user profile information.
 */
public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";
    private EditText etFullName, etEmail, etPhone;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initializeViews();
        
        // TASK 4: Fetch existing user data
        fetchUserData();
    }

    private void initializeViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);

        Button btnSaveChanges = findViewById(R.id.btnSaveChanges);
        Button btnCancel = findViewById(R.id.btnCancel);

        if (btnSaveChanges != null) {
            btnSaveChanges.setOnClickListener(v -> {
                // TODO: Implement save logic here
                finish();
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }
    }

    // TASK 4: Implementation of Fetching User Data
    private void fetchUserData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "No user logged in");
            return;
        }

        String uid = currentUser.getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Populate UI elements with data from Firestore
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");
                        String phone = documentSnapshot.getString("phone"); // Assuming 'phone' field exists

                        if (etFullName != null) etFullName.setText(name);
                        if (etEmail != null) etEmail.setText(email);
                        if (etPhone != null) etPhone.setText(phone);
                        
                        Log.d(TAG, "User data fetched and populated");
                    } else {
                        Log.d(TAG, "No user document found for UID: " + uid);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user data", e);
                    Toast.makeText(EditProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                });
    }
}
