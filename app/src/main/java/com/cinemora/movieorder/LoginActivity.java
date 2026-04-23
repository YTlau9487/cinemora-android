package com.cinemora.movieorder;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText emailInput, passwordInput;
    private Button btnLogin;
    private TextView tvSignUp;
    private ImageView ivClose;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private FirebaseManager firebaseManager;

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
        firebaseManager = new FirebaseManager();

        // Initialize Views
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        btnLogin = findViewById(R.id.btn_login);
        tvSignUp = findViewById(R.id.tv_signup);
        ivClose = findViewById(R.id.ivClose);

        setupTextWatchers();

        btnLogin.setOnClickListener(v -> loginUser());
        
        tvSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            // No finish() here, keep login in stack so user can go back to it from register
        });

        if (ivClose != null) {
            ivClose.setOnClickListener(v -> handleCloseAction());
        }
    }

    private void handleCloseAction() {
        // Return to Home (MainActivity)
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // System back button also follows the "Back to Home" logic for consistency
        handleCloseAction();
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
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        boolean isValid = true;

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Please enter your email");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Invalid email format");
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Please enter your password");
            isValid = false;
        }

        if (!isValid) return;

        setLoadingState(true);
        signInWithEmail(email, password);
    }

    private void signInWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        validateCartAndNavigate(uid);
                    } else {
                        setLoadingState(false);
                        handleSignInError(task.getException());
                    }
                });
    }

    private void validateCartAndNavigate(String uid) {
        List<CartItem> cartItems = CartManager.getInstance(this).getCartItems();
        if (cartItems.isEmpty()) {
            fetchUserDataAndNavigate(uid);
            return;
        }

        firebaseManager.getUserOwnedMovieIds(uid, new FirebaseManager.OnOwnedMoviesLoadedListener() {
            @Override
            public void onLoaded(Set<String> ownedIds) {
                cleanCart(ownedIds, uid);
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error validating cart: " + message);
                fetchUserDataAndNavigate(uid);
            }
        });
    }

    private void cleanCart(Set<String> purchasedMovieIds, String uid) {
        CartManager cartManager = CartManager.getInstance(this);
        List<CartItem> cartItems = cartManager.getCartItems();
        boolean itemsRemoved = false;
        List<String> removedNames = new ArrayList<>();

        for (CartItem item : new ArrayList<>(cartItems)) {
            if (purchasedMovieIds.contains(item.getMovieId())) {
                cartManager.removeItem(item.getMovieId());
                removedNames.add(item.getMovieName());
                itemsRemoved = true;
            }
        }

        if (itemsRemoved) {
            String message = "Note: Already owned movies were removed from your cart.";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }

        fetchUserDataAndNavigate(uid);
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