package com.example.chitchat;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Useradapter — Firestore version
 *
 * Key improvement over the old Realtime DB version:
 * Firestore gives us a ListenerRegistration object for each listener.
 * We store it in the ViewHolder and call .remove() in onViewRecycled(),
 * completely eliminating the memory-leak bug from before.
 */
public class Useradapter extends RecyclerView.Adapter<Useradapter.ViewHolder> {

    private final MainActivity    mainActivity;
    private final ArrayList<User> userList;
    private final FirebaseFirestore db;

    public Useradapter(MainActivity mainActivity, ArrayList<User> userList) {
        this.mainActivity = mainActivity;
        this.userList     = userList;
        this.db           = FirebaseFirestore.getInstance();
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

        // Online badge
        boolean isOnline = "Online".equalsIgnoreCase(user.getStatus());
        holder.onlineStatusBadge.setVisibility(isOnline ? View.VISIBLE : View.GONE);

        String myUid     = mainActivity.auth.getUid();
        String friendUid = user.getUid();
        if (myUid == null) return;

        // Lexicographic chat room ID — same formula used in chatscreen.java
        String chatRoomId = myUid.compareTo(friendUid) < 0
                ? myUid + "_" + friendUid
                : friendUid + "_" + myUid;

        // Remove any stale listener from a recycled ViewHolder
        holder.detachListeners();

        // ── Unread count listener ────────────────────────────────────
        // Listens to: chats/{chatRoomId}  field: unreadCount.{myUid}
        holder.unreadListener = db.collection("chats")
                .document(chatRoomId)
                .addSnapshotListener((snapshot, error) -> {
                    if (snapshot == null || !snapshot.exists()) {
                        holder.unreadBadge.setVisibility(View.GONE);
                        return;
                    }
                    // unreadCount is stored as a Map<String, Long> inside the doc
                    Long count = 0L;
                    Object unreadMap = snapshot.get("unreadCount");
                    if (unreadMap instanceof java.util.Map) {
                        Object val = ((java.util.Map<?, ?>) unreadMap).get(myUid);
                        if (val instanceof Long) count = (Long) val;
                    }
                    if (count > 0) {
                        holder.unreadBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                        holder.unreadBadge.setVisibility(View.VISIBLE);
                    } else {
                        holder.unreadBadge.setVisibility(View.GONE);
                    }
                });

        // ── Status listener (online dot updates in real-time) ────────
        holder.statusListener = db.collection("users")
                .document(friendUid)
                .addSnapshotListener((snapshot, error) -> {
                    if (snapshot == null) return;
                    String status = snapshot.getString("status");
                    boolean online = "Online".equalsIgnoreCase(status);
                    holder.onlineStatusBadge.setVisibility(online ? View.VISIBLE : View.GONE);
                    if (status != null) holder.status.setText(status);
                });

        // Click to open chat
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(mainActivity, chatscreen.class);
            intent.putExtra("nameee",     user.getUsername());
            intent.putExtra("reciverimg", user.getProfilepic());
            intent.putExtra("uid",        user.getUid());
            mainActivity.startActivity(intent);
        });
    }

    // Detach Firestore listeners when ViewHolder is recycled
    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.detachListeners();
    }

    @Override
    public int getItemCount() { return userList.size(); }

    // ── ViewHolder ───────────────────────────────────────────────────
    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView userImage;
        TextView        username, status, unreadBadge;
        View            onlineStatusBadge;

        ListenerRegistration unreadListener;
        ListenerRegistration statusListener;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            userImage         = itemView.findViewById(R.id.llprofile_image);
            username          = itemView.findViewById(R.id.llname);
            status            = itemView.findViewById(R.id.llstatus);
            onlineStatusBadge = itemView.findViewById(R.id.online_status_badge);
            unreadBadge       = itemView.findViewById(R.id.unread_count_badge);
        }

        void detachListeners() {
            if (unreadListener != null) { unreadListener.remove(); unreadListener = null; }
            if (statusListener != null) { statusListener.remove(); statusListener = null; }
        }
    }
}