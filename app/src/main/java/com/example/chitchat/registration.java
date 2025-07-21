package com.example.chitchat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
    FirebaseAuth auth;
    Uri imguri;
    FirebaseDatabase database;
    FirebaseStorage storage;
    String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
    String imageuri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ✅ Firebase Initialization (Fix for NullPointerException)
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        // UI bindings
        rtl = findViewById(R.id.rgtologin);
        username = findViewById(R.id.rgusername);
        email = findViewById(R.id.rgEmail);
        pass = findViewById(R.id.rgPassword);
        register = findViewById(R.id.rgbutton);
        img = findViewById(R.id.profile_image);


        rtl.setOnClickListener(v -> {
            Intent intent = new Intent(registration.this, login.class);
            startActivity(intent);
            finish();
        });

        register.setOnClickListener(v -> {
            String name = username.getText().toString();
            String Email = email.getText().toString();
            String Pass = pass.getText().toString();
            String status = "Hey I'm using this Application";

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(Email) || TextUtils.isEmpty(Pass)) {
                Toast.makeText(registration.this, "Please enter the Details", Toast.LENGTH_SHORT).show();
            } else if (!Email.matches(emailPattern)) {
                email.setError("Give Proper Email");
            } else if (Pass.length() < 6) {
                pass.setError("More than 6 characters");
                Toast.makeText(registration.this, "Password Must be longer than 6 characters", Toast.LENGTH_SHORT).show();
            } else {
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
                                        User user = new User(id, name, Email, Pass, img, status); // ✅ Password included
                                        reference.setValue(user).addOnCompleteListener(task2 -> {
                                            if (task2.isSuccessful()) {
                                                startActivity(new Intent(registration.this, MainActivity.class));
                                                finish();
                                            } else {
                                                Toast.makeText(registration.this, "Error In sign up", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    });
                                }
                            });
                        } else {
                            img.setImageResource(R.drawable.man);
                            User user = new User(id, name, Email, Pass, imageuri, status); // ✅ Password included
                            reference.setValue(user).addOnCompleteListener(task3 -> {
                                if (task3.isSuccessful()) {
                                    startActivity(new Intent(registration.this, MainActivity.class));
                                    finish();
                                } else {
                                    Toast.makeText(registration.this, "Error In sign up", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        Toast.makeText(registration.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        img.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), 10);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10 && data != null) {
            imguri = data.getData();
            img.setImageURI(imguri);
        }
    }
}
