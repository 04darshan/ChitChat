package com.example.chitchat;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

/**
 * ENHANCED login.java
 *
 * IMPROVEMENTS:
 * - Uses Material3 TextInputLayout for proper inline error display
 * - Loading state disables the button to prevent double-taps
 * - Cleaner null-safe input reading
 * - Redirects to MainActivity if already logged in
 */
public class login extends AppCompatActivity {

    private TextInputLayout emailLayout, passwordLayout;
    private TextInputEditText emailInput, passInput;
    private MaterialButton loginButton, signupLink;
    private CircularProgressIndicator progressBar;
    private View loadingOverlay;

    private FirebaseAuth auth;
    private static final String EMAIL_PATTERN = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        // If already logged in, skip straight to MainActivity
        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        emailLayout    = findViewById(R.id.emailInputLayout);
        passwordLayout = findViewById(R.id.passwordInputLayout);
        emailInput     = findViewById(R.id.loginEmail);
        passInput      = findViewById(R.id.loginPassword);
        loginButton    = findViewById(R.id.loginButton);
        signupLink     = findViewById(R.id.lgntosignup);
        progressBar    = findViewById(R.id.progressBar);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        loginButton.setOnClickListener(v -> attemptLogin());

        signupLink.setOnClickListener(v -> {
            startActivity(new Intent(login.this, registration.class));
            finish();
        });
    }

    private void attemptLogin() {
        // Clear previous errors
        emailLayout.setError(null);
        passwordLayout.setError(null);

        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String pass  = passInput.getText()  != null ? passInput.getText().toString()         : "";

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email is required");
            return;
        }
        if (!email.matches(EMAIL_PATTERN)) {
            emailLayout.setError("Enter a valid email address");
            return;
        }
        if (TextUtils.isEmpty(pass)) {
            passwordLayout.setError("Password is required");
            return;
        }
        if (pass.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            return;
        }

        setLoading(true);
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            setLoading(false);
            if (task.isSuccessful()) {
                startActivity(new Intent(login.this, MainActivity.class));
                finish();
            } else {
                String msg = task.getException() != null
                        ? task.getException().getMessage() : "Login failed";
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        signupLink.setEnabled(!loading);
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }
}