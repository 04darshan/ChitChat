package com.example.chitchat;

import android.os.Bundle;
import android.view.View;
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

public class FriendRequestsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FriendRequestAdapter adapter;
    private ArrayList<User> requestUserList;
    private DatabaseReference requestsRef, usersRef;
    private FirebaseAuth auth;
    private TextView noRequestsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_requests);

        auth = FirebaseAuth.getInstance();
        String myUid = auth.getUid();
        requestsRef = FirebaseDatabase.getInstance().getReference("friend_requests").child(myUid);
        usersRef = FirebaseDatabase.getInstance().getReference("user");

        recyclerView = findViewById(R.id.friend_requests_recycler_view);
        noRequestsText = findViewById(R.id.no_requests_text);
        requestUserList = new ArrayList<>();
        adapter = new FriendRequestAdapter(this, requestUserList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fetchFriendRequests();
    }

    private void fetchFriendRequests() {
        requestsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                requestUserList.clear();
                if (!snapshot.exists()) {
                    noRequestsText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    noRequestsText.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }

                for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                    String senderId = requestSnapshot.getKey();
                    if (senderId != null) {
                        // For each sender ID, get their user details
                        usersRef.child(senderId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                User user = userSnapshot.getValue(User.class);
                                if (user != null) {
                                    requestUserList.add(user);
                                    adapter.notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
