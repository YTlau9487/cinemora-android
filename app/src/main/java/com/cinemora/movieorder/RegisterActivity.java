package com.cinemora.movieorder;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilUsername, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText usernameInput, emailInput, passwordInput, confirmPasswordInput;
    private CheckBox termsCheckbox;
    private Button btnRegister;
    private TextView tvSignIn;
    private ImageView ivClose;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    private boolean isAuthenticating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        // Initialize Views
        tilUsername = findViewById(R.id.til_username);
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        
        usernameInput = findViewById(R.id.username_input);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        termsCheckbox = findViewById(R.id.terms_checkbox);
        btnRegister = findViewById(R.id.btn_register);
        tvSignIn = findViewById(R.id.tv_signin);
        ivClose = findViewById(R.id.ivClose);

        setupTextWatchers();

        btnRegister.setOnClickListener(v -> registerUser());
        
        tvSignIn.setOnClickListener(v -> {
            if (isAuthenticating) return;
            // Simply finish and go back to login, since register was started from login
            finish();
        });

        if (ivClose != null) {
            ivClose.setOnClickListener(v -> {
                if (isAuthenticating) return;
                handleCloseAction();
            });
        }
    }

    private void handleCloseAction() {
        // Return to Home (MainActivity)
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (isAuthenticating) return;
        // Default behavior: go back to Login (if started from there)
        super.onBackPressed();
    }

    private void setupTextWatchers() {
        TextWatcher commonWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilUsername.setError(null);
                tilEmail.setError(null);
                tilPassword.setError(null);
                tilConfirmPassword.setError(null);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };
        usernameInput.addTextChangedListener(commonWatcher);
        emailInput.addTextChangedListener(commonWatcher);
        passwordInput.addTextChangedListener(commonWatcher);
        confirmPasswordInput.addTextChangedListener(commonWatcher);
    }

    private void registerUser() {
        if (isAuthenticating) return;

        String username = usernameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        boolean isValid = true;

        if (TextUtils.isEmpty(username)) {
            tilUsername.setError("Please enter a username");
            isValid = false;
        }

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Please enter an email address");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Invalid email format");
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Please enter a password");
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            isValid = false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError("Please confirm your password");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match");
            isValid = false;
        }

        if (!isValid) return;

        if (!termsCheckbox.isChecked()) {
            Toast.makeText(this, "Please agree to the Terms & Conditions", Toast.LENGTH_SHORT).show();
            return;
        }

        isAuthenticating = true;
        setLoadingState(true);

        // Check if username is already taken before creating Auth user
        mFirestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        isAuthenticating = false;
                        setLoadingState(false);
                        tilUsername.setError("This username is already taken");
                    } else if (task.isSuccessful()) {
                        // Username is available, proceed with registration
                        createAuthUser(username, email, password);
                    } else {
                        isAuthenticating = false;
                        setLoadingState(false);
                        Toast.makeText(RegisterActivity.this, "Error checking username", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createAuthUser(String username, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        saveUserToFirestore(uid, username, email);
                    } else {
                        isAuthenticating = false;
                        setLoadingState(false);
                        handleRegistrationError(task.getException());
                    }
                });
    }

    private void handleRegistrationError(Exception exception) {
        if (exception instanceof FirebaseAuthUserCollisionException) {
            tilEmail.setError("This email is already registered. Please use another one.");
        } else {
            Toast.makeText(RegisterActivity.this, "Registration failed: " + 
                    (exception != null ? exception.getMessage() : "Unknown error"), 
                    Toast.LENGTH_LONG).show();
        }
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            btnRegister.setEnabled(false);
            btnRegister.setText("Creating Account...");
            btnRegister.setAlpha(0.5f);
        } else {
            btnRegister.setEnabled(true);
            btnRegister.setText("Register");
            btnRegister.setAlpha(1.0f);
        }
    }

    private void saveUserToFirestore(String uid, String username, String email) {
        long currentTimestamp = DateUtils.getCurrentTimestamp();
        User user = new User(uid, username, email, "", 0, currentTimestamp, currentTimestamp);

        mFirestore.collection("users").document(uid)
                .set(user)
                .addOnCompleteListener(task -> {
                    isAuthenticating = false;
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "Account Created Successfully!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        setLoadingState(false);
                        Toast.makeText(RegisterActivity.this, "Error saving profile: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                    }
                });
    }
}