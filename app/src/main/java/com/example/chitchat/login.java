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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Map;

public class login extends AppCompatActivity {

    private TextInputLayout   emailLayout, passwordLayout;
    private TextInputEditText emailInput, passInput;
    private MaterialButton    loginButton, signupLink;
    private View              loadingOverlay;

    private FirebaseAuth      auth;
    private FirebaseFirestore db;

    private static final String EMAIL_PATTERN = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        // Already logged in → skip to main
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
        loadingOverlay = findViewById(R.id.loadingOverlay);

        loginButton.setOnClickListener(v -> attemptLogin());
        signupLink.setOnClickListener(v -> {
            startActivity(new Intent(this, registration.class));
            finish();
        });
    }

    private void attemptLogin() {
        emailLayout.setError(null);
        passwordLayout.setError(null);

        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String pass  = passInput.getText()  != null ? passInput.getText().toString()         : "";

        if (TextUtils.isEmpty(email)) { emailLayout.setError("Email is required"); return; }
        if (!email.matches(EMAIL_PATTERN)) { emailLayout.setError("Enter a valid email"); return; }
        if (TextUtils.isEmpty(pass))  { passwordLayout.setError("Password is required"); return; }
        if (pass.length() < 6)        { passwordLayout.setError("Min. 6 characters"); return; }

        setLoading(true);

        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful() && auth.getCurrentUser() != null) {
                // Mark Online in Firestore on login
                db.collection("users")
                        .document(auth.getCurrentUser().getUid())
                        .set(Map.of("status", "Online"), SetOptions.merge())
                        .addOnCompleteListener(t -> {
                            setLoading(false);
                            startActivity(new Intent(login.this, MainActivity.class));
                            finish();
                        });
            } else {
                setLoading(false);
                String msg = task.getException() != null
                        ? task.getException().getMessage() : "Login failed";
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        if (signupLink   != null) signupLink.setEnabled(!loading);
        if (loadingOverlay != null)
            loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}