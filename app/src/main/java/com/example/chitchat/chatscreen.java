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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class chatscreen extends AppCompatActivity {

    // --- BUG FIX: renamed for clarity; reciveruid → receiverUid ---
    String receiverImg, receiverName, receiverUid, senderUid, senderImg;
    CircleImageView profileImageView;
    TextView receiverNameText, typingIndicator;
    FloatingActionButton sendBtn;
    TextInputEditText msgBox;
    RecyclerView recyclerView;
    ArrayList<msgModel> messageList;
    FirebaseAuth firebaseAuth;
    FirebaseDatabase database;
    MsgAdapter msgAdapter;
    String chatRoomId;

    // Typing indicator debounce handler
    private final android.os.Handler typingHandler = new android.os.Handler();
    private Runnable stopTypingRunnable;
    private boolean isTyping = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatscreen);

        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        receiverName = getIntent().getStringExtra("nameee");
        receiverImg  = getIntent().getStringExtra("reciverimg");
        receiverUid  = getIntent().getStringExtra("uid");

        // BUG FIX: Null-check on currentUser before calling getUid()
        if (firebaseAuth.getCurrentUser() == null) {
            finish();
            return;
        }
        senderUid = firebaseAuth.getCurrentUser().getUid();

        // BUG FIX: Use string comparison instead of hashCode() to avoid collisions.
        // Lexicographic ordering guarantees a unique, collision-free room ID.
        if (senderUid.compareTo(receiverUid) < 0) {
            chatRoomId = senderUid + "_" + receiverUid;
        } else {
            chatRoomId = receiverUid + "_" + senderUid;
        }

        // View bindings
        profileImageView = findViewById(R.id.profile_chat);
        receiverNameText = findViewById(R.id.recivername);
        typingIndicator  = findViewById(R.id.typingIndicator);
        msgBox           = findViewById(R.id.chatboxxxx);
        sendBtn          = findViewById(R.id.sendchatbtn);
        recyclerView     = findViewById(R.id.msgadapter);

        // Back button
        ImageButton backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> onBackPressed());
        }

        messageList = new ArrayList<>();
        receiverNameText.setText(receiverName);
        Glide.with(this).load(receiverImg).placeholder(R.drawable.man).into(profileImageView);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

        // Fetch sender's profile picture, then set up the adapter and listeners
        DatabaseReference senderRef = database.getReference("user").child(senderUid);
        senderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                senderImg = snapshot.child("profilepic").getValue(String.class);
                msgAdapter = new MsgAdapter(chatscreen.this, messageList, senderImg, receiverImg);
                recyclerView.setAdapter(msgAdapter);
                listenForMessages();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Still set up adapter so app doesn't hang
                msgAdapter = new MsgAdapter(chatscreen.this, messageList, null, receiverImg);
                recyclerView.setAdapter(msgAdapter);
                listenForMessages();
            }
        });

        // Send button click
        sendBtn.setOnClickListener(v -> {
            if (msgBox.getText() == null) return;
            String msg = msgBox.getText().toString().trim();
            if (msg.isEmpty()) {
                Toast.makeText(this, "Enter a message", Toast.LENGTH_SHORT).show();
                return;
            }
            msgBox.setText("");
            stopTypingState(); // clear typing indicator before sending
            sendMessage(msg);
        });

        // Typing indicator
        setupTypingIndicator();
    }

    // -----------------------------------------------------------------------
    // FEATURE: Real-time typing indicator
    // -----------------------------------------------------------------------
    private void setupTypingIndicator() {
        DatabaseReference typingRef = database.getReference("chats")
                .child(chatRoomId).child("typing").child(receiverUid);

        // Watch whether the OTHER person is typing
        typingRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isReceiverTyping = snapshot.getValue(Boolean.class);
                if (isReceiverTyping != null && isReceiverTyping) {
                    typingIndicator.setText("typing...");
                    typingIndicator.setVisibility(View.VISIBLE);
                } else {
                    typingIndicator.setVisibility(View.GONE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Publish OUR typing state
        DatabaseReference myTypingRef = database.getReference("chats")
                .child(chatRoomId).child("typing").child(senderUid);

        stopTypingRunnable = () -> {
            isTyping = false;
            myTypingRef.setValue(false);
        };

        msgBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isTyping) {
                    isTyping = true;
                    myTypingRef.setValue(true);
                }
                typingHandler.removeCallbacks(stopTypingRunnable);
                typingHandler.postDelayed(stopTypingRunnable, 2000); // stop after 2s idle
            }
        });
    }

    private void stopTypingState() {
        typingHandler.removeCallbacks(stopTypingRunnable);
        isTyping = false;
        database.getReference("chats").child(chatRoomId)
                .child("typing").child(senderUid).setValue(false);
    }

    // -----------------------------------------------------------------------
    // FEATURE: Message listener — marks received messages as seen
    // -----------------------------------------------------------------------
    private void listenForMessages() {
        DatabaseReference chatRef = database.getReference("chats").child(chatRoomId).child("messages");
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    msgModel message = dataSnapshot.getValue(msgModel.class);
                    if (message == null) continue;

                    // BUG FIX: Null-check on senderId before comparing
                    if (message.getSenderId() != null
                            && !message.getSenderId().equals(senderUid)
                            && !message.getIsSeen()) {
                        dataSnapshot.getRef().child("isSeen").setValue(true);
                    }
                    messageList.add(message);
                }
                if (msgAdapter != null) {
                    msgAdapter.notifyDataSetChanged();
                    if (!messageList.isEmpty()) {
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // -----------------------------------------------------------------------
    // Send a message + update last-message preview + increment unread count
    // -----------------------------------------------------------------------
    private void sendMessage(String message) {
        long timestamp = new Date().getTime();
        msgModel model = new msgModel(message, senderUid, timestamp);

        DatabaseReference chatRoomRef = database.getReference("chats").child(chatRoomId);

        chatRoomRef.child("messages").push().setValue(model);

        // Update last message preview
        Map<String, Object> lastMsgUpdate = new HashMap<>();
        lastMsgUpdate.put("lastMessage", message);
        lastMsgUpdate.put("lastMessageTimestamp", timestamp);
        chatRoomRef.updateChildren(lastMsgUpdate);

        // Increment receiver's unread count atomically
        DatabaseReference unreadRef = chatRoomRef.child("unreadCount").child(receiverUid);
        unreadRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long current = snapshot.exists() && snapshot.getValue(Long.class) != null
                        ? snapshot.getValue(Long.class) : 0L;
                unreadRef.setValue(current + 1);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reset our unread count when we open the chat
        if (chatRoomId != null && senderUid != null) {
            database.getReference("chats").child(chatRoomId)
                    .child("unreadCount").child(senderUid).setValue(0L);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // BUG FIX: Always clear typing indicator when leaving the screen
        stopTypingState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        typingHandler.removeCallbacksAndMessages(null);
    }
}