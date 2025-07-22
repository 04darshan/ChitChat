package com.example.chitchat;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import de.hdodenhof.circleimageview.CircleImageView;

public class Useradapter extends RecyclerView.Adapter<Useradapter.viewholder> {
    MainActivity mainActivity;
    ArrayList<User> userArrayList;

    public Useradapter(MainActivity mainActivity, ArrayList<User> userArrayList) {
        this.mainActivity = mainActivity;
        this.userArrayList = userArrayList;
    }

    @NonNull
    @Override
    public Useradapter.viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mainActivity).inflate(R.layout.user_item, parent, false);
        return new viewholder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Useradapter.viewholder holder, int position) {
        User user = userArrayList.get(position);
        holder.username.setText(user.getUsername());
        holder.status.setText(user.getStatus());

        Glide.with(mainActivity)
                .load(user.getProfilepic())
                .placeholder(R.drawable.man)
                .error(R.drawable.man)
                .into(holder.userimg);

        // NEW: Fetch and set the last message
        String senderId = mainActivity.auth.getUid();
        String receiverId = user.getUid();
        String chatRoomId;
        if (senderId.hashCode() < receiverId.hashCode()) {
            chatRoomId = senderId + receiverId;
        } else {
            chatRoomId = receiverId + senderId;
        }

        FirebaseDatabase.getInstance().getReference().child("chats").child(chatRoomId)
                .child("lastMessage").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                            String lastMsg = snapshot.getValue(String.class);
                            holder.lastMessage.setText(lastMsg);
                        } else {
                            holder.lastMessage.setText("Tap to chat");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });


        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(mainActivity, chatscreen.class);
            intent.putExtra("nameee", user.getUsername());
            intent.putExtra("reciverimg", user.getProfilepic());
            intent.putExtra("uid", user.getUid());
            mainActivity.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userArrayList.size();
    }

    public class viewholder extends RecyclerView.ViewHolder {
        CircleImageView userimg;
        TextView username, status, lastMessage; // UPDATED: add lastMessage TextView

        public viewholder(@NonNull View itemView) {
            super(itemView);
            userimg = itemView.findViewById(R.id.llprofile_image);
            username = itemView.findViewById(R.id.llname);
            status = itemView.findViewById(R.id.llstatus);
            lastMessage = itemView.findViewById(R.id.ll_last_message); // UPDATED: find the new view by its ID
        }
    }
}