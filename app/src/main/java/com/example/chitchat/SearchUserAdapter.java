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
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * SearchUserAdapter — FIXED
 *
 * BUG: Was writing to old path  friend_requests/{uid}/requests/{sender}
 *      FriendRequestsActivity listens on  connections/{uid}/requests
 *      So requests were written to wrong place — never appeared.
 *
 * FIX: Now uses ConnectionManager.sendRequest() which writes to the
 *      correct path  connections/{recipientUid}/requests/{senderUid}
 *      AND also writes the sent copy to connections/{myUid}/sent/{recipient}
 *      — so both paths stay in sync.
 */
public class SearchUserAdapter extends RecyclerView.Adapter<SearchUserAdapter.SearchVH> {

    private final Context           context;
    private final ArrayList<User>   userList;
    private final ArrayList<String> friendUids;
    private final ArrayList<String> sentRequestUids;
    private final String            myUid;

    public SearchUserAdapter(Context context, ArrayList<User> userList,
                             ArrayList<String> friendUids,
                             ArrayList<String> sentRequestUids) {
        this.context         = context;
        this.userList        = userList;
        this.friendUids      = friendUids;
        this.sentRequestUids = sentRequestUids;
        this.myUid           = FirebaseAuth.getInstance().getUid();
    }

    @NonNull
    @Override
    public SearchVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.search_user_item, parent, false);
        return new SearchVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchVH holder, int position) {
        User user = userList.get(position);
        holder.username.setText(user.getUsername());
        Glide.with(context)
                .load(user.getProfilepic())
                .placeholder(R.drawable.man)
                .circleCrop()
                .into(holder.profileImage);

        // Set button state based on current relationship
        updateButtonState(holder.addBtn, user.getUid());

        holder.addBtn.setOnClickListener(v -> {
            holder.addBtn.setEnabled(false);
            sendRequest(holder.addBtn, user.getUid());
        });
    }

    private void updateButtonState(MaterialButton btn, String uid) {
        if (friendUids.contains(uid)) {
            btn.setText("Friends ✓");
            btn.setEnabled(false);
        } else if (sentRequestUids.contains(uid)) {
            btn.setText("Sent");
            btn.setEnabled(false);
        } else {
            btn.setText("Add");
            btn.setEnabled(true);
        }
    }

    /**
     * FIX: Use ConnectionManager so the request is written to the
     * correct path that FriendRequestsActivity listens on:
     *   connections/{recipientUid}/requests/{senderUid}
     *
     * No message passed from search screen — user can add a note
     * from the PeopleActivity via SendRequestBottomSheet instead.
     */
    private void sendRequest(MaterialButton btn, String recipientUid) {
        if (myUid == null) return;

        ConnectionManager mgr = new ConnectionManager(myUid);
        mgr.sendRequest(
                recipientUid,
                "",   // no note from search screen
                new ConnectionManager.Callback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(context,
                                "Request sent!", Toast.LENGTH_SHORT).show();
                        btn.setText("Sent");
                        // sentRequestUids listener in SearchUsersActivity
                        // will update the button state automatically
                    }

                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context,
                                "Failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        btn.setEnabled(true);
                    }
                }
        );
    }

    @Override
    public int getItemCount() { return userList.size(); }

    static class SearchVH extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView        username;
        MaterialButton  addBtn;

        SearchVH(@NonNull View v) {
            super(v);
            profileImage = v.findViewById(R.id.profile_image);
            username     = v.findViewById(R.id.username_text);
            addBtn       = v.findViewById(R.id.add_friend_button);
        }
    }
}