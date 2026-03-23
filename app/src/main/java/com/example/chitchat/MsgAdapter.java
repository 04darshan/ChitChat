package com.example.chitchat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * ENHANCED MsgAdapter
 * - BUG FIX: Null-check on FirebaseAuth.getCurrentUser() (was crashing when logged out)
 * - ENHANCEMENT: Shows formatted timestamp on each message bubble
 * - ENHANCEMENT: Shows "seen" checkmark on sender messages when isSeen == true
 * - ENHANCEMENT: Uses ViewHolder pattern correctly (no Firebase calls inside onBindViewHolder)
 */
public class MsgAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final ArrayList<msgModel> messageList;
    private final String senderImgUrl;
    private final String receiverImgUrl;

    private static final int ITEM_SEND    = 1;
    private static final int ITEM_RECEIVE = 2;

    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public MsgAdapter(Context context,
                      ArrayList<msgModel> messageList,
                      String senderImgUrl,
                      String receiverImgUrl) {
        this.context       = context;
        this.messageList   = messageList;
        this.senderImgUrl  = senderImgUrl;
        this.receiverImgUrl = receiverImgUrl;
    }

    @Override
    public int getItemViewType(int position) {
        msgModel message = messageList.get(position);
        // BUG FIX: Null-check on currentUser — was crashing when session expired
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null
                && auth.getCurrentUser().getUid().equals(message.getSenderId())) {
            return ITEM_SEND;
        }
        return ITEM_RECEIVE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == ITEM_SEND) {
            View view = inflater.inflate(R.layout.sender_layout, parent, false);
            return new SenderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.reciver_layout, parent, false);
            return new ReceiverViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        msgModel message = messageList.get(position);
        String formattedTime = timeFormat.format(new Date(message.getTimestamp()));

        if (holder instanceof SenderViewHolder) {
            SenderViewHolder senderHolder = (SenderViewHolder) holder;
            senderHolder.msgText.setText(message.getMessage());
            senderHolder.timestamp.setText(formattedTime);

            // Show "seen" double-tick when message has been read
            if (senderHolder.seenIndicator != null) {
                senderHolder.seenIndicator.setVisibility(
                        message.getIsSeen() ? View.VISIBLE : View.GONE);
            }

            // Load avatar (placeholder if null)
            Glide.with(context)
                    .load(senderImgUrl)
                    .placeholder(R.drawable.man)
                    .circleCrop()
                    .into(senderHolder.profileImage);

        } else if (holder instanceof ReceiverViewHolder) {
            ReceiverViewHolder receiverHolder = (ReceiverViewHolder) holder;
            receiverHolder.msgText.setText(message.getMessage());
            receiverHolder.timestamp.setText(formattedTime);

            Glide.with(context)
                    .load(receiverImgUrl)
                    .placeholder(R.drawable.man)
                    .circleCrop()
                    .into(receiverHolder.profileImage);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // -----------------------------------------------------------------------
    // ViewHolders
    // -----------------------------------------------------------------------
    static class SenderViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView msgText, timestamp;
        ImageView seenIndicator;

        SenderViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage  = itemView.findViewById(R.id.profilerggg);
            msgText       = itemView.findViewById(R.id.msgsendertyp);
            timestamp     = itemView.findViewById(R.id.msgTimestamp);
            seenIndicator = itemView.findViewById(R.id.seenIndicator);
        }
    }

    static class ReceiverViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView msgText, timestamp;

        ReceiverViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.pro);
            msgText      = itemView.findViewById(R.id.recivertextset);
            timestamp    = itemView.findViewById(R.id.msgTimestamp);
        }
    }
}