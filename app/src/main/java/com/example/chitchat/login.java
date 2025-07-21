package com.example.chitchat;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class login extends AppCompatActivity {
Button btnl;
EditText email,pass;
TextView lgts;
ProgressBar load;

FirebaseAuth auth;
    String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth=FirebaseAuth.getInstance();
        btnl=findViewById(R.id.loginButton);
        email=findViewById(R.id.loginEmail);
        pass=findViewById(R.id.loginPassword);
        lgts=findViewById(R.id.lgntosignup);
        ProgressBar progressBar = findViewById(R.id.progressBar);

        
        btnl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               String Email=email.getText().toString();
               String Pass=pass.getText().toString();

               if(TextUtils.isEmpty(Email)){
                   Toast.makeText(login.this, "Enter An Email", Toast.LENGTH_SHORT).show();
               }else if(TextUtils.isEmpty(Pass)){
                   Toast.makeText(login.this, "Enter a Password", Toast.LENGTH_SHORT).show();
               }else if(!Email.matches(emailPattern)){
                email.setError("Give Proper Email");
               } else if (pass.length()<6) {
                   pass.setError("More than 6 characters");
                   Toast.makeText(login.this, "Password Must be longer than 6 characters", Toast.LENGTH_SHORT).show();
               }else {
                   progressBar.setVisibility(View.VISIBLE);
                   auth.signInWithEmailAndPassword(Email,Pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                       @Override
                       public void onComplete(@NonNull Task<AuthResult> task) {
                           if(task.isSuccessful()){
                                   progressBar.setVisibility(View.GONE);
                               try{

                                   Intent intent=new Intent(login.this, MainActivity.class);
                                   startActivity(intent);
                                   finish();
                               }catch(Exception e){
                                   Toast.makeText(login.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                               }
                           }else{
                               Toast.makeText(login.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                               progressBar.setVisibility(View.GONE);
                           }
                       }
                   });
               }


            }
        });

        lgts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(login.this, registration.class);
                startActivity(intent);
                finish();
            }
        });
    }
}