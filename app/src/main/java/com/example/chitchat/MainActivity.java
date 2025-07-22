package com.example.chitchat;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

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
import java.util.Collections;

public class MainActivity extends AppCompatActivity {
    ImageView logoutMain;
    ImageButton findFriendsButton, friendRequestsButton;
    TextView noFriendsText;
    FirebaseAuth auth;
    RecyclerView recyclerView;
    Useradapter useradapter;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference usersRef, friendsRef;
    ArrayList<User> userArrayList;
    private Dialog logoutDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, login.class));
            finish();
            return;
        }

        firebaseDatabase = FirebaseDatabase.getInstance();
        usersRef = firebaseDatabase.getReference().child("user");
        friendsRef = firebaseDatabase.getReference().child("friends").child(auth.getUid());

        logoutMain = findViewById(R.id.logbtn);
        findFriendsButton = findViewById(R.id.find_friends_button);
        friendRequestsButton = findViewById(R.id.friend_requests_button);
        noFriendsText = findViewById(R.id.no_friends_text);
        recyclerView = findViewById(R.id.rcvmain);

        userArrayList = new ArrayList<>();
        useradapter = new Useradapter(MainActivity.this, userArrayList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(useradapter);

        fetchFriends();

        logoutMain.setOnClickListener(v -> showLogoutDialog());
        findFriendsButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SearchUsersActivity.class)));
        friendRequestsButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, FriendRequestsActivity.class)));

        setupPresenceSystem();
    }

    // UPDATED: This method is now more robust and less prone to race conditions.
    private void fetchFriends() {
        friendsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Step 1: Get a list of all friend UIDs.
                ArrayList<String> friendUids = new ArrayList<>();
                for (DataSnapshot friendSnapshot : snapshot.getChildren()) {
                    friendUids.add(friendSnapshot.getKey());
                }
                // Step 2: Fetch the details for that list of UIDs.
                fetchFriendDetails(friendUids);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                updateNoFriendsView(); // Handle errors
            }
        });
    }

    private void fetchFriendDetails(ArrayList<String> friendUids) {
        userArrayList.clear(); // Start with a fresh list

        if (friendUids.isEmpty()) {
            useradapter.notifyDataSetChanged();
            updateNoFriendsView();
            return;
        }

        // Use a counter to know when all asynchronous calls are finished.
        final int[] fetchCounter = {0};

        for (String uid : friendUids) {
            usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                    User friend = userSnapshot.getValue(User.class);
                    if (friend != null) {
                        userArrayList.add(friend);
                    }

                    // Check if this is the last friend to be fetched.
                    fetchCounter[0]++;
                    if (fetchCounter[0] == friendUids.size()) {
                        // All friends have been fetched, now update the UI.
                        // Optional: Sort the list alphabetically by username.
                        Collections.sort(userArrayList, (u1, u2) -> u1.getUsername().compareToIgnoreCase(u2.getUsername()));
                        useradapter.notifyDataSetChanged();
                        updateNoFriendsView();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle the case where a user profile might be missing
                    fetchCounter[0]++;
                    if (fetchCounter[0] == friendUids.size()) {
                        useradapter.notifyDataSetChanged();
                        updateNoFriendsView();
                    }
                }
            });
        }
    }


    private void updateNoFriendsView() {
        if (userArrayList.isEmpty()) {
            noFriendsText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            noFriendsText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showLogoutDialog() {
        logoutDialog = new Dialog(MainActivity.this);
        logoutDialog.setContentView(R.layout.dialog_layout);
        Button ys = logoutDialog.findViewById(R.id.yslgtmain);
        Button no = logoutDialog.findViewById(R.id.nolgtmain);
        ys.setOnClickListener(v1 -> {
            if (auth.getUid() != null) {
                DatabaseReference userStatusRef = firebaseDatabase.getReference().child("user").child(auth.getUid()).child("status");
                userStatusRef.setValue("Offline");
            }
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MainActivity.this, login.class));
            finish();
        });
        no.setOnClickListener(v12 -> logoutDialog.dismiss());
        logoutDialog.show();
    }

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
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (auth.getCurrentUser() != null) {
            firebaseDatabase.getReference().child("user").child(auth.getUid()).child("status").setValue("Online");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (auth.getCurrentUser() != null) {
            firebaseDatabase.getReference().child("user").child(auth.getUid()).child("status").setValue("Offline");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (logoutDialog != null && logoutDialog.isShowing()) {
            logoutDialog.dismiss();
        }
    }
}
