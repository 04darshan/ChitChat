package com.example.chitchat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

/**
 * ENHANCED splash.java
 *
 * IMPROVEMENTS:
 * - Uses new Handler(Looper.getMainLooper()) — the no-arg Handler() constructor
 *   is deprecated in API 30+ and can cause subtle threading issues.
 * - EdgeToEdge removed here; use the theme's windowBackground for true splash.
 * - 2.5s delay (was 4s — unnecessarily long for a splash screen).
 */
public class splash extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        FirebaseAuth auth = FirebaseAuth.getInstance();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Class<?> destination = (auth.getCurrentUser() != null)
                    ? MainActivity.class
                    : login.class;
            startActivity(new Intent(splash.this, destination));
            finish();
        }, SPLASH_DELAY_MS);
    }
}