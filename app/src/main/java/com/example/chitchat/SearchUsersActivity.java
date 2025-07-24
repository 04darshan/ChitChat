package com.example.chitchat;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.widget.SearchView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

public class SearchUsersActivity extends AppCompatActivity {

    private SearchView searchView;
    private RecyclerView recyclerView;
    private SearchUserAdapter adapter;
    private ArrayList<User> userList;
    private DatabaseReference usersRef;
    private FirebaseAuth auth;
    private ArrayList<String> friendUids;
    private ArrayList<String> sentRequestUids;
    private LinearLayout searchContainer; // NEW: Declare the LinearLayout

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_users);

        searchView = findViewById(R.id.search_view);
        recyclerView = findViewById(R.id.search_recycler_view);
        searchContainer = findViewById(R.id.search_container); // NEW: Initialize the LinearLayout
        usersRef = FirebaseDatabase.getInstance().getReference("user");
        auth = FirebaseAuth.getInstance();

        userList = new ArrayList<>();
        friendUids = new ArrayList<>();
        sentRequestUids = new ArrayList<>();

        adapter = new SearchUserAdapter(this, userList, friendUids, sentRequestUids);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fetchCurrentUserData();

        // NEW: Set a click listener on the entire container
        searchContainer.setOnClickListener(v -> {
            // This activates the SearchView and brings up the keyboard
            searchView.setIconified(false);
            searchView.requestFocus();
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchUsers(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchUsers(newText);
                return false;
            }
        });
    }

    private void fetchCurrentUserData() {
        String myUid = auth.getUid();
        if (myUid == null) return;

        DatabaseReference friendsRef = FirebaseDatabase.getInstance().getReference("friends").child(myUid);
        friendsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                friendUids.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    friendUids.add(dataSnapshot.getKey());
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        DatabaseReference requestsRef = FirebaseDatabase.getInstance().getReference("friend_requests");
        requestsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                sentRequestUids.clear();
                for (DataSnapshot recipientSnapshot : snapshot.getChildren()) {
                    if (recipientSnapshot.hasChild(myUid)) {
                        sentRequestUids.add(recipientSnapshot.getKey());
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void searchUsers(String queryText) {
        if (queryText.isEmpty()) {
            userList.clear();
            adapter.notifyDataSetChanged();
            return;
        }

        Query searchQuery = usersRef.orderByChild("username")
                .startAt(queryText)
                .endAt(queryText + "\uf8ff");

        searchQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null && !user.getUid().equals(auth.getUid())) {
                        userList.add(user);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
