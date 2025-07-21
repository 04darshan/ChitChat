package com.example.chitchat;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import de.hdodenhof.circleimageview.CircleImageView;

public class chatscreen extends AppCompatActivity {

    String reciverimg, recivername, reciveruid, senderUid;
    CircleImageView pfl;
    TextView txtrecname;
    CardView sendbtn;
    EditText msgbox;
    RecyclerView recyclerViewadapter;
    ArrayList<msgModel> msgesArrylist;
    FirebaseAuth firebaseAuth;
    FirebaseDatabase database;
    MsgAdapter msgAdapter;
    public static String senderImg, reciverImg;
    String senderRoom, reciverRoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chatscreen);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        // Initialize views
        pfl = findViewById(R.id.profile_chat);
        txtrecname = findViewById(R.id.recivername);
        msgbox = findViewById(R.id.chatboxxxx);
        sendbtn = findViewById(R.id.sendchatbtn);
        recyclerViewadapter = findViewById(R.id.msgadapter);

        // Get intent extras
        recivername = getIntent().getStringExtra("nameee");
        reciverimg = getIntent().getStringExtra("reciverimg");
        reciveruid = getIntent().getStringExtra("uid");
        senderUid = firebaseAuth.getUid();

        senderRoom = senderUid + reciveruid;
        reciverRoom = reciveruid + senderUid;

        txtrecname.setText(recivername);
        pfl.setImageResource(R.drawable.man);

        // Initialize message list and adapter
        msgesArrylist = new ArrayList<>();
        msgAdapter = new MsgAdapter(chatscreen.this, msgesArrylist);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewadapter.setLayoutManager(layoutManager);
        recyclerViewadapter.setAdapter(msgAdapter);

        // Listen for messages
        DatabaseReference chatreference = database.getReference().child("chats").child(senderRoom).child("messages");
        chatreference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                msgesArrylist.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    msgModel messagesss = dataSnapshot.getValue(msgModel.class);
                    msgesArrylist.add(messagesss);
                }
                msgAdapter.notifyDataSetChanged();
                recyclerViewadapter.scrollToPosition(msgesArrylist.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        // Get sender image
        DatabaseReference reference = database.getReference().child("user").child(senderUid);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object img = snapshot.child("pfl").getValue();
                senderImg = (img != null) ? img.toString() : "default";
                reciverImg = reciverimg;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        // Send message
        sendbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = msgbox.getText().toString();
                if (msg.isEmpty()) {
                    Toast.makeText(chatscreen.this, "Enter a message", Toast.LENGTH_SHORT).show();
                    return;
                }

                msgbox.setText("");
                Date date = new Date();
                msgModel modelmsd = new msgModel(msg, senderUid, date.getTime());

                database.getReference().child("chats").child(senderRoom).child("messages").push().setValue(modelmsd)
                        .addOnCompleteListener(task -> {
                            database.getReference().child("chats").child(reciverRoom).child("messages").push().setValue(modelmsd);
                        });
            }
        });
    }
}
