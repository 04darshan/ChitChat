package com.example.chitchat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class splash extends AppCompatActivity {

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        auth = FirebaseAuth.getInstance();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(auth.getCurrentUser() != null) {
                    // User is logged in, go to MainActivity
                    startActivity(new Intent(splash.this, MainActivity.class));
                } else {
                    // User not logged in, go to login screen
                    startActivity(new Intent(splash.this, login.class));
                }
                finish();
            }
        }, 4000); // 4 seconds delay
    }
}
