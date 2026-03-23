package com.example.chitchat;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/**
 * ENHANCED FriendRequestsActivity
 *
 * BUGS FIXED:
 * 1. STALE EMPTY STATE: The empty state was only checked once at the top of
 *    onDataChange. If the last request was accepted/declined, the RecyclerView
 *    remained visible but empty. Now the empty state is checked AFTER all user
 *    detail fetches complete.
 *
 * 2. RACE CONDITION: The old code cleared requestUserList then immediately checked
 *    snapshot.exists() for the empty state — but user details were fetched async,
 *    so the empty state could flash incorrectly. Fixed with a counter pattern.
 *
 * IMPROVEMENT: Back navigation via Toolbar.
 */
public class FriendRequestsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FriendRequestAdapter adapter;
    private ArrayList<User> requestUserList;
    private DatabaseReference requestsRef, usersRef;
    private View noRequestsLayout; // now a LinearLayout in the new XML

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_requests);

        // Back navigation
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null) {
            finish();
            return;
        }

        requestsRef    = FirebaseDatabase.getInstance().getReference("friend_requests").child(myUid);
        usersRef       = FirebaseDatabase.getInstance().getReference("user");
        recyclerView   = findViewById(R.id.friend_requests_recycler_view);
        noRequestsLayout = findViewById(R.id.no_requests_text);

        requestUserList = new ArrayList<>();
        adapter = new FriendRequestAdapter(this, requestUserList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fetchFriendRequests();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void fetchFriendRequests() {
        requestsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                requestUserList.clear();

                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    // BUG FIX: Update empty state AFTER clearing the list
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                    return;
                }

                // Collect all sender IDs
                ArrayList<String> senderIds = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (ds.getKey() != null) senderIds.add(ds.getKey());
                }

                // BUG FIX: Use a counter to know when all async fetches are done
                final int[] counter = {0};
                for (String senderId : senderIds) {
                    usersRef.child(senderId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                            User user = userSnapshot.getValue(User.class);
                            if (user != null) requestUserList.add(user);
                            counter[0]++;
                            if (counter[0] == senderIds.size()) {
                                // All fetches done — now update UI
                                adapter.notifyDataSetChanged();
                                updateEmptyState();
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            counter[0]++;
                            if (counter[0] == senderIds.size()) {
                                adapter.notifyDataSetChanged();
                                updateEmptyState();
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                updateEmptyState();
            }
        });
    }

    private void updateEmptyState() {
        if (noRequestsLayout == null) return;
        boolean empty = requestUserList.isEmpty();
        noRequestsLayout.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}