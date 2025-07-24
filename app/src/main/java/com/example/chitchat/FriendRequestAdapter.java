package com.example.chitchat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.RequestViewHolder> {

    Context context;
    ArrayList<User> requestList;
    DatabaseReference friendsRef, requestsRef;
    String myUid;

    public FriendRequestAdapter(Context context, ArrayList<User> requestList) {
        this.context = context;
        this.requestList = requestList;
        this.friendsRef = FirebaseDatabase.getInstance().getReference("friends");
        this.requestsRef = FirebaseDatabase.getInstance().getReference("friend_requests");
        this.myUid = FirebaseAuth.getInstance().getUid();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.friend_request_item, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        User user = requestList.get(position);
        holder.username.setText(user.getUsername());
        Glide.with(context).load(user.getProfilepic()).placeholder(R.drawable.man).into(holder.profileImage);

        // UPDATED: Pass the user's UID instead of the position
        holder.acceptButton.setOnClickListener(v -> acceptRequest(user.getUid()));
        holder.declineButton.setOnClickListener(v -> declineRequest(user.getUid()));
    }

    // UPDATED: Removed the 'position' parameter and the list modification code
    private void acceptRequest(String senderId) {
        if (myUid == null) return;
        // Add to friends list for both users
        friendsRef.child(myUid).child(senderId).setValue(true);
        friendsRef.child(senderId).child(myUid).setValue(true);

        // Remove the request. The listener in the Activity will handle the UI update automatically.
        requestsRef.child(myUid).child(senderId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(context, "Friend request accepted.", Toast.LENGTH_SHORT).show();
                // We no longer modify the list here to prevent the crash.
            }
        });
    }

    // UPDATED: Removed the 'position' parameter and the list modification code
    private void declineRequest(String senderId) {
        if (myUid == null) return;
        // Remove the request. The listener in the Activity will handle the UI update automatically.
        requestsRef.child(myUid).child(senderId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(context, "Friend request declined.", Toast.LENGTH_SHORT).show();
                // We no longer modify the list here.
            }
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView username;
        Button acceptButton, declineButton;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            username = itemView.findViewById(R.id.username_text);
            acceptButton = itemView.findViewById(R.id.accept_button);
            declineButton = itemView.findViewById(R.id.decline_button);
        }
    }
}
