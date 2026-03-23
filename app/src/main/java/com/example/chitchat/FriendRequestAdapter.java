package com.example.chitchat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import android.widget.TextView;

/**
 * ENHANCED FriendRequestAdapter
 *
 * IMPROVEMENTS:
 * - Accept/Decline buttons use MaterialButton (matches new layout)
 * - Button disabled during async operation to prevent double-taps
 * - Consistent null-check on myUid
 * - Avatar loads with circleCrop() for cleaner Glide handling
 */
public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.RequestViewHolder> {

    private final Context context;
    private final ArrayList<User> requestList;
    private final DatabaseReference friendsRef;
    private final DatabaseReference requestsRef;
    private final String myUid;

    public FriendRequestAdapter(Context context, ArrayList<User> requestList) {
        this.context     = context;
        this.requestList = requestList;
        this.friendsRef  = FirebaseDatabase.getInstance().getReference("friends");
        this.requestsRef = FirebaseDatabase.getInstance().getReference("friend_requests");
        this.myUid       = FirebaseAuth.getInstance().getUid();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.friend_request_item, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        User user = requestList.get(position);
        holder.username.setText(user.getUsername());

        Glide.with(context)
                .load(user.getProfilepic())
                .placeholder(R.drawable.man)
                .circleCrop()
                .into(holder.profileImage);

        // Re-enable buttons for recycled views
        holder.acceptButton.setEnabled(true);
        holder.declineButton.setEnabled(true);

        holder.acceptButton.setOnClickListener(v -> {
            holder.acceptButton.setEnabled(false);
            holder.declineButton.setEnabled(false);
            acceptRequest(user.getUid());
        });

        holder.declineButton.setOnClickListener(v -> {
            holder.acceptButton.setEnabled(false);
            holder.declineButton.setEnabled(false);
            declineRequest(user.getUid());
        });
    }

    private void acceptRequest(String senderId) {
        if (myUid == null) return;
        // Add to both users' friends nodes
        friendsRef.child(myUid).child(senderId).setValue(true);
        friendsRef.child(senderId).child(myUid).setValue(true);
        // Remove the request — the ValueEventListener in the Activity refreshes the list
        requestsRef.child(myUid).child(senderId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(context, "Friend request accepted", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void declineRequest(String senderId) {
        if (myUid == null) return;
        requestsRef.child(myUid).child(senderId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(context, "Request declined", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView username;
        MaterialButton acceptButton, declineButton;

        RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage  = itemView.findViewById(R.id.profile_image);
            username      = itemView.findViewById(R.id.username_text);
            acceptButton  = itemView.findViewById(R.id.accept_button);
            declineButton = itemView.findViewById(R.id.decline_button);
        }
    }
}