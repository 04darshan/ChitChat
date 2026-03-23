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
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import android.widget.TextView;

/**
 * ENHANCED SearchUserAdapter
 *
 * IMPROVEMENTS:
 * - Button re-enabled in onBindViewHolder for recycled views (was staying disabled after scroll)
 * - Uses MaterialButton from new layout (typed correctly in ViewHolder)
 * - Glide uses circleCrop() for consistent avatar rendering
 */
public class SearchUserAdapter extends RecyclerView.Adapter<SearchUserAdapter.SearchViewHolder> {

    private final Context context;
    private final ArrayList<User> userList;
    private final ArrayList<String> friendUids;
    private final ArrayList<String> sentRequestUids;
    private final FirebaseAuth auth;

    public SearchUserAdapter(Context context,
                             ArrayList<User> userList,
                             ArrayList<String> friendUids,
                             ArrayList<String> sentRequestUids) {
        this.context         = context;
        this.userList        = userList;
        this.friendUids      = friendUids;
        this.sentRequestUids = sentRequestUids;
        this.auth            = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.search_user_item, parent, false);
        return new SearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        User user = userList.get(position);
        holder.username.setText(user.getUsername());

        Glide.with(context)
                .load(user.getProfilepic())
                .placeholder(R.drawable.man)
                .circleCrop()
                .into(holder.profileImage);

        // Always reset button state before setting (for recycled views)
        updateButtonState(holder.addFriendButton, user.getUid());

        holder.addFriendButton.setOnClickListener(v -> {
            holder.addFriendButton.setEnabled(false);
            sendFriendRequest(holder.addFriendButton, user.getUid());
        });
    }

    private void updateButtonState(MaterialButton button, String userId) {
        if (friendUids.contains(userId)) {
            button.setText("Friends ✓");
            button.setEnabled(false);
        } else if (sentRequestUids.contains(userId)) {
            button.setText("Sent");
            button.setEnabled(false);
        } else {
            button.setText("Add");
            button.setEnabled(true);
        }
    }

    private void sendFriendRequest(MaterialButton button, String recipientId) {
        String senderId = auth.getUid();
        if (senderId == null) return;

        DatabaseReference requestRef = FirebaseDatabase.getInstance()
                .getReference("friend_requests")
                .child(recipientId)
                .child(senderId);

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("status", "pending");
        requestData.put("timestamp", System.currentTimeMillis());

        requestRef.setValue(requestData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(context, "Friend request sent!", Toast.LENGTH_SHORT).show();
                // sentRequestUids listener in the Activity will update button state automatically
            } else {
                Toast.makeText(context, "Failed to send request", Toast.LENGTH_SHORT).show();
                button.setEnabled(true); // re-enable on failure
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class SearchViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView username;
        MaterialButton addFriendButton;

        SearchViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage   = itemView.findViewById(R.id.profile_image);
            username       = itemView.findViewById(R.id.username_text);
            addFriendButton = itemView.findViewById(R.id.add_friend_button);
        }
    }
}