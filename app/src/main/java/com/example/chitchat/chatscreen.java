package com.example.chitchat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * chatscreen — Firestore version
 *
 * Firestore structure used:
 *   chats/{chatRoomId}/messages  (subcollection, ordered by timestamp)
 *   chats/{chatRoomId}           (doc with lastMessage, unreadCount, typing)
 *   users/{senderUid}            (to fetch sender profile pic)
 */
public class chatscreen extends AppCompatActivity {

    private String receiverImg, receiverName, receiverUid, senderUid, senderImg;
    private CircleImageView   profileImageView;
    private TextView          receiverNameText, typingIndicator;
    private FloatingActionButton sendBtn;
    private TextInputEditText msgBox;
    private RecyclerView      recyclerView;
    private ArrayList<msgModel> messageList;
    private MsgAdapter        msgAdapter;
    private String            chatRoomId;

    private FirebaseAuth      auth;
    private FirebaseFirestore db;

    // Firestore listeners — stored for cleanup
    private ListenerRegistration messagesListener;
    private ListenerRegistration typingListener;

    // Typing debounce
    private final android.os.Handler typingHandler = new android.os.Handler();
    private       Runnable            stopTypingRunnable;
    private       boolean             isTyping = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatscreen);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) { finish(); return; }

        senderUid    = auth.getCurrentUser().getUid();
        receiverName = getIntent().getStringExtra("nameee");
        receiverImg  = getIntent().getStringExtra("reciverimg");
        receiverUid  = getIntent().getStringExtra("uid");

        // Collision-free room ID
        chatRoomId = senderUid.compareTo(receiverUid) < 0
                ? senderUid + "_" + receiverUid
                : receiverUid + "_" + senderUid;

        // Views
        profileImageView = findViewById(R.id.profile_chat);
        receiverNameText = findViewById(R.id.recivername);
        typingIndicator  = findViewById(R.id.typingIndicator);
        msgBox           = findViewById(R.id.chatboxxxx);
        sendBtn          = findViewById(R.id.sendchatbtn);
        recyclerView     = findViewById(R.id.msgadapter);

        ImageButton backBtn = findViewById(R.id.backButton);
        if (backBtn != null) backBtn.setOnClickListener(v -> onBackPressed());

        receiverNameText.setText(receiverName);
        Glide.with(this).load(receiverImg).placeholder(R.drawable.man).into(profileImageView);

        messageList = new ArrayList<>();

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

        // Fetch sender profile pic → then set up adapter + listeners
        db.collection("users").document(senderUid).get()
                .addOnSuccessListener(doc -> {
                    senderImg = doc.getString("profilepic");
                    msgAdapter = new MsgAdapter(
                            chatscreen.this, messageList, senderImg, receiverImg);
                    recyclerView.setAdapter(msgAdapter);
                    listenForMessages();
                    listenForTyping();
                })
                .addOnFailureListener(e -> {
                    // Still set up adapter even if profile fetch fails
                    msgAdapter = new MsgAdapter(
                            chatscreen.this, messageList, null, receiverImg);
                    recyclerView.setAdapter(msgAdapter);
                    listenForMessages();
                    listenForTyping();
                });

        sendBtn.setOnClickListener(v -> {
            if (msgBox.getText() == null) return;
            String text = msgBox.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Enter a message", Toast.LENGTH_SHORT).show();
                return;
            }
            msgBox.setText("");
            stopTypingState();
            sendMessage(text);
        });

        setupTypingBroadcast();
    }

    // ───────────────────────────────────────────────────────────────
    // Listen to messages subcollection, ordered by timestamp
    // ───────────────────────────────────────────────────────────────
    private void listenForMessages() {
        messagesListener = db.collection("chats")
                .document(chatRoomId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    messageList.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc
                            : snapshots.getDocuments()) {

                        msgModel msg = doc.toObject(msgModel.class);
                        if (msg == null) continue;

                        // Mark as seen if received by us and not yet seen
                        if (msg.getSenderId() != null
                                && !msg.getSenderId().equals(senderUid)
                                && !msg.getIsSeen()) {
                            doc.getReference().update("isSeen", true);
                        }
                        messageList.add(msg);
                    }

                    if (msgAdapter != null) {
                        msgAdapter.notifyDataSetChanged();
                        if (!messageList.isEmpty()) {
                            recyclerView.scrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }

    // ───────────────────────────────────────────────────────────────
    // Typing indicator — listen to other person's typing flag
    // ───────────────────────────────────────────────────────────────
    private void listenForTyping() {
        typingListener = db.collection("chats")
                .document(chatRoomId)
                .addSnapshotListener((snapshot, error) -> {
                    if (snapshot == null) return;
                    Object typingMap = snapshot.get("typing");
                    if (typingMap instanceof Map) {
                        Object val = ((Map<?, ?>) typingMap).get(receiverUid);
                        boolean theyAreTyping = Boolean.TRUE.equals(val);
                        typingIndicator.setVisibility(
                                theyAreTyping ? View.VISIBLE : View.GONE);
                        typingIndicator.setText("typing...");
                    }
                });
    }

    // ───────────────────────────────────────────────────────────────
    // Broadcast OUR typing state on every keystroke
    // ───────────────────────────────────────────────────────────────
    private void setupTypingBroadcast() {
        stopTypingRunnable = () -> {
            isTyping = false;
            db.collection("chats").document(chatRoomId)
                    .set(Map.of("typing",
                            Map.of(senderUid, false)), SetOptions.merge());
        };

        msgBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isTyping) {
                    isTyping = true;
                    db.collection("chats").document(chatRoomId)
                            .set(Map.of("typing",
                                    Map.of(senderUid, true)), SetOptions.merge());
                }
                typingHandler.removeCallbacks(stopTypingRunnable);
                typingHandler.postDelayed(stopTypingRunnable, 2000);
            }
        });
    }

    private void stopTypingState() {
        typingHandler.removeCallbacks(stopTypingRunnable);
        isTyping = false;
        db.collection("chats").document(chatRoomId)
                .set(Map.of("typing", Map.of(senderUid, false)), SetOptions.merge());
    }

    // ───────────────────────────────────────────────────────────────
    // Send a message
    // ───────────────────────────────────────────────────────────────
    private void sendMessage(String text) {
        DocumentReference chatRef = db.collection("chats").document(chatRoomId);

        // Build message map with server timestamp
        Map<String, Object> msgData = new HashMap<>();
        msgData.put("message",   text);
        msgData.put("senderId",  senderUid);
        msgData.put("timestamp", FieldValue.serverTimestamp());
        msgData.put("isSeen",    false);
        msgData.put("imageUrl",  null);

        // Add to messages subcollection
        chatRef.collection("messages").add(msgData);

        // Update chat room metadata
        Map<String, Object> chatUpdate = new HashMap<>();
        chatUpdate.put("lastMessage",          text);
        chatUpdate.put("lastMessageTimestamp", FieldValue.serverTimestamp());

        // Increment receiver's unread count atomically
        chatUpdate.put("unreadCount." + receiverUid, FieldValue.increment(1));

        chatRef.set(chatUpdate, SetOptions.merge());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reset OUR unread count when we open this chat
        if (chatRoomId != null && senderUid != null) {
            db.collection("chats").document(chatRoomId)
                    .set(Map.of("unreadCount",
                            Map.of(senderUid, 0L)), SetOptions.merge());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTypingState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove all Firestore listeners — no leaks
        if (messagesListener != null) messagesListener.remove();
        if (typingListener   != null) typingListener.remove();
        typingHandler.removeCallbacksAndMessages(null);
    }
}