package com.example.chitchat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class splash extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        FirebaseAuth auth = FirebaseAuth.getInstance();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (auth.getCurrentUser() != null) {
                // Mark user Online when app opens
                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(auth.getCurrentUser().getUid())
                        .set(Map.of("status", "Online"), SetOptions.merge());

                startActivity(new Intent(splash.this, MainActivity.class));
            } else {
                startActivity(new Intent(splash.this, login.class));
            }
            finish();
        }, SPLASH_DELAY_MS);
    }
}