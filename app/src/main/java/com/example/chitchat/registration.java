package com.example.chitchat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * ENHANCED registration.java
 *
 * IMPROVEMENTS:
 * - Uses Material3 TextInputLayout for proper inline error messages
 * - Loading state disables the register button to prevent double-taps
 * - Null-safe input reading via getText() checks
 * - Cleaner validation flow with early returns
 * - Profile image tap area now covers the whole avatar + badge
 */
public class registration extends AppCompatActivity {

    private static final String TAG = "RegistrationActivity";
    private static final int PICK_IMAGE_REQUEST = 10;
    private static final String DEFAULT_AVATAR_URL =
            "https://firebasestorage.googleapis.com/v0/b/chitchat-e89a5.appspot.com/o/man.png?alt=media&token=48c82302-3f19-482a-870f-0761e3a103c8";
    private static final String EMAIL_PATTERN = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

    private TextInputLayout usernameLayout, emailLayout, passwordLayout;
    private TextInputEditText usernameInput, emailInput, passwordInput;
    private MaterialButton registerButton, loginLink;
    private CircleImageView profileImage;
    private View loadingOverlay;

    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private FirebaseStorage storage;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        auth     = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        storage  = FirebaseStorage.getInstance();

        // View bindings
        profileImage   = findViewById(R.id.profile_image);
        usernameInput  = findViewById(R.id.rgusername);
        emailInput     = findViewById(R.id.rgEmail);
        passwordInput  = findViewById(R.id.rgPassword);
        registerButton = findViewById(R.id.rgbutton);
        loginLink      = findViewById(R.id.rgtologin);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        // Find TextInputLayouts (wrap EditTexts) for inline error display
        // They are the direct parents in the new layout
        usernameLayout = (TextInputLayout) usernameInput.getParent().getParent();
        emailLayout    = (TextInputLayout) emailInput.getParent().getParent();
        passwordLayout = (TextInputLayout) passwordInput.getParent().getParent();

        // Profile image picker
        profileImage.setOnClickListener(v -> openImagePicker());

        registerButton.setOnClickListener(v -> attemptRegister());

        loginLink.setOnClickListener(v -> {
            startActivity(new Intent(registration.this, login.class));
            finish();
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Profile Photo"), PICK_IMAGE_REQUEST);
    }

    private void attemptRegister() {
        // Clear previous errors
        if (usernameLayout != null) usernameLayout.setError(null);
        if (emailLayout    != null) emailLayout.setError(null);
        if (passwordLayout != null) passwordLayout.setError(null);

        String name  = usernameInput.getText() != null ? usernameInput.getText().toString().trim() : "";
        String email = emailInput.getText()    != null ? emailInput.getText().toString().trim()    : "";
        String pass  = passwordInput.getText() != null ? passwordInput.getText().toString()        : "";

        // Validate
        if (TextUtils.isEmpty(name)) {
            if (usernameLayout != null) usernameLayout.setError("Username is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            if (emailLayout != null) emailLayout.setError("Email is required");
            return;
        }
        if (!email.matches(EMAIL_PATTERN)) {
            if (emailLayout != null) emailLayout.setError("Enter a valid email address");
            return;
        }
        if (TextUtils.isEmpty(pass)) {
            if (passwordLayout != null) passwordLayout.setError("Password is required");
            return;
        }
        if (pass.length() < 6) {
            if (passwordLayout != null) passwordLayout.setError("Password must be at least 6 characters");
            return;
        }

        setLoading(true);

        final String status = "Hey I'm using ChitChat!";

        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult().getUser() == null) {
                setLoading(false);
                String msg = task.getException() != null
                        ? task.getException().getMessage() : "Registration failed";
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                return;
            }

            String uid = task.getResult().getUser().getUid();
            DatabaseReference userRef = database.getReference("user").child(uid);

            if (selectedImageUri != null) {
                // Upload profile picture to Firebase Storage
                StorageReference storageRef = storage.getReference()
                        .child("Upload").child(uid).child("profile.jpg");

                storageRef.putFile(selectedImageUri).addOnCompleteListener(uploadTask -> {
                    if (!uploadTask.isSuccessful()) {
                        setLoading(false);
                        String errMsg = uploadTask.getException() != null
                                ? uploadTask.getException().getMessage() : "Image upload failed";
                        Log.e(TAG, "Image upload failed", uploadTask.getException());
                        Toast.makeText(this, "Upload failed: " + errMsg, Toast.LENGTH_LONG).show();
                        return;
                    }
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        User user = new User(uid, name, email, uri.toString(), status);
                        saveUserAndProceed(userRef, user);
                    });
                });
            } else {
                // No image selected — use default avatar
                User user = new User(uid, name, email, DEFAULT_AVATAR_URL, status);
                saveUserAndProceed(userRef, user);
            }
        });
    }

    private void saveUserAndProceed(DatabaseReference ref, User user) {
        ref.setValue(user).addOnCompleteListener(task -> {
            setLoading(false);
            if (task.isSuccessful()) {
                startActivity(new Intent(registration.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Could not save user data. Please try again.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        registerButton.setEnabled(!loading);
        if (loginLink != null) loginLink.setEnabled(!loading);
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            profileImage.setImageURI(selectedImageUri);
        }
    }
}