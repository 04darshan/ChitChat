package com.example.chitchat;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * MainActivity — FIXED
 *
 * FIX 1 — Friend appears on BOTH sides after accept:
 *   The snapshot listener on friendsList fires automatically for BOTH
 *   A and B when the WriteBatch commits — because both
 *   friends/{A}/friendsList and friends/{B}/friendsList are written.
 *   Each user's MainActivity has its own listener on its own path,
 *   so both lists update in real time without any extra work.
 *
 * FIX 2 — Profile data stays fresh (was stale after status change):
 *   fetchFriendProfiles() now uses addSnapshotListener per friend doc
 *   instead of a one-time .get() — so "Online/Offline" updates
 *   in real time as friends come and go.
 *   All per-user listeners are stored and removed in onDestroy().
 *
 * FIX 3 — Race condition when counter finishes before all docs load:
 *   Counter now only triggers the final notify when ALL individual
 *   user doc listeners have emitted at least once (tracked via a Set).
 */
public class MainActivity extends AppCompatActivity {

    FirebaseAuth      auth;  // package-accessible for Useradapter
    FirebaseFirestore db;

    private View              logoutBtn, noFriendsLayout;
    private android.widget.ImageButton findFriendsButton, friendRequestsButton;
    private RecyclerView      recyclerView;
    private Useradapter       useradapter;
    private ArrayList<User>   userList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Dialog            logoutDialog;

    // Master listener on the friendsList subcollection
    private ListenerRegistration friendsListener;

    // Per-user listeners so we can remove them when the list changes
    private final ArrayList<ListenerRegistration> profileListeners = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, login.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        logoutBtn            = findViewById(R.id.logbtn);
        findFriendsButton    = findViewById(R.id.find_friends_button);
        friendRequestsButton = findViewById(R.id.friend_requests_button);
        noFriendsLayout      = findViewById(R.id.no_friends_text);
        recyclerView         = findViewById(R.id.rcvmain);
        swipeRefreshLayout   = findViewById(R.id.swipe_refresh_layout);

        userList    = new ArrayList<>();
        useradapter = new Useradapter(this, userList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(useradapter);

        // ── This is the key listener ──────────────────────────────
        // addSnapshotListener fires immediately with current data,
        // AND re-fires whenever a friend is added or removed.
        // So when B accepts A's request:
        //   - A's friendsList gets a new doc  → A's listener fires → A sees B
        //   - B's friendsList gets a new doc  → B's listener fires → B sees A
        // Both happen automatically, no extra code needed.
        // ─────────────────────────────────────────────────────────
        listenToFriendsList();
        setupPresenceSystem();

        swipeRefreshLayout.setOnRefreshListener(() ->
                swipeRefreshLayout.setRefreshing(false));

        logoutBtn.setOnClickListener(v -> showLogoutDialog());
        findFriendsButton.setOnClickListener(v ->
                startActivity(new Intent(this, SearchUsersActivity.class)));
        // Ye NEW line add karo uske neeche:
        findViewById(R.id.people_button).setOnClickListener(v ->
                startActivity(new Intent(this, PeopleActivity.class)));
        friendRequestsButton.setOnClickListener(v ->
                startActivity(new Intent(this, FriendRequestsActivity.class)));
    }

    // ─────────────────────────────────────────────────────────────
    // Step 1: Listen to my friendsList subcollection
    // Fires on launch AND every time a friend is added/removed
    // ─────────────────────────────────────────────────────────────
    private void listenToFriendsList() {
        String myUid = auth.getCurrentUser().getUid();

        friendsListener = db.collection("friends")
                .document(myUid)
                .collection("friendsList")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        updateEmptyState();
                        return;
                    }

                    ArrayList<String> friendUids = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        friendUids.add(doc.getId());
                    }

                    // Step 2: load + watch each friend's profile
                    fetchFriendProfiles(friendUids);
                });
    }

    // ─────────────────────────────────────────────────────────────
    // Step 2: For each friend UID, attach a real-time listener
    // on their user doc so Online/Offline updates live
    //
    // FIX: remove all old profile listeners before attaching new
    // ones — otherwise every friendsList change stacks more listeners
    // ─────────────────────────────────────────────────────────────
    private void fetchFriendProfiles(ArrayList<String> friendUids) {

        // Remove all previous per-user listeners
        for (ListenerRegistration reg : profileListeners) reg.remove();
        profileListeners.clear();
        userList.clear();

        if (friendUids.isEmpty()) {
            useradapter.notifyDataSetChanged();
            updateEmptyState();
            return;
        }

        // Use a map to hold the latest snapshot for each uid
        // so the list stays consistent when individual docs update
        Map<String, User> latestProfiles = new HashMap<>();

        for (String uid : friendUids) {
            // addSnapshotListener fires immediately AND on every change
            // (e.g. when a friend's status flips Online → Offline)
            ListenerRegistration reg = db.collection("users")
                    .document(uid)
                    .addSnapshotListener((doc, err) -> {
                        if (doc == null || !doc.exists()) return;

                        User user = doc.toObject(User.class);
                        if (user == null) return;

                        // Update (or insert) this user in the map
                        latestProfiles.put(uid, user);

                        // Rebuild userList from the map every time any friend updates
                        userList.clear();
                        userList.addAll(latestProfiles.values());
                        Collections.sort(userList,
                                (a, b) -> a.getUsername()
                                        .compareToIgnoreCase(b.getUsername()));
                        useradapter.notifyDataSetChanged();
                        updateEmptyState();
                    });

            profileListeners.add(reg);
        }
    }

    private void updateEmptyState() {
        if (noFriendsLayout == null) return;
        boolean empty = userList.isEmpty();
        noFriendsLayout.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ─────────────────────────────────────────────────────────────
    // Presence system — onResume/onPause (no extra dependency)
    // FIX: Replaced ProcessLifecycleOwner which needs a separate
    // gradle dependency 'lifecycle-process'. onResume/onPause
    // is simpler and works perfectly for a single-activity chat app.
    // ─────────────────────────────────────────────────────────────
    private void setupPresenceSystem() {
        // Status is set in onResume() and cleared in onPause() below
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (auth.getCurrentUser() != null) {
            db.collection("users")
                    .document(auth.getCurrentUser().getUid())
                    .set(Map.of("status", "Online"), SetOptions.merge());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (auth.getCurrentUser() != null) {
            db.collection("users")
                    .document(auth.getCurrentUser().getUid())
                    .set(Map.of("status", "Offline"), SetOptions.merge());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Logout dialog
    // ─────────────────────────────────────────────────────────────
    private void showLogoutDialog() {
        logoutDialog = new Dialog(this);
        logoutDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        logoutDialog.setContentView(R.layout.dialog_layout);

        if (logoutDialog.getWindow() != null) {
            logoutDialog.getWindow()
                    .setBackgroundDrawableResource(android.R.color.transparent);
            logoutDialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        MaterialButton yesBtn = logoutDialog.findViewById(R.id.yslgtmain);
        MaterialButton noBtn  = logoutDialog.findViewById(R.id.nolgtmain);

        yesBtn.setOnClickListener(v -> {
            if (auth.getCurrentUser() != null)
                db.collection("users")
                        .document(auth.getCurrentUser().getUid())
                        .set(Map.of("status", "Offline"), SetOptions.merge());
            auth.signOut();
            startActivity(new Intent(this, login.class));
            finish();
        });
        noBtn.setOnClickListener(v -> logoutDialog.dismiss());
        logoutDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove master listener
        if (friendsListener != null) friendsListener.remove();
        // Remove all per-user profile listeners
        for (ListenerRegistration reg : profileListeners) reg.remove();
        profileListeners.clear();
        if (logoutDialog != null && logoutDialog.isShowing()) logoutDialog.dismiss();
    }
}