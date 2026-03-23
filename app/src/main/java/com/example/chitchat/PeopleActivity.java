package com.example.chitchat;

import android.os.Build;
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
import java.util.List;

/**
 * PeopleActivity — "People You May Know"
 *
 * Shows a scored, sorted list of suggested connections.
 * Each card shows:
 *   - Profile photo
 *   - Username
 *   - Mutual friend count ("3 mutual friends")
 *   - Connect button → opens SendRequestBottomSheet
 *   - Dismiss button → removes from suggestions
 *
 * Add this Activity to AndroidManifest.xml:
 *   <activity android:name=".PeopleActivity"
 *       android:windowSoftInputMode="adjustResize" />
 *
 * Add an entry point button in MainActivity toolbar.
 */
public class PeopleActivity extends AppCompatActivity {

    private RecyclerView          recyclerView;
    private PeopleAdapter         adapter;
    private ArrayList<SuggestionWithUser> suggestionList;
    private LinearLayout          emptyState;

    private FirebaseFirestore db;
    private String            myUid;
    private ConnectionManager connectionManager;
    private ListenerRegistration suggestionsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_people);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null)
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        myUid             = FirebaseAuth.getInstance().getUid();
        db                = FirebaseFirestore.getInstance();
        connectionManager = new ConnectionManager(myUid);

        recyclerView   = findViewById(R.id.people_recycler_view);
        emptyState     = findViewById(R.id.empty_state);
        suggestionList = new ArrayList<>();

        adapter = new PeopleAdapter(this, suggestionList, connectionManager, myUid);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // First time — generate suggestions, then listen
        connectionManager.generateSuggestions(myUid);
        listenForSuggestions();
    }

    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }

    private void listenForSuggestions() {
        suggestionsListener = db.collection("suggestions")
                .document(myUid)
                .collection("list")
                .orderBy("score",
                        com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .addSnapshotListener((snapshots, error) -> {
                    if (snapshots == null) return;

                    List<Suggestion> suggestions = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        suggestions.add(doc.toObject(Suggestion.class));
                    }

                    // Fetch user profiles for each suggestion
                    loadUsersForSuggestions(suggestions);
                });
    }

    private void loadUsersForSuggestions(List<Suggestion> suggestions) {
        suggestionList.clear();

        if (suggestions.isEmpty()) {
            adapter.notifyDataSetChanged();
            updateEmptyState();
            return;
        }

        final int total  = suggestions.size();
        final int[] done = {0};

        for (Suggestion suggestion : suggestions) {
            db.collection("users").document(suggestion.getSuggestedUid()).get()
                    .addOnSuccessListener(doc -> {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            suggestionList.add(
                                    new SuggestionWithUser(suggestion, user));
                        }
                        done[0]++;
                        if (done[0] == total) {
                            // Sort by score (already done by Firestore but keep consistent)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                suggestionList.sort((a, b) ->
                                        Double.compare(b.suggestion.getScore(),
                                                a.suggestion.getScore()));
                            }
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
        boolean empty = suggestionList.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (suggestionsListener != null) suggestionsListener.remove();
    }

    /**
     * Wrapper to keep Suggestion + User together in one list item.
     */
    public static class SuggestionWithUser {
        public final Suggestion suggestion;
        public final User       user;

        public SuggestionWithUser(Suggestion suggestion, User user) {
            this.suggestion = suggestion;
            this.user       = user;
        }
    }
}