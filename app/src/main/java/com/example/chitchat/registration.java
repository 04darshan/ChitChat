package com.example.chitchat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import de.hdodenhof.circleimageview.CircleImageView;

public class registration extends AppCompatActivity {
    TextView rtl;
    EditText username, email, pass;
    Button register;
    CircleImageView img;
    ProgressBar progressBar;
    FirebaseAuth auth;
    Uri imguri;
    FirebaseDatabase database;
    FirebaseStorage storage;
    String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
    String imageuri; // This will hold the URL string

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        rtl = findViewById(R.id.rgtologin);
        username = findViewById(R.id.rgusername);
        email = findViewById(R.id.rgEmail);
        pass = findViewById(R.id.rgPassword);
        register = findViewById(R.id.rgbutton);
        img = findViewById(R.id.profile_image);
        progressBar = findViewById(R.id.progressBar);

        register.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            String name = username.getText().toString();
            String Email = email.getText().toString();
            String Pass = pass.getText().toString();
            String status = "Hey I'm using this Application";

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(Email) || TextUtils.isEmpty(Pass)) {
                Toast.makeText(registration.this, "Please enter all details", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                return;
            }

            // ... (rest of the validation)

            auth.createUserWithEmailAndPassword(Email, Pass).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String id = task.getResult().getUser().getUid();
                    DatabaseReference reference = database.getReference().child("user").child(id);
                    StorageReference storageReference = storage.getReference().child("Upload").child(id);

                    if (imguri != null) {
                        storageReference.putFile(imguri).addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                                    imageuri = uri.toString();
                                    // UPDATED: Create User object with image URL string and NO password
                                    User user = new User(id, name, Email, imageuri, status);
                                    saveUserAndProceed(reference, user);
                                });
                            }
                        });
                    } else {
                        // UPDATED: No image was selected
                        imageuri = "https://firebasestorage.googleapis.com/v0/b/chitchat-e89a5.appspot.com/o/man.png?alt=media&token=48c82302-3f19-482a-870f-0761e3a103c8"; // A default image URL
                        User user = new User(id, name, Email, imageuri, status);
                        saveUserAndProceed(reference, user);
                    }
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(registration.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        img.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), 10);
        });
    }

    private void saveUserAndProceed(DatabaseReference reference, User user) {
        reference.setValue(user).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                startActivity(new Intent(registration.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(registration.this, "Error in creating user", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10 && resultCode == RESULT_OK && data != null) {
            imguri = data.getData();
            img.setImageURI(imguri);
        }
    }
}