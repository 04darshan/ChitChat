package com.example.chitchat;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

/**
 * FriendRequestsActivity — FIXED
 *
 * BUG 1 — Wrong Firestore path:
 *   Old SearchUserAdapter was writing to friend_requests/{uid}/requests
 *   but this activity was listening on connections/{uid}/requests.
 *   Now SearchUserAdapter uses ConnectionManager which writes to the
 *   correct path connections/{uid}/requests — everything matches.
 *
 * BUG 2 — orderBy("timestamp") crash:
 *   When request is first written, serverTimestamp() is async — the
 *   field arrives as null in the first snapshot, causing the ordered
 *   query to throw an exception and return nothing.
 *   FIX: Removed orderBy — requests are shown as they arrive.
 *   Simple and reliable. Can re-add ordering once timestamps settle.
 *
 * BUG 3 — error field not checked:
 *   The snapshot listener was ignoring the error parameter.
 *   FIX: Log the error and show empty state instead of silent failure.
 */
public class FriendRequestsActivity extends AppCompatActivity {

    private RecyclerView         recyclerView;
    private FriendRequestAdapter adapter;
    private ArrayList<FriendRequestAdapter.RequestItem> requestList;
    private LinearLayout         emptyState;

    private FirebaseFirestore db;
    private String            myUid;
    private ConnectionManager connectionManager;
    private ListenerRegistration requestsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_requests);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null)
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null) { finish(); return; }

        db                = FirebaseFirestore.getInstance();
        connectionManager = new ConnectionManager(myUid);

        recyclerView = findViewById(R.id.friend_requests_recycler_view);
        emptyState   = findViewById(R.id.no_requests_text);
        requestList  = new ArrayList<>();

        adapter = new FriendRequestAdapter(this, requestList, connectionManager);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        listenForRequests();
    }

    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }

    private void listenForRequests() {
        requestsListener = db.collection("connections")
                .document(myUid)
                .collection("requests")
                // FIX: No orderBy — avoids null-timestamp crash on first write
                .addSnapshotListener((snapshots, error) -> {

                    // FIX: Check error first — was being silently ignored before
                    if (error != null) {
                        android.util.Log.e("FriendRequests",
                                "Listen failed: " + error.getMessage());
                        updateEmptyState();
                        return;
                    }

                    if (snapshots == null) {
                        updateEmptyState();
                        return;
                    }

                    ArrayList<String> senderIds = new ArrayList<>();
                    ArrayList<ConnectionRequest> reqs = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        ConnectionRequest req = doc.toObject(ConnectionRequest.class);
                        senderIds.add(doc.getId()); // doc ID == senderUid
                        reqs.add(req);
                    }

                    loadRequestsWithUsers(senderIds, reqs);
                });
    }

    private void loadRequestsWithUsers(ArrayList<String> senderIds,
                                       ArrayList<ConnectionRequest> reqs) {
        requestList.clear();

        if (senderIds.isEmpty()) {
            adapter.notifyDataSetChanged();
            updateEmptyState();
            return;
        }

        final int   total = senderIds.size();
        final int[] done  = {0};

        for (int i = 0; i < senderIds.size(); i++) {
            final String senderUid      = senderIds.get(i);
            final ConnectionRequest req = reqs.get(i);

            db.collection("users").document(senderUid).get()
                    .addOnSuccessListener(doc -> {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            requestList.add(
                                    new FriendRequestAdapter.RequestItem(user, req));
                        }
                        done[0]++;
                        if (done[0] == total) {
                            adapter.notifyDataSetChanged();
                            updateEmptyState();
                        }
                    })
                    .addOnFailureListener(e -> {
                        done[0]++;
                        if (done[0] == total) {
                            adapter.notifyDataSetChanged();
                            updateEmptyState();
                        }
                    });
        }
    }

    private void updateEmptyState() {
        boolean empty = requestList.isEmpty();
        if (emptyState != null)
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestsListener != null) requestsListener.remove();
    }
}