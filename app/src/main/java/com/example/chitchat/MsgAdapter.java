package com.example.chitchat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class MsgAdapter extends RecyclerView.Adapter {

    Context context;
    ArrayList<msgModel> messageAdapeerArraylist;
    int ITEM_SEND = 1;
    int ITEM_RECEIVE = 2;

    public MsgAdapter(Context context, ArrayList<msgModel> messageAdapeerArraylist) {
        this.context = context;
        this.messageAdapeerArraylist = messageAdapeerArraylist;
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

        if (holder instanceof senderViewholder) {
            senderViewholder viewholder = (senderViewholder) holder;
            viewholder.msgtxt.setText(messages.getMessage());
            viewholder.circleImageView.setImageResource(R.drawable.man);
        } else {
            reciverViewholder viewholder = (reciverViewholder) holder;
            viewholder.msgtxt.setText(messages.getMessage());
            viewholder.circleImageView.setImageResource(R.drawable.man);
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
