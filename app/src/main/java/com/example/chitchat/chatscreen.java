package com.example.chitchat;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

    String reciverimg, recivername, reciveruid, senderUid, senderImg;
    CircleImageView pfl;
    TextView txtrecname;
    CardView sendbtn;
    EditText msgbox;
    RecyclerView recyclerViewadapter;
    ArrayList<msgModel> msgesArrylist;
    FirebaseAuth firebaseAuth;
    FirebaseDatabase database;
    MsgAdapter msgAdapter;
    String senderRoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatscreen);

        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        recivername = getIntent().getStringExtra("nameee");
        reciverimg = getIntent().getStringExtra("reciverimg");
        reciveruid = getIntent().getStringExtra("uid");
        senderUid = firebaseAuth.getUid();

        if (senderUid.hashCode() < reciveruid.hashCode()) {
            senderRoom = senderUid + reciveruid;
        } else {
            senderRoom = reciveruid + senderUid;
        }

        pfl = findViewById(R.id.profile_chat);
        txtrecname = findViewById(R.id.recivername);
        msgbox = findViewById(R.id.chatboxxxx);
        sendbtn = findViewById(R.id.sendchatbtn);
        recyclerViewadapter = findViewById(R.id.msgadapter);

        msgesArrylist = new ArrayList<>();
        txtrecname.setText(recivername);
        Glide.with(this).load(reciverimg).placeholder(R.drawable.man).into(pfl);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewadapter.setLayoutManager(layoutManager);

        // UPDATED: We will create the adapter *after* fetching the sender's image URL
        DatabaseReference reference = database.getReference().child("user").child(senderUid);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                senderImg = snapshot.child("profilepic").getValue(String.class);
                // NEW: Now that we have both image URLs, create the adapter
                msgAdapter = new MsgAdapter(chatscreen.this, msgesArrylist, senderImg, reciverimg);
                recyclerViewadapter.setAdapter(msgAdapter);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        DatabaseReference chatreference = database.getReference().child("chats").child(senderRoom).child("messages");
        chatreference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                msgesArrylist.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    msgModel messagesss = dataSnapshot.getValue(msgModel.class);
                    msgesArrylist.add(messagesss);
                }
                if(msgAdapter != null) {
                    msgAdapter.notifyDataSetChanged();
                    recyclerViewadapter.scrollToPosition(msgesArrylist.size() - 1);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        sendbtn.setOnClickListener(v -> {
            String msg = msgbox.getText().toString();
            if (msg.isEmpty()) {
                Toast.makeText(chatscreen.this, "Enter a message", Toast.LENGTH_SHORT).show();
                return;
            }
            msgbox.setText("");
            Date date = new Date();
            msgModel modelmsd = new msgModel(msg, senderUid, date.getTime());

            // NEW: Create a map to update the last message for both users
            Map<String, Object> lastMsgObj = new HashMap<>();
            lastMsgObj.put("lastMessage", msg);
            lastMsgObj.put("lastMessageTimestamp", date.getTime());

            DatabaseReference dbRef = database.getReference().child("chats").child(senderRoom);

            dbRef.child("messages").push().setValue(modelmsd).addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    // NEW: Update the last message data in the main chat node
                    dbRef.updateChildren(lastMsgObj);
                }
            });
        });
    }
}