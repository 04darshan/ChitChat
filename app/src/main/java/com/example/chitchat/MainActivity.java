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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
    private SwipeRefreshLayout swipeRefreshLayout;

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
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        userArrayList = new ArrayList<>();
        useradapter = new Useradapter(MainActivity.this, userArrayList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(useradapter);

        // This listener will now update the UI in real-time
        setupFriendsListener();

        swipeRefreshLayout.setOnRefreshListener(this::fetchFriendsManual);

        logoutMain.setOnClickListener(v -> showLogoutDialog());
        findFriendsButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SearchUsersActivity.class)));
        friendRequestsButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, FriendRequestsActivity.class)));

        setupPresenceSystem();
    }

    // UPDATED: This is now the primary way friends are loaded and kept in sync.
    private void setupFriendsListener() {
        friendsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // When the friends list changes, get the new list of UIDs
                ArrayList<String> friendUids = new ArrayList<>();
                for (DataSnapshot friendSnapshot : snapshot.getChildren()) {
                    friendUids.add(friendSnapshot.getKey());
                }
                // Fetch the details for the new list
                fetchFriendDetails(friendUids, false); // false because this is not a manual refresh
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                updateNoFriendsView();
            }
        });
    }

    // This method is now only for the manual swipe-to-refresh gesture
    private void fetchFriendsManual() {
        friendsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<String> friendUids = new ArrayList<>();
                for (DataSnapshot friendSnapshot : snapshot.getChildren()) {
                    friendUids.add(friendSnapshot.getKey());
                }
                fetchFriendDetails(friendUids, true); // true because it's a manual refresh
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                swipeRefreshLayout.setRefreshing(false);
                updateNoFriendsView();
            }
        });
    }

    private void fetchFriendDetails(ArrayList<String> friendUids, boolean isManualRefresh) {
        if (isManualRefresh) {
            swipeRefreshLayout.setRefreshing(true);
        }
        userArrayList.clear();

        if (friendUids.isEmpty()) {
            useradapter.notifyDataSetChanged();
            updateNoFriendsView();
            if (isManualRefresh) swipeRefreshLayout.setRefreshing(false);
            return;
        }

        final int[] fetchCounter = {0};

        for (String uid : friendUids) {
            usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                    User friend = userSnapshot.getValue(User.class);
                    if (friend != null) {
                        userArrayList.add(friend);
                    }

                    fetchCounter[0]++;
                    if (fetchCounter[0] == friendUids.size()) {
                        Collections.sort(userArrayList, (u1, u2) -> u1.getUsername().compareToIgnoreCase(u2.getUsername()));
                        useradapter.notifyDataSetChanged();
                        updateNoFriendsView();
                        if (isManualRefresh) swipeRefreshLayout.setRefreshing(false);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    fetchCounter[0]++;
                    if (fetchCounter[0] == friendUids.size()) {
                        useradapter.notifyDataSetChanged();
                        updateNoFriendsView();
                        if (isManualRefresh) swipeRefreshLayout.setRefreshing(false);
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
    protected void onDestroy() {
        super.onDestroy();
        if (logoutDialog != null && logoutDialog.isShowing()) {
            logoutDialog.dismiss();
        }
    }
}
