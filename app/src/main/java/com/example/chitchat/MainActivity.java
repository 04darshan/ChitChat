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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ImageView logoutMain;
    ImageButton findFriendsButton, friendRequestsButton; // UPDATED
    TextView noFriendsText; // NEW
    FirebaseAuth auth;
    RecyclerView recyclerView;
    Useradapter useradapter;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference usersRef, friendsRef; // NEW
    ArrayList<User> userArrayList;
    ArrayList<String> friendUids; // NEW

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        usersRef = firebaseDatabase.getReference().child("user");
        friendsRef = firebaseDatabase.getReference().child("friends").child(auth.getUid());

        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, login.class));
            finish();
            return;
        }

        logoutMain = findViewById(R.id.logbtn);
        findFriendsButton = findViewById(R.id.find_friends_button);
        friendRequestsButton = findViewById(R.id.friend_requests_button);
        noFriendsText = findViewById(R.id.no_friends_text);
        recyclerView = findViewById(R.id.rcvmain);

        userArrayList = new ArrayList<>();
        friendUids = new ArrayList<>();
        useradapter = new Useradapter(MainActivity.this, userArrayList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(useradapter);

        // NEW: Fetch friends list instead of all users
        fetchFriends();

        logoutMain.setOnClickListener(v -> showLogoutDialog());

        // NEW: Click listeners for new buttons
        findFriendsButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SearchUsersActivity.class)));
        friendRequestsButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, FriendRequestsActivity.class)));

        setupPresenceSystem();
    }

    private void fetchFriends() {
        friendsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String friendUid = snapshot.getKey();
                if (friendUid != null && !friendUids.contains(friendUid)) {
                    friendUids.add(friendUid);
                    usersRef.child(friendUid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                            User friend = userSnapshot.getValue(User.class);
                            if (friend != null) {
                                userArrayList.add(friend);
                                useradapter.notifyItemInserted(userArrayList.size() - 1);
                                updateNoFriendsView();
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String friendUidToRemove = snapshot.getKey();
                if (friendUidToRemove != null) {
                    friendUids.remove(friendUidToRemove);
                    for (int i = 0; i < userArrayList.size(); i++) {
                        if (userArrayList.get(i).getUid().equals(friendUidToRemove)) {
                            userArrayList.remove(i);
                            useradapter.notifyItemRemoved(i);
                            break;
                        }
                    }
                    updateNoFriendsView();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Check if friend list is empty initially
        friendsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                updateNoFriendsView();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
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
        Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.dialog_layout);
        Button ys = dialog.findViewById(R.id.yslgtmain);
        Button no = dialog.findViewById(R.id.nolgtmain);
        ys.setOnClickListener(v1 -> {
            DatabaseReference userStatusRef = firebaseDatabase.getReference().child("user").child(auth.getUid()).child("status");
            userStatusRef.setValue("Offline");
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MainActivity.this, login.class));
            finish();
        });
        no.setOnClickListener(v12 -> dialog.dismiss());
        dialog.show();
    }

    private void setupPresenceSystem() {
        // ... (presence system code remains the same)
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ... (onResume code remains the same)
    }

    @Override
    protected void onPause() {
        super.onPause();
        // ... (onPause code remains the same)
    }
}
