package com.example.chitchat;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class Useradapter extends RecyclerView.Adapter<Useradapter.viewholder> {
    MainActivity mainActivity;
    ArrayList<User> userArrayList;
    public Useradapter(MainActivity mainActivity, ArrayList<User> userArrayList) {
        this.mainActivity=mainActivity;
        this.userArrayList=userArrayList;
    }

    @NonNull
    @Override
    public Useradapter.viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(mainActivity).inflate(R.layout.user_item,parent,false);

        return new viewholder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull Useradapter.viewholder holder, int position) {

        User user=userArrayList.get(position);
        holder.username.setText(user.username);
        holder.status.setText(user.status);
        holder.userimg.setImageResource(R.drawable.man);
//        Picasso.get().load(String.valueOf(user.img)).into(holder.userimg);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(mainActivity, chatscreen.class);
                intent.putExtra("nameee",user.getUsername());
                intent.putExtra("reciverImg",user.getProfilepic());
                intent.putExtra("uid",user.getUid());
                mainActivity.startActivity(intent);
            }
        });

    }

    @Override
    public int getItemCount() {
        return userArrayList.size();
    }

    public class viewholder extends RecyclerView.ViewHolder{
        CircleImageView userimg;
        TextView username,status;
        public viewholder(@NonNull View itemView) {
            super(itemView);
            userimg=itemView.findViewById(R.id.llprofile_image);
            username=itemView.findViewById(R.id.llname);
            status=itemView.findViewById(R.id.llstatus);
        }
    }
}
