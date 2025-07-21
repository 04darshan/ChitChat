package com.example.chitchat;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ProgressBar load;
    ImageView logoutMain;
    FirebaseAuth auth;
    Button ys,no;
    RecyclerView recyclerView;
    Useradapter useradapter;
    FirebaseDatabase firebaseDatabase;
    ArrayList<User> userArrayList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        logoutMain=findViewById(R.id.logbtn);
        load=findViewById(R.id.progressBar);


        firebaseDatabase=FirebaseDatabase.getInstance();
        auth=FirebaseAuth.getInstance();

        DatabaseReference reference=firebaseDatabase.getReference().child("user");

        userArrayList=new ArrayList<>();
        reference.addValueEventListener(new ValueEventListener() {
            @Override

            public void onDataChange(@NonNull DataSnapshot snapshot) {
               
                userArrayList.clear(); // Clear the list first
                for (DataSnapshot dataSnapshot: snapshot.getChildren()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null && !user.getUid().equals(auth.getCurrentUser().getUid())) {
                        userArrayList.add(user); // Optional: exclude current user
                    }
                }
                useradapter.notifyDataSetChanged();
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        logoutMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog dialog=new Dialog(MainActivity.this);
                dialog.setContentView(R.layout.dialog_layout);
                ys=dialog.findViewById(R.id.yslgtmain);
                no=dialog.findViewById(R.id.nolgtmain);

                ys.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        FirebaseAuth.getInstance().signOut();
                        Intent intent =new Intent(MainActivity.this, login.class);
                        startActivity(intent);

                    }
                });

                no.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                dialog.show();

            }
        });

        recyclerView=findViewById(R.id.rcvmain);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        useradapter=new Useradapter(MainActivity.this,userArrayList);
        recyclerView.setAdapter(useradapter);


        if(auth.getCurrentUser()==null){
            Intent intent=new Intent(MainActivity.this,login.class);
            startActivity(intent);
            finish();
        }
    }

}