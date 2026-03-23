package com.example.chitchat;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

/**
 * ENHANCED MainActivity.java
 *
 * IMPROVEMENTS:
 * - Empty state is now a LinearLayout (icon + text) matching the new XML
 * - Logout dialog uses the new Material3 dialog layout
 * - Presence system unchanged (was already correct)
 * - Null-check on auth.getUid() throughout
 * - Dialog uses transparent background so rounded corners show correctly
 */
public class MainActivity extends AppCompatActivity {

    // Made package-accessible for Useradapter (reads auth.getUid())
    FirebaseAuth auth;

    private android.widget.ImageButton findFriendsButton, friendRequestsButton;
    private android.view.View logoutBtn;
    private View noFriendsLayout;
    private RecyclerView recyclerView;
    private Useradapter useradapter;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference usersRef, friendsRef;
    private ArrayList<User> userList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Dialog logoutDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        // Guard: not logged in → go to login
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, login.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        firebaseDatabase = FirebaseDatabase.getInstance();
        String uid = auth.getCurrentUser().getUid();
        usersRef  = firebaseDatabase.getReference("user");
        friendsRef = firebaseDatabase.getReference("friends").child(uid);

        // View bindings — using the new Material Toolbar layout
        logoutBtn            = findViewById(R.id.logbtn);
        findFriendsButton    = findViewById(R.id.find_friends_button);
        friendRequestsButton = findViewById(R.id.friend_requests_button);
        noFriendsLayout      = findViewById(R.id.no_friends_text); // now a LinearLayout
        recyclerView         = findViewById(R.id.rcvmain);
        swipeRefreshLayout   = findViewById(R.id.swipe_refresh_layout);

        userList    = new ArrayList<>();
        useradapter = new Useradapter(this, userList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(useradapter);

        setupFriendsListener();

        swipeRefreshLayout.setOnRefreshListener(this::fetchFriendsManual);

        logoutBtn.setOnClickListener(v -> showLogoutDialog());
        findFriendsButton.setOnClickListener(v ->
                startActivity(new Intent(this, SearchUsersActivity.class)));
        friendRequestsButton.setOnClickListener(v ->
                startActivity(new Intent(this, FriendRequestsActivity.class)));

        setupPresenceSystem();
    }

    // -----------------------------------------------------------------------
    // Real-time listener: reloads friend list whenever the friends node changes
    // -----------------------------------------------------------------------
    private void setupFriendsListener() {
        friendsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<String> friendUids = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    friendUids.add(ds.getKey());
                }
                fetchFriendDetails(friendUids, false);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                updateEmptyState();
            }
        });
    }

    // Called by swipe-to-refresh only
    private void fetchFriendsManual() {
        friendsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<String> friendUids = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    friendUids.add(ds.getKey());
                }
                fetchFriendDetails(friendUids, true);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                swipeRefreshLayout.setRefreshing(false);
                updateEmptyState();
            }
        });
    }

    private void fetchFriendDetails(ArrayList<String> friendUids, boolean isManualRefresh) {
        if (isManualRefresh) swipeRefreshLayout.setRefreshing(true);
        userList.clear();

        if (friendUids.isEmpty()) {
            useradapter.notifyDataSetChanged();
            updateEmptyState();
            if (isManualRefresh) swipeRefreshLayout.setRefreshing(false);
            return;
        }

        final int[] counter = {0};
        for (String uid : friendUids) {
            usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User friend = snapshot.getValue(User.class);
                    if (friend != null) userList.add(friend);
                    finishIfDone();
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    finishIfDone();
                }

                private void finishIfDone() {
                    counter[0]++;
                    if (counter[0] == friendUids.size()) {
                        Collections.sort(userList,
                                (a, b) -> a.getUsername().compareToIgnoreCase(b.getUsername()));
                        useradapter.notifyDataSetChanged();
                        updateEmptyState();
                        if (isManualRefresh) swipeRefreshLayout.setRefreshing(false);
                    }
                }
            });
        }
    }

    private void updateEmptyState() {
        if (noFriendsLayout == null) return;
        boolean empty = userList.isEmpty();
        noFriendsLayout.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // -----------------------------------------------------------------------
    // Material3 Logout Dialog
    // -----------------------------------------------------------------------
    private void showLogoutDialog() {
        logoutDialog = new Dialog(this);
        logoutDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        logoutDialog.setContentView(R.layout.dialog_layout);

        // Make dialog background transparent so the card's rounded corners show
        if (logoutDialog.getWindow() != null) {
            logoutDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            logoutDialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        MaterialButton yesBtn = logoutDialog.findViewById(R.id.yslgtmain);
        MaterialButton noBtn  = logoutDialog.findViewById(R.id.nolgtmain);

        yesBtn.setOnClickListener(v -> {
            // Set status to Offline before signing out
            if (auth.getUid() != null) {
                firebaseDatabase.getReference("user").child(auth.getUid())
                        .child("status").setValue("Offline");
            }
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, login.class));
            finish();
        });

        noBtn.setOnClickListener(v -> logoutDialog.dismiss());
        logoutDialog.show();
    }

    // -----------------------------------------------------------------------
    // Presence system — marks user Online/Offline automatically
    // -----------------------------------------------------------------------
    private void setupPresenceSystem() {
        String uid = auth.getUid();
        if (uid == null) return;

        DatabaseReference statusRef = firebaseDatabase.getReference("user").child(uid).child("status");
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");

        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (Boolean.TRUE.equals(connected)) {
                    statusRef.setValue("Online");
                    statusRef.onDisconnect().setValue("Offline");
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