package com.example.chitchat;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

        // Load the user's profile picture from the URL
        Glide.with(mainActivity).load(user.getProfilepic()).placeholder(R.drawable.man).into(holder.userimg);

        // Logic to show/hide the online status badge
        if (user.getStatus() != null && user.getStatus().equalsIgnoreCase("Online")) {
            holder.onlineStatusBadge.setVisibility(View.VISIBLE);
        } else {
            holder.onlineStatusBadge.setVisibility(View.GONE);
        }

        // Logic for the unread message count badge
        String myUid = mainActivity.auth.getUid();
        String friendUid = user.getUid();
        String chatRoomId;
        if (myUid.hashCode() < friendUid.hashCode()) {
            chatRoomId = myUid + friendUid;
        } else {
            chatRoomId = friendUid + myUid;
        }

        FirebaseDatabase.getInstance().getReference().child("chats").child(chatRoomId)
                .child("unreadCount").child(myUid).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Long unreadCount = snapshot.getValue(Long.class);
                            if (unreadCount != null && unreadCount > 0) {
                                holder.unreadBadge.setText(String.valueOf(unreadCount));
                                holder.unreadBadge.setVisibility(View.VISIBLE);
                            } else {
                                holder.unreadBadge.setVisibility(View.GONE);
                            }
                        } else {
                            holder.unreadBadge.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        // Logic for the click listener to open the chat screen
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
        TextView username, status;
        ImageView onlineStatusBadge;
        TextView unreadBadge;

        public viewholder(@NonNull View itemView) {
            super(itemView);
            userimg = itemView.findViewById(R.id.llprofile_image);
            username = itemView.findViewById(R.id.llname);
            status = itemView.findViewById(R.id.llstatus);
            onlineStatusBadge = itemView.findViewById(R.id.online_status_badge);
            unreadBadge = itemView.findViewById(R.id.unread_count_badge);
        }
    }
}
