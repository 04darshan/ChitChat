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
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * MsgAdapter — Firestore version
 * Uses Firestore Timestamp for message time formatting.
 */
public class MsgAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context            context;
    private final ArrayList<msgModel> messageList;
    private final String             senderImgUrl;
    private final String             receiverImgUrl;

    private static final int ITEM_SEND    = 1;
    private static final int ITEM_RECEIVE = 2;

    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public MsgAdapter(Context context, ArrayList<msgModel> messageList,
                      String senderImgUrl, String receiverImgUrl) {
        this.context        = context;
        this.messageList    = messageList;
        this.senderImgUrl   = senderImgUrl;
        this.receiverImgUrl = receiverImgUrl;
    }

    @Override
    public int getItemViewType(int position) {
        msgModel msg = messageList.get(position);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null
                && auth.getCurrentUser().getUid().equals(msg.getSenderId())) {
            return ITEM_SEND;
        }
        return ITEM_RECEIVE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(context);
        if (viewType == ITEM_SEND) {
            return new SenderVH(inf.inflate(R.layout.sender_layout, parent, false));
        }
        return new ReceiverVH(inf.inflate(R.layout.reciver_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        msgModel msg = messageList.get(position);

        // Format Firestore Timestamp → "hh:mm a"
        String time = "";
        Timestamp ts = msg.getTimestamp();
        if (ts != null) {
            time = timeFormat.format(ts.toDate());
        }

        if (holder instanceof SenderVH) {
            SenderVH h = (SenderVH) holder;
            h.msgText.setText(msg.getMessage());
            h.timestamp.setText(time);
            if (h.seenIndicator != null) {
                h.seenIndicator.setVisibility(msg.getIsSeen() ? View.VISIBLE : View.GONE);
            }
            Glide.with(context).load(senderImgUrl)
                    .placeholder(R.drawable.man).circleCrop().into(h.profileImage);

        } else if (holder instanceof ReceiverVH) {
            ReceiverVH h = (ReceiverVH) holder;
            h.msgText.setText(msg.getMessage());
            h.timestamp.setText(time);
            Glide.with(context).load(receiverImgUrl)
                    .placeholder(R.drawable.man).circleCrop().into(h.profileImage);
        }
    }

    @Override
    public int getItemCount() { return messageList.size(); }

    // ── ViewHolders ──────────────────────────────────────────────────
    static class SenderVH extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView        msgText, timestamp;
        ImageView       seenIndicator;

        SenderVH(@NonNull View v) {
            super(v);
            profileImage  = v.findViewById(R.id.profilerggg);
            msgText       = v.findViewById(R.id.msgsendertyp);
            timestamp     = v.findViewById(R.id.msgTimestamp);
            seenIndicator = v.findViewById(R.id.seenIndicator);
        }
    }

    static class ReceiverVH extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView        msgText, timestamp;

        ReceiverVH(@NonNull View v) {
            super(v);
            profileImage = v.findViewById(R.id.pro);
            msgText      = v.findViewById(R.id.recivertextset);
            timestamp    = v.findViewById(R.id.msgTimestamp);
        }
    }
}