package com.example.chitchat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * FriendRequestAdapter — UPGRADED
 *
 * Each card now shows:
 *   - Profile photo
 *   - Sender's name
 *   - Mutual friend count ("3 mutual friends")
 *   - Personal note from the sender (if any)
 *   - [Accept] button
 *   - [Decline] button
 *   - [⋮ More] → long-press or icon → Block user option
 *
 * Pair this adapter with the upgraded item_connection_request.xml layout.
 */
public class FriendRequestAdapter
        extends RecyclerView.Adapter<FriendRequestAdapter.RequestVH> {

    public static class RequestItem {
        public final User              user;
        public final ConnectionRequest request;

        public RequestItem(User user, ConnectionRequest request) {
            this.user    = user;
            this.request = request;
        }
    }

    private final Context             context;
    private final ArrayList<RequestItem> list;
    private final ConnectionManager   connectionManager;

    public FriendRequestAdapter(Context context,
                                ArrayList<RequestItem> list,
                                ConnectionManager connectionManager) {
        this.context           = context;
        this.list              = list;
        this.connectionManager = connectionManager;
    }

    @NonNull
    @Override
    public RequestVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_connection_request, parent, false);
        return new RequestVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestVH holder, int position) {
        RequestItem item    = list.get(position);
        User        user    = item.user;
        ConnectionRequest r = item.request;

        holder.username.setText(user.getUsername());

        // Mutual friend count
        long mutual = r.getMutualCount();
        if (mutual > 0) {
            holder.mutualLabel.setText(mutual == 1
                    ? "1 mutual friend"
                    : mutual + " mutual friends");
            holder.mutualLabel.setVisibility(View.VISIBLE);
        } else {
            holder.mutualLabel.setVisibility(View.GONE);
        }

        // Personal note — show only if present
        String note = r.getMessage();
        if (note != null && !note.isEmpty()) {
            holder.noteText.setText("\"" + note + "\"");
            holder.noteText.setVisibility(View.VISIBLE);
        } else {
            holder.noteText.setVisibility(View.GONE);
        }

        Glide.with(context)
                .load(user.getProfilepic())
                .placeholder(R.drawable.man)
                .circleCrop()
                .into(holder.profileImage);

        // Reset button states
        holder.acceptBtn.setEnabled(true);
        holder.declineBtn.setEnabled(true);

        holder.acceptBtn.setOnClickListener(v -> {
            holder.acceptBtn.setEnabled(false);
            holder.declineBtn.setEnabled(false);
            connectionManager.acceptRequest(user.getUid(),
                    new ConnectionManager.Callback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(context, "Connected with "
                                    + user.getUsername(), Toast.LENGTH_SHORT).show();
                            // Firestore listener in the activity will remove the card
                        }
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            holder.acceptBtn.setEnabled(true);
                            holder.declineBtn.setEnabled(true);
                            Toast.makeText(context, "Failed — try again",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        holder.declineBtn.setOnClickListener(v -> {
            holder.acceptBtn.setEnabled(false);
            holder.declineBtn.setEnabled(false);
            connectionManager.declineRequest(user.getUid(),
                    new ConnectionManager.Callback() {
                        @Override
                        public void onSuccess() {}
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            holder.acceptBtn.setEnabled(true);
                            holder.declineBtn.setEnabled(true);
                        }
                    });
        });

        // Block option via long-press on the card
        holder.itemView.setOnLongClickListener(v -> {
            showBlockDialog(user);
            return true;
        });


    }

    private void showBlockDialog(User user) {
        new AlertDialog.Builder(context)
                .setTitle(user.getUsername())
                .setItems(new String[]{"Block " + user.getUsername(), "Cancel"},
                        (dialog, which) -> {
                            if (which == 0) blockUser(user);
                        })
                .show();
    }

    private void blockUser(User user) {
        connectionManager.blockUser(user.getUid(),
                new ConnectionManager.Callback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(context,
                                user.getUsername() + " blocked",
                                Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, "Could not block user",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class RequestVH extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView        username, mutualLabel, noteText;
        MaterialButton  acceptBtn, declineBtn;

        RequestVH(@NonNull View v) {
            super(v);
            profileImage = v.findViewById(R.id.profile_image);
            username     = v.findViewById(R.id.username_text);
            mutualLabel  = v.findViewById(R.id.mutual_label);
            noteText     = v.findViewById(R.id.note_text);
            acceptBtn    = v.findViewById(R.id.accept_button);
            declineBtn   = v.findViewById(R.id.decline_button);
        }
    }
}