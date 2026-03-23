package com.example.chitchat;

import android.os.Build;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ConnectionManager — central class for all friend/connection logic.
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │  Features                                                   │
 * │  1. Send request with optional personal note               │
 * │  2. Accept / Decline / Withdraw request                    │
 * │  3. Block user (hides both sides from each other)          │
 * │  4. Mutual friend count calculation                        │
 * │  5. "People You May Know" suggestion engine                │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Firestore paths used:
 *   connections/{uid}/requests/{senderUid}   — incoming requests
 *   connections/{uid}/sent/{recipientUid}    — outgoing requests (for withdraw)
 *   friends/{uid}/friendsList/{friendUid}    — confirmed friends
 *   blocked/{uid}/blockedList/{blockedUid}   — blocked users
 *   suggestions/{uid}/list/{suggestedUid}    — PYMK suggestions
 */
public class ConnectionManager {

    public interface Callback {
        void onSuccess();
        void onFailure(@NonNull Exception e);
    }

    public interface MutualCountCallback {
        void onResult(long count);
    }

    public interface SuggestionsCallback {
        void onResult(List<Suggestion> suggestions);
    }

    private final FirebaseFirestore db;
    private final String myUid;

    public ConnectionManager(@NonNull String myUid) {
        this.myUid = myUid;
        this.db    = FirebaseFirestore.getInstance();
    }

    // ─────────────────────────────────────────────────────────────
    // 1. SEND CONNECTION REQUEST (with optional note)
    // ─────────────────────────────────────────────────────────────
    public void sendRequest(@NonNull String recipientUid,
                            String message,
                            @NonNull Callback callback) {

        // First check if the recipient has blocked us
        db.collection("blocked").document(recipientUid)
                .collection("blockedList").document(myUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        callback.onFailure(new Exception("Cannot send request"));
                        return;
                    }
                    // Also check if WE have blocked them
                    db.collection("blocked").document(myUid)
                            .collection("blockedList").document(recipientUid)
                            .get()
                            .addOnSuccessListener(doc2 -> {
                                if (doc2.exists()) {
                                    callback.onFailure(new Exception("Unblock this user first"));
                                    return;
                                }
                                // Calculate mutual count, then write
                                getMutualFriendCount(recipientUid, count -> {
                                    String safeMessage = (message != null && !message.trim().isEmpty())
                                            ? message.trim().substring(0, Math.min(message.trim().length(), 200))
                                            : "";

                                    Map<String, Object> requestData = new HashMap<>();
                                    requestData.put("senderUid",    myUid);
                                    requestData.put("recipientUid", recipientUid);
                                    requestData.put("message",      safeMessage);
                                    requestData.put("status",       ConnectionRequest.STATUS_PENDING);
                                    requestData.put("mutualCount",  count);
                                    requestData.put("timestamp",    FieldValue.serverTimestamp());

                                    WriteBatch batch = db.batch();

                                    // Incoming: connections/{recipientUid}/requests/{myUid}
                                    batch.set(db.collection("connections").document(recipientUid)
                                            .collection("requests").document(myUid), requestData);

                                    // Outgoing: connections/{myUid}/sent/{recipientUid}
                                    batch.set(db.collection("connections").document(myUid)
                                            .collection("sent").document(recipientUid), requestData);

                                    // Remove from my suggestions (I already took action)
                                    batch.delete(db.collection("suggestions").document(myUid)
                                            .collection("list").document(recipientUid));

                                    batch.commit()
                                            .addOnSuccessListener(v -> callback.onSuccess())
                                            .addOnFailureListener(callback::onFailure);
                                });
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ─────────────────────────────────────────────────────────────
    // 2. ACCEPT REQUEST
    // ─────────────────────────────────────────────────────────────
    public void acceptRequest(@NonNull String senderUid, @NonNull Callback callback) {
        Map<String, Object> friendEntry = new HashMap<>();
        friendEntry.put("since", FieldValue.serverTimestamp());

        WriteBatch batch = db.batch();

        // Add to both sides' friendsList
        batch.set(db.collection("friends").document(myUid)
                .collection("friendsList").document(senderUid), friendEntry);
        batch.set(db.collection("friends").document(senderUid)
                .collection("friendsList").document(myUid), friendEntry);

        // Delete incoming request
        batch.delete(db.collection("connections").document(myUid)
                .collection("requests").document(senderUid));

        // Delete outgoing sent record on sender side
        batch.delete(db.collection("connections").document(senderUid)
                .collection("sent").document(myUid));

        // Remove from suggestions on both sides
        batch.delete(db.collection("suggestions").document(myUid)
                .collection("list").document(senderUid));
        batch.delete(db.collection("suggestions").document(senderUid)
                .collection("list").document(myUid));

        batch.commit()
                .addOnSuccessListener(v -> {
                    // After accepting, regenerate suggestions for both users
                    generateSuggestions(myUid);
                    generateSuggestions(senderUid);
                    callback.onSuccess();
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ─────────────────────────────────────────────────────────────
    // 3. DECLINE REQUEST
    // ─────────────────────────────────────────────────────────────
    public void declineRequest(@NonNull String senderUid, @NonNull Callback callback) {
        WriteBatch batch = db.batch();

        batch.delete(db.collection("connections").document(myUid)
                .collection("requests").document(senderUid));
        batch.delete(db.collection("connections").document(senderUid)
                .collection("sent").document(myUid));

        batch.commit()
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // ─────────────────────────────────────────────────────────────
    // 4. WITHDRAW SENT REQUEST
    // ─────────────────────────────────────────────────────────────
    public void withdrawRequest(@NonNull String recipientUid, @NonNull Callback callback) {
        WriteBatch batch = db.batch();

        batch.delete(db.collection("connections").document(recipientUid)
                .collection("requests").document(myUid));
        batch.delete(db.collection("connections").document(myUid)
                .collection("sent").document(recipientUid));

        batch.commit()
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // ─────────────────────────────────────────────────────────────
    // 5. BLOCK USER
    //    - Removes any existing friendship
    //    - Removes any pending requests both ways
    //    - Hides both users from each other's search + suggestions
    // ─────────────────────────────────────────────────────────────
    public void blockUser(@NonNull String targetUid, @NonNull Callback callback) {
        Map<String, Object> blockEntry = new HashMap<>();
        blockEntry.put("blockedUid", targetUid);
        blockEntry.put("timestamp",  FieldValue.serverTimestamp());

        WriteBatch batch = db.batch();

        // Write block record (one-directional — only YOU block THEM)
        batch.set(db.collection("blocked").document(myUid)
                .collection("blockedList").document(targetUid), blockEntry);

        // Remove friendship if exists
        batch.delete(db.collection("friends").document(myUid)
                .collection("friendsList").document(targetUid));
        batch.delete(db.collection("friends").document(targetUid)
                .collection("friendsList").document(myUid));

        // Remove all pending requests both ways
        batch.delete(db.collection("connections").document(myUid)
                .collection("requests").document(targetUid));
        batch.delete(db.collection("connections").document(targetUid)
                .collection("requests").document(myUid));
        batch.delete(db.collection("connections").document(myUid)
                .collection("sent").document(targetUid));
        batch.delete(db.collection("connections").document(targetUid)
                .collection("sent").document(myUid));

        // Remove from suggestions
        batch.delete(db.collection("suggestions").document(myUid)
                .collection("list").document(targetUid));

        batch.commit()
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // ─────────────────────────────────────────────────────────────
    // 6. UNBLOCK USER
    // ─────────────────────────────────────────────────────────────
    public void unblockUser(@NonNull String targetUid, @NonNull Callback callback) {
        db.collection("blocked").document(myUid)
                .collection("blockedList").document(targetUid)
                .delete()
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // ─────────────────────────────────────────────────────────────
    // 7. MUTUAL FRIEND COUNT
    //    Fetches both friend lists and intersects them
    // ─────────────────────────────────────────────────────────────
    public void getMutualFriendCount(@NonNull String otherUid,
                                     @NonNull MutualCountCallback callback) {
        Task<com.google.firebase.firestore.QuerySnapshot> myFriends =
                db.collection("friends").document(myUid)
                        .collection("friendsList").get();

        Task<com.google.firebase.firestore.QuerySnapshot> theirFriends =
                db.collection("friends").document(otherUid)
                        .collection("friendsList").get();

        Tasks.whenAllSuccess(myFriends, theirFriends)
                .addOnSuccessListener(results -> {
                    Set<String> mine = new HashSet<>();
                    for (DocumentSnapshot d :
                            ((com.google.firebase.firestore.QuerySnapshot) results.get(0))) {
                        mine.add(d.getId());
                    }
                    long count = 0;
                    for (DocumentSnapshot d :
                            ((com.google.firebase.firestore.QuerySnapshot) results.get(1))) {
                        if (mine.contains(d.getId())) count++;
                    }
                    callback.onResult(count);
                })
                .addOnFailureListener(e -> callback.onResult(0));
    }

    // ─────────────────────────────────────────────────────────────
    // 8. PEOPLE YOU MAY KNOW — suggestion engine
    //
    //    Algorithm:
    //      For every friend F of mine, look at F's friends.
    //      If person P is NOT already my friend and NOT blocked
    //      and NOT me, they're a candidate.
    //      Score = number of my friends who are also friends with P.
    //      Write the top 20 to suggestions/{myUid}/list/
    // ─────────────────────────────────────────────────────────────
    public void generateSuggestions(@NonNull String targetUid) {
        // Fetch my friends
        db.collection("friends").document(targetUid)
                .collection("friendsList").get()
                .addOnSuccessListener(myFriendsSnap -> {
                    if (myFriendsSnap.isEmpty()) return;

                    Set<String> myFriendIds = new HashSet<>();
                    for (QueryDocumentSnapshot d : myFriendsSnap) {
                        myFriendIds.add(d.getId());
                    }

                    // Fetch blocked list so we can exclude them
                    db.collection("blocked").document(targetUid)
                            .collection("blockedList").get()
                            .addOnSuccessListener(blockedSnap -> {
                                Set<String> blockedIds = new HashSet<>();
                                for (QueryDocumentSnapshot d : blockedSnap) {
                                    blockedIds.add(d.getId());
                                }

                                // Count how many of my friends know each candidate
                                Map<String, Long> mutualCount = new HashMap<>();

                                List<Task<com.google.firebase.firestore.QuerySnapshot>> tasks =
                                        new ArrayList<>();
                                for (String friendId : myFriendIds) {
                                    tasks.add(db.collection("friends").document(friendId)
                                            .collection("friendsList").get());
                                }

                                Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                                    for (Object result : results) {
                                        com.google.firebase.firestore.QuerySnapshot snap =
                                                (com.google.firebase.firestore.QuerySnapshot) result;
                                        for (QueryDocumentSnapshot d : snap) {
                                            String candidateId = d.getId();
                                            // Skip: me, existing friends, blocked
                                            if (candidateId.equals(targetUid)) continue;
                                            if (myFriendIds.contains(candidateId)) continue;
                                            if (blockedIds.contains(candidateId)) continue;

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                mutualCount.put(candidateId,
                                                        mutualCount.getOrDefault(candidateId, 0L) + 1);
                                            }
                                        }
                                    }

                                    // Sort by score, take top 20, write to Firestore
                                    List<Map.Entry<String, Long>> sorted =
                                            new ArrayList<>(mutualCount.entrySet());
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
                                    }

                                    WriteBatch batch = db.batch();
                                    int written = 0;
                                    for (Map.Entry<String, Long> entry : sorted) {
                                        if (written >= 20) break;
                                        String candidateId = entry.getKey();
                                        long   mutual      = entry.getValue();

                                        Map<String, Object> suggData = new HashMap<>();
                                        suggData.put("suggestedUid", candidateId);
                                        suggData.put("mutualCount",  mutual);
                                        suggData.put("score",        (double) mutual);
                                        suggData.put("reason", mutual == 1
                                                ? "1 mutual friend"
                                                : mutual + " mutual friends");
                                        suggData.put("createdAt", FieldValue.serverTimestamp());

                                        batch.set(db.collection("suggestions").document(targetUid)
                                                .collection("list").document(candidateId), suggData);
                                        written++;
                                    }
                                    batch.commit();
                                });
                            });
                });
    }

    // ─────────────────────────────────────────────────────────────
    // 9. FETCH SUGGESTIONS (sorted by score)
    // ─────────────────────────────────────────────────────────────
    public void getSuggestions(@NonNull SuggestionsCallback callback) {
        db.collection("suggestions").document(myUid)
                .collection("list")
                .orderBy("score", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Suggestion> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Suggestion s = doc.toObject(Suggestion.class);
                        list.add(s);
                    }
                    callback.onResult(list);
                })
                .addOnFailureListener(e -> callback.onResult(new ArrayList<>()));
    }
}