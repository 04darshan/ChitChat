package com.example.chitchat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

/**
 * SearchUsersActivity — Firestore version
 *
 * Firestore query:  users  where username >= query && username <= query+\uf8ff
 * (same prefix-match trick, but Firestore handles it cleanly with no
 *  listener-stacking bug — we just cancel the previous Task before starting a new one)
 *
 * Friends + sent-requests lists are kept in sync via real-time Firestore listeners
 * stored as ListenerRegistration so they can be properly removed in onDestroy().
 */
public class SearchUsersActivity extends AppCompatActivity {

    private RecyclerView        recyclerView;
    private SearchUserAdapter   adapter;
    private ArrayList<User>     userList;
    private ArrayList<String>   friendUids;
    private ArrayList<String>   sentRequestUids;
    private LinearLayout        searchHintLayout;

    private FirebaseFirestore   db;
    private FirebaseAuth        auth;

    private ListenerRegistration friendsListener;
    private ListenerRegistration requestsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_users);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null)
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db              = FirebaseFirestore.getInstance();
        auth            = FirebaseAuth.getInstance();
        userList        = new ArrayList<>();
        friendUids      = new ArrayList<>();
        sentRequestUids = new ArrayList<>();

        recyclerView     = findViewById(R.id.search_recycler_view);
        searchHintLayout = findViewById(R.id.search_hint_layout);

        adapter = new SearchUserAdapter(this, userList, friendUids, sentRequestUids);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fetchCurrentUserData();

        TextInputEditText searchInput = findViewById(R.id.search_view);
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
                @Override public void afterTextChanged(Editable s) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchUsers(s.toString().trim());
                }
            });
            searchInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String q = searchInput.getText() != null
                            ? searchInput.getText().toString().trim() : "";
                    searchUsers(q);
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }

    // ───────────────────────────────────────────────────────────────
    // Real-time listeners for friends + sent requests
    // ───────────────────────────────────────────────────────────────
    private void fetchCurrentUserData() {
        String myUid = auth.getUid();
        if (myUid == null) return;

        // Friends subcollection
        friendsListener = db.collection("friends")
                .document(myUid)
                .collection("friendsList")
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots == null) return;
                    friendUids.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        friendUids.add(doc.getId());
                    }
                    adapter.notifyDataSetChanged();
                });

        // Sent requests: where WE are the sender
        requestsListener = db.collectionGroup("requests")
                .whereEqualTo("senderUid", myUid)
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots == null) return;
                    sentRequestUids.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        // doc.getId() is the sender (us); parent doc ID is recipient
                        sentRequestUids.add(doc.getReference()
                                .getParent().getParent().getId());
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    // ───────────────────────────────────────────────────────────────
    // Firestore prefix search on username field
    // ───────────────────────────────────────────────────────────────
    private void searchUsers(String queryText) {
        if (queryText.isEmpty()) {
            userList.clear();
            adapter.notifyDataSetChanged();
            showHint(true);
            return;
        }

        showHint(false);

        String myUid = auth.getUid();

        db.collection("users")
                .orderBy("username")
                .startAt(queryText)
                .endAt(queryText + "\uf8ff")
                .get()
                .addOnSuccessListener(snapshots -> {
                    userList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        User user = doc.toObject(User.class);
                        if (!user.getUid().equals(myUid)) {
                            userList.add(user);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    showHint(userList.isEmpty());
                });
    }

    private void showHint(boolean show) {
        if (searchHintLayout != null)
            searchHintLayout.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (friendsListener  != null) friendsListener.remove();
        if (requestsListener != null) requestsListener.remove();
    }
}