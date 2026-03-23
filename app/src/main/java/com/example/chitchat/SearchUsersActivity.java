package com.example.chitchat;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/**
 * ENHANCED SearchUsersActivity
 *
 * BUGS FIXED:
 * 1. DUPLICATE RESULTS / LISTENER LEAK: Every keystroke called searchUsers() which added a NEW
 *    addValueEventListener to the query. The old listener was never removed, causing:
 *    - Duplicate results appearing in the list
 *    - Memory leaks as listeners accumulated
 *    FIX: Store the active query and its listener, remove the old listener before creating a new
 *    one on each keystroke. Use addListenerForSingleValueEvent for keyword searches.
 *
 * 2. EMPTY STATE: No visual feedback when search returns 0 results.
 *    FIX: Show a hint/empty-state layout.
 *
 * ENHANCEMENT: Back navigation via toolbar.
 */
public class SearchUsersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SearchUserAdapter adapter;
    private ArrayList<User> userList;
    private DatabaseReference usersRef;
    private FirebaseAuth auth;
    private ArrayList<String> friendUids;
    private ArrayList<String> sentRequestUids;
    private LinearLayout searchHintLayout;

    // BUG FIX: Track the current active query + listener so we can remove it
    private Query activeQuery;
    private ValueEventListener activeSearchListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_users);

        // Toolbar back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        SearchView searchView = findViewById(R.id.search_view);
        recyclerView          = findViewById(R.id.search_recycler_view);
        searchHintLayout      = findViewById(R.id.search_hint_layout);

        usersRef = FirebaseDatabase.getInstance().getReference("user");
        auth     = FirebaseAuth.getInstance();

        userList         = new ArrayList<>();
        friendUids       = new ArrayList<>();
        sentRequestUids  = new ArrayList<>();

        adapter = new SearchUserAdapter(this, userList, friendUids, sentRequestUids);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fetchCurrentUserData();

        if (searchView != null) {
            searchView.setIconifiedByDefault(false);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    searchUsers(query.trim());
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    searchUsers(newText.trim());
                    return false;
                }
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // -----------------------------------------------------------------------
    // Fetch current user's friends + sent requests (real-time listeners OK here
    // because they're on small node paths, not on a search query that changes)
    // -----------------------------------------------------------------------
    private void fetchCurrentUserData() {
        String myUid = auth.getUid();
        if (myUid == null) return;

        FirebaseDatabase.getInstance().getReference("friends").child(myUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        friendUids.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            friendUids.add(ds.getKey());
                        }
                        adapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        FirebaseDatabase.getInstance().getReference("friend_requests")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        sentRequestUids.clear();
                        for (DataSnapshot recipientSnap : snapshot.getChildren()) {
                            if (recipientSnap.hasChild(myUid)) {
                                sentRequestUids.add(recipientSnap.getKey());
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // -----------------------------------------------------------------------
    // BUG FIX: Remove the previous listener before attaching a new one.
    // Use addListenerForSingleValueEvent so the listener auto-removes after firing.
    // -----------------------------------------------------------------------
    private void searchUsers(String queryText) {
        // Remove any previously active listener to prevent duplicate results
        if (activeQuery != null && activeSearchListener != null) {
            activeQuery.removeEventListener(activeSearchListener);
            activeQuery = null;
            activeSearchListener = null;
        }

        if (queryText.isEmpty()) {
            userList.clear();
            adapter.notifyDataSetChanged();
            showHint(true);
            return;
        }

        showHint(false);

        activeQuery = usersRef.orderByChild("username")
                .startAt(queryText)
                .endAt(queryText + "\uf8ff");

        // BUG FIX: Use SingleValueEvent — only fires once, no stacking listeners
        activeSearchListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                String myUid = auth.getUid();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User user = ds.getValue(User.class);
                    if (user != null && !user.getUid().equals(myUid)) {
                        userList.add(user);
                    }
                }
                adapter.notifyDataSetChanged();

                if (userList.isEmpty()) {
                    showHint(true);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        // Using addListenerForSingleValueEvent means the listener fires once and is gone,
        // eliminating the accumulation bug while still keeping activeQuery reference correct.
        activeQuery.addListenerForSingleValueEvent(activeSearchListener);
    }

    private void showHint(boolean show) {
        if (searchHintLayout != null) {
            searchHintLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}