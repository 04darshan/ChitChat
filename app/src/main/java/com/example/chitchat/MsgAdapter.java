package com.example.chitchat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // NEW: Import Glide
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class MsgAdapter extends RecyclerView.Adapter {

    Context context;
    ArrayList<msgModel> messageAdapeerArraylist;
    int ITEM_SEND = 1;
    int ITEM_RECEIVE = 2;

    // NEW: Add fields for image URLs
    String senderImgUrl;
    String receiverImgUrl;

    // UPDATED: Constructor now accepts image URLs
    public MsgAdapter(Context context, ArrayList<msgModel> messageAdapeerArraylist, String senderImgUrl, String receiverImgUrl) {
        this.context = context;
        this.messageAdapeerArraylist = messageAdapeerArraylist;
        this.senderImgUrl = senderImgUrl;
        this.receiverImgUrl = receiverImgUrl;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_SEND) {
            View view = LayoutInflater.from(context).inflate(R.layout.sender_layout, parent, false);
            return new senderViewholder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.reciver_layout, parent, false);
            return new reciverViewholder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        msgModel messages = messageAdapeerArraylist.get(position);

        if (holder.getClass() == senderViewholder.class) {
            senderViewholder viewholder = (senderViewholder) holder;
            viewholder.msgtxt.setText(messages.getMessage());
            // NEW: Load sender's image
            Glide.with(context).load(senderImgUrl).placeholder(R.drawable.man).into(viewholder.circleImageView);
        } else {
            reciverViewholder viewholder = (reciverViewholder) holder;
            viewholder.msgtxt.setText(messages.getMessage());
            // NEW: Load receiver's image
            Glide.with(context).load(receiverImgUrl).placeholder(R.drawable.man).into(viewholder.circleImageView);
        }
    }

    @Override
    public int getItemCount() {
        return messageAdapeerArraylist.size();
    }

    @Override
    public int getItemViewType(int position) {
        msgModel messages = messageAdapeerArraylist.get(position);
        if (FirebaseAuth.getInstance().getCurrentUser().getUid().equals(messages.getSenderId())) {
            return ITEM_SEND;
        } else {
            return ITEM_RECEIVE;
        }
    }

    class senderViewholder extends RecyclerView.ViewHolder {
        CircleImageView circleImageView;
        TextView msgtxt;

        public senderViewholder(@NonNull View itemView) {
            super(itemView);
            circleImageView = itemView.findViewById(R.id.profilerggg);
            msgtxt = itemView.findViewById(R.id.msgsendertyp);
        }
    }

    class reciverViewholder extends RecyclerView.ViewHolder {
        CircleImageView circleImageView;
        TextView msgtxt;

        public reciverViewholder(@NonNull View itemView) {
            super(itemView);
            circleImageView = itemView.findViewById(R.id.pro);
            msgtxt = itemView.findViewById(R.id.recivertextset);
        }
    }
}