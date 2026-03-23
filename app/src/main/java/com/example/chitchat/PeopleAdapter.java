package com.example.chitchat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * PeopleAdapter — for the "People You May Know" list.
 *
 * Each card shows:
 *   - Profile picture
 *   - Username
 *   - Mutual friend label ("3 mutual friends")
 *   - [Connect] → opens SendRequestBottomSheet
 *   - [✕ Dismiss] → deletes suggestion doc silently
 */
public class PeopleAdapter extends RecyclerView.Adapter<PeopleAdapter.PeopleVH> {

    private final Context                             context;
    private final ArrayList<PeopleActivity.SuggestionWithUser> list;
    private final ConnectionManager                   connectionManager;
    private final String                              myUid;
    private final FirebaseFirestore                   db;

    public PeopleAdapter(Context context,
                         ArrayList<PeopleActivity.SuggestionWithUser> list,
                         ConnectionManager connectionManager,
                         String myUid) {
        this.context           = context;
        this.list              = list;
        this.connectionManager = connectionManager;
        this.myUid             = myUid;
        this.db                = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public PeopleVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_people_suggestion, parent, false);
        return new PeopleVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PeopleVH holder, int position) {
        PeopleActivity.SuggestionWithUser item = list.get(position);
        User       user       = item.user;
        Suggestion suggestion = item.suggestion;

        holder.username.setText(user.getUsername());
        holder.mutualLabel.setText(suggestion.getReason());

        // Hide mutual label if no mutuals (e.g. just new users nearby)
        holder.mutualLabel.setVisibility(
                suggestion.getMutualCount() > 0 ? View.VISIBLE : View.GONE);

        Glide.with(context)
                .load(user.getProfilepic())
                .placeholder(R.drawable.man)
                .circleCrop()
                .into(holder.profileImage);

        // Reset button state
        holder.connectBtn.setText("Connect");
        holder.connectBtn.setEnabled(true);

        // Connect → open bottom sheet to add optional note
        holder.connectBtn.setOnClickListener(v -> {
            holder.connectBtn.setEnabled(false);
            SendRequestBottomSheet sheet = SendRequestBottomSheet.newInstance(
                    user.getUid(),
                    user.getUsername(),
                    user.getProfilepic(),
                    suggestion.getMutualCount()
            );
            sheet.setOnRequestSentListener(success -> {
                if (success) {
                    holder.connectBtn.setText("Sent ✓");
                    // Remove from list after short delay
                    holder.itemView.postDelayed(() -> {
                        int pos = list.indexOf(item);
                        if (pos >= 0) {
                            list.remove(pos);
                            notifyItemRemoved(pos);
                        }
                    }, 600);
                } else {
                    holder.connectBtn.setEnabled(true);
                    Toast.makeText(context, "Failed to send request", Toast.LENGTH_SHORT).show();
                }
            });
            if (context instanceof androidx.fragment.app.FragmentActivity) {
                sheet.show(((androidx.fragment.app.FragmentActivity) context)
                        .getSupportFragmentManager(), "send_request");
            }
        });

        // Dismiss → remove suggestion silently
        holder.dismissBtn.setOnClickListener(v -> {
            db.collection("suggestions").document(myUid)
                    .collection("list").document(user.getUid())
                    .delete();
            int pos = list.indexOf(item);
            if (pos >= 0) {
                list.remove(pos);
                notifyItemRemoved(pos);
            }
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class PeopleVH extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView        username, mutualLabel;
        MaterialButton  connectBtn, dismissBtn;

        PeopleVH(@NonNull View v) {
            super(v);
            profileImage = v.findViewById(R.id.profile_image);
            username     = v.findViewById(R.id.username_text);
            mutualLabel  = v.findViewById(R.id.mutual_label);
            connectBtn   = v.findViewById(R.id.connect_button);
            dismissBtn   = v.findViewById(R.id.dismiss_button);
        }
    }
}