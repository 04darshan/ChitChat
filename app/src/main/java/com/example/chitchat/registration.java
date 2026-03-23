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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import de.hdodenhof.circleimageview.CircleImageView;

public class registration extends AppCompatActivity {

    private static final String TAG              = "RegistrationActivity";
    private static final int    PICK_IMAGE       = 10;
    private static final String DEFAULT_AVATAR   =
            "https://firebasestorage.googleapis.com/v0/b/chitchat-e89a5.appspot.com/o/man.png?alt=media&token=48c82302-3f19-482a-870f-0761e3a103c8";
    private static final String EMAIL_PATTERN    = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

    private TextInputLayout   usernameLayout, emailLayout, passwordLayout;
    private TextInputEditText usernameInput, emailInput, passwordInput;
    private MaterialButton    registerButton, loginLink;
    private CircleImageView   profileImage;
    private View              loadingOverlay;

    private FirebaseAuth      auth;
    private FirebaseFirestore db;
    private FirebaseStorage   storage;
    private Uri               selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        auth    = FirebaseAuth.getInstance();
        db      = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        profileImage   = findViewById(R.id.profile_image);
        usernameInput  = findViewById(R.id.rgusername);
        emailInput     = findViewById(R.id.rgEmail);
        passwordInput  = findViewById(R.id.rgPassword);
        registerButton = findViewById(R.id.rgbutton);
        loginLink      = findViewById(R.id.rgtologin);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        // Safely find parent TextInputLayouts
        usernameLayout = findParentLayout(usernameInput);
        emailLayout    = findParentLayout(emailInput);
        passwordLayout = findParentLayout(passwordInput);

        profileImage.setOnClickListener(v -> openImagePicker());
        registerButton.setOnClickListener(v -> attemptRegister());
        loginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, login.class));
            finish();
        });
    }

    private TextInputLayout findParentLayout(View v) {
        if (v.getParent() != null && v.getParent().getParent() instanceof TextInputLayout) {
            return (TextInputLayout) v.getParent().getParent();
        }
        return null;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Profile Photo"), PICK_IMAGE);
    }

    private void attemptRegister() {
        clearErrors();

        String name  = usernameInput.getText() != null ? usernameInput.getText().toString().trim() : "";
        String email = emailInput.getText()    != null ? emailInput.getText().toString().trim()    : "";
        String pass  = passwordInput.getText() != null ? passwordInput.getText().toString()        : "";

        if (TextUtils.isEmpty(name))           { setError(usernameLayout, "Username is required"); return; }
        if (TextUtils.isEmpty(email))          { setError(emailLayout,    "Email is required");    return; }
        if (!email.matches(EMAIL_PATTERN))     { setError(emailLayout,    "Enter a valid email");  return; }
        if (TextUtils.isEmpty(pass))           { setError(passwordLayout, "Password is required"); return; }
        if (pass.length() < 6)                 { setError(passwordLayout, "Min. 6 characters");    return; }

        setLoading(true);

        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult().getUser() == null) {
                setLoading(false);
                String msg = task.getException() != null
                        ? task.getException().getMessage() : "Registration failed";
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                return;
            }

            String uid = task.getResult().getUser().getUid();

            if (selectedImageUri != null) {
                StorageReference ref = storage.getReference()
                        .child("Upload").child(uid).child("profile.jpg");
                ref.putFile(selectedImageUri).addOnCompleteListener(uploadTask -> {
                    if (!uploadTask.isSuccessful()) {
                        setLoading(false);
                        Log.e(TAG, "Image upload failed", uploadTask.getException());
                        Toast.makeText(this, "Image upload failed", Toast.LENGTH_LONG).show();
                        return;
                    }
                    ref.getDownloadUrl().addOnSuccessListener(uri ->
                            saveUserToFirestore(uid, name, email, uri.toString()));
                });
            } else {
                saveUserToFirestore(uid, name, email, DEFAULT_AVATAR);
            }
        });
    }

    /**
     * Saves user document to Firestore  →  users/{uid}
     */
    private void saveUserToFirestore(String uid, String name, String email, String photoUrl) {
        User user = new User(uid, name, email, photoUrl, "Online");

        db.collection("users").document(uid)
                .set(user)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Could not save profile. Try again.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        registerButton.setEnabled(!loading);
        if (loginLink      != null) loginLink.setEnabled(!loading);
        if (loadingOverlay != null)
            loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void clearErrors() {
        if (usernameLayout != null) usernameLayout.setError(null);
        if (emailLayout    != null) emailLayout.setError(null);
        if (passwordLayout != null) passwordLayout.setError(null);
    }

    private void setError(TextInputLayout layout, String msg) {
        if (layout != null) layout.setError(msg);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            profileImage.setImageURI(selectedImageUri);
        }
    }
}