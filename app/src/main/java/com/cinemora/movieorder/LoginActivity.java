package com.cinemora.movieorder;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText emailInput, passwordInput;
    private Button btnLogin;
    private TextView tvSignUp;
    private View layoutBack;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        // Initialize Views
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        btnLogin = findViewById(R.id.btn_login);
        tvSignUp = findViewById(R.id.tv_signup);
        layoutBack = findViewById(R.id.layoutBack);

        setupTextWatchers();

        btnLogin.setOnClickListener(v -> loginUser());
        
        tvSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });

        if (layoutBack != null) {
            layoutBack.setOnClickListener(v -> finish());
        }
    }

    private void setupTextWatchers() {
        TextWatcher commonWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilEmail.setError(null);
                tilPassword.setError(null);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };
        emailInput.addTextChangedListener(commonWatcher);
        passwordInput.addTextChangedListener(commonWatcher);
    }

    private void loginUser() {
        String input = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        boolean isValid = true;

        if (TextUtils.isEmpty(input)) {
            tilEmail.setError("Please enter your email or username");
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Please enter your password");
            isValid = false;
        }

        if (!isValid) return;

        setLoadingState(true);

        // Check if input is email or username
        if (input.contains("@")) {
            if (!Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
                setLoadingState(false);
                tilEmail.setError("Invalid email format");
                return;
            }
            signInWithEmail(input, password);
        } else {
            signInWithUsername(input, password);
        }
    }

    private void signInWithUsername(String username, String password) {
        mFirestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        String email = null;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            email = document.getString("email");
                            break;
                        }
                        
                        if (email != null) {
                            signInWithEmail(email, password);
                        } else {
                            setLoadingState(false);
                            tilEmail.setError("Error finding account for this username");
                        }
                    } else {
                        setLoadingState(false);
                        tilEmail.setError("Username not found");
                    }
                })
                .addOnFailureListener(e -> {
                    setLoadingState(false);
                    Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void signInWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        fetchUserDataAndNavigate(uid);
                    } else {
                        setLoadingState(false);
                        handleSignInError(task.getException());
                    }
                });
    }

    private void handleSignInError(Exception exception) {
        if (exception instanceof FirebaseAuthInvalidUserException) {
            tilEmail.setError("Account not found. Please register first.");
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            tilPassword.setError("Incorrect password. Please try again.");
        } else {
            Toast.makeText(LoginActivity.this, "Authentication failed: " + 
                    (exception != null ? exception.getMessage() : "Unknown error"), 
                    Toast.LENGTH_LONG).show();
        }
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            btnLogin.setEnabled(false);
            btnLogin.setText("Logging in...");
            btnLogin.setAlpha(0.5f);
        } else {
            btnLogin.setEnabled(true);
            btnLogin.setText("Login");
            btnLogin.setAlpha(1.0f);
        }
    }

    private void fetchUserDataAndNavigate(String uid) {
        mFirestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            Toast.makeText(LoginActivity.this, "Welcome " + user.getName() + "!", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.putExtra("username", user.getName());
                            intent.putExtra("credits", user.getEarnedCredit());

                            startActivity(intent);
                            finish();
                        }
                    } else {
                        setLoadingState(false);
                        Toast.makeText(LoginActivity.this, "User profile not found", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    setLoadingState(false);
                    Toast.makeText(LoginActivity.this, "Error fetching profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}