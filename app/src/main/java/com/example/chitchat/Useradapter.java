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
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * ENHANCED Useradapter
 *
 * BUGS FIXED:
 * 1. MEMORY LEAK: Previous version added a new Firebase ValueEventListener on EVERY
 *    onBindViewHolder call with no way to remove them. After scrolling a list of friends,
 *    dozens of orphaned listeners would stack up in Firebase.
 *    FIX: Each ViewHolder stores its own listener reference. When the ViewHolder is recycled
 *    (onViewRecycled), we explicitly remove the listener from Firebase.
 *
 * 2. NULL POINTER: myUid could be null if the user was logged out mid-session.
 *    FIX: Added null-check for myUid before building chatRoomId.
 *
 * ENHANCEMENTS:
 * - Shows online badge as a green dot (via the existing online_status_badge View)
 * - Unread badge updates in real-time and cleans up properly
 */
public class Useradapter extends RecyclerView.Adapter<Useradapter.ViewHolder> {

    private final MainActivity mainActivity;
    private final ArrayList<User> userList;

    public Useradapter(MainActivity mainActivity, ArrayList<User> userList) {
        this.mainActivity = mainActivity;
        this.userList = userList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mainActivity)
                .inflate(R.layout.user_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);

        holder.username.setText(user.getUsername());
        holder.status.setText(user.getStatus());

        Glide.with(mainActivity)
                .load(user.getProfilepic())
                .placeholder(R.drawable.man)
                .circleCrop()
                .into(holder.userImage);

        // Online status badge (green dot)
        boolean isOnline = "Online".equalsIgnoreCase(user.getStatus());
        holder.onlineStatusBadge.setVisibility(isOnline ? View.VISIBLE : View.GONE);

        // BUG FIX: Null-check for myUid
        String myUid = mainActivity.auth.getUid();
        if (myUid == null) return;

        String friendUid = user.getUid();
        // BUG FIX: Use the same lexicographic room ID logic as chatscreen
        String chatRoomId = myUid.compareTo(friendUid) < 0
                ? myUid + "_" + friendUid
                : friendUid + "_" + myUid;

        // BUG FIX: Remove any previously attached listener before attaching a new one
        holder.detachUnreadListener();

        // Attach a fresh unread count listener
        holder.unreadListenerPath = FirebaseDatabase.getInstance().getReference()
                .child("chats").child(chatRoomId).child("unreadCount").child(myUid);

        holder.unreadListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.exists() && snapshot.getValue(Long.class) != null
                        ? snapshot.getValue(Long.class) : 0L;
                if (count > 0) {
                    holder.unreadBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                    holder.unreadBadge.setVisibility(View.VISIBLE);
                } else {
                    holder.unreadBadge.setVisibility(View.GONE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                holder.unreadBadge.setVisibility(View.GONE);
            }
        };

        holder.unreadListenerPath.addValueEventListener(holder.unreadListener);

        // Click to open chat
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(mainActivity, chatscreen.class);
            intent.putExtra("nameee", user.getUsername());
            intent.putExtra("reciverimg", user.getProfilepic());
            intent.putExtra("uid", user.getUid());
            mainActivity.startActivity(intent);
        });
    }

    // BUG FIX: Remove listeners when ViewHolder is recycled to prevent memory leaks
    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.detachUnreadListener();
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    // -----------------------------------------------------------------------
    // ViewHolder — stores the active listener so it can be removed on recycle
    // -----------------------------------------------------------------------
    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView userImage;
        TextView username, status, unreadBadge;
        View onlineStatusBadge;

        // Listener management — key to fixing the memory leak
        com.google.firebase.database.DatabaseReference unreadListenerPath;
        ValueEventListener unreadListener;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            userImage         = itemView.findViewById(R.id.llprofile_image);
            username          = itemView.findViewById(R.id.llname);
            status            = itemView.findViewById(R.id.llstatus);
            onlineStatusBadge = itemView.findViewById(R.id.online_status_badge);
            unreadBadge       = itemView.findViewById(R.id.unread_count_badge);
        }

        void detachUnreadListener() {
            if (unreadListenerPath != null && unreadListener != null) {
                unreadListenerPath.removeEventListener(unreadListener);
                unreadListener = null;
                unreadListenerPath = null;
            }
        }
    }
}