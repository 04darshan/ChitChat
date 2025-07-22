package com.example.chitchat;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ImageView logoutMain;
    FirebaseAuth auth;
    RecyclerView recyclerView;
    Useradapter useradapter;
    FirebaseDatabase firebaseDatabase;
    ArrayList<User> userArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, login.class));
            finish();
            return; // Important to return here
        }

        userArrayList = new ArrayList<>();
        DatabaseReference reference = firebaseDatabase.getReference().child("user");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userArrayList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null && !user.getUid().equals(auth.getUid())) {
                        userArrayList.add(user);
                    }
                }
                useradapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        logoutMain = findViewById(R.id.logbtn);
        recyclerView = findViewById(R.id.rcvmain);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        useradapter = new Useradapter(MainActivity.this, userArrayList);
        recyclerView.setAdapter(useradapter);

        logoutMain.setOnClickListener(v -> {
            Dialog dialog = new Dialog(MainActivity.this);
            dialog.setContentView(R.layout.dialog_layout);
            Button ys = dialog.findViewById(R.id.yslgtmain);
            Button no = dialog.findViewById(R.id.nolgtmain);
            ys.setOnClickListener(v1 -> {
                // NEW: Set status to offline before signing out
                DatabaseReference userStatusRef = firebaseDatabase.getReference().child("user").child(auth.getUid()).child("status");
                userStatusRef.setValue("Offline");

                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(MainActivity.this, login.class));
                finish();
            });
            no.setOnClickListener(v12 -> dialog.dismiss());
            dialog.show();
        });

        // NEW: Call the presence system method
        setupPresenceSystem();
    }

    // NEW: Method to handle online/offline status
    private void setupPresenceSystem() {
        String uid = auth.getUid();
        if (uid == null) return;

        final DatabaseReference userStatusRef = firebaseDatabase.getReference().child("user").child(uid).child("status");
        final DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");

        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    userStatusRef.setValue("Online");
                    userStatusRef.onDisconnect().setValue("Offline");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Could log this error
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // NEW: Set status to "Online" when app is resumed
        if (auth.getCurrentUser() != null) {
            firebaseDatabase.getReference().child("user").child(auth.getUid()).child("status").setValue("Online");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // NEW: Set status to "Offline" when app is paused
        if (auth.getCurrentUser() != null) {
            firebaseDatabase.getReference().child("user").child(auth.getUid()).child("status").setValue("Offline");
        }
    }
}