package com.androidexam.printshare;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends AppCompatActivity {

    private SharedPreferences savedValues;

    private ImageView profile_image;
    private TextView profile_username;
    private TextView profile_position;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);
        mAuth = FirebaseAuth.getInstance();

        savedValues = getSharedPreferences("ProfileSavedValues", MODE_PRIVATE);

        profile_image = findViewById(R.id.profile_image);
        profile_username = findViewById(R.id.profile_username);
        profile_position = findViewById(R.id.profile_position);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser current_user = mAuth.getCurrentUser();
        if(current_user != null){
            reload();
        }

    }

    @Override
    protected void onPause() {
        Editor editor = savedValues.edit();
        editor.putString("username", profile_username.getText().toString());
        editor.putString("position", profile_position.getText().toString());
        editor.commit();

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        profile_username.setText(savedValues.getString("username", ""));
        profile_position.setText(savedValues.getString("position", ""));
    }

    private void reload(){
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference();
        myRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot s : snapshot.getChildren()){
                    Log.i("READING DATA ", s.getKey()+", "+s.child("username").getValue());
                    if(s.getKey().equals(mAuth.getUid()))
                        showValues(s.child("metadata").child("username").getValue(String.class),
                                    s.child("metadata").child("position").getValue(String.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ERROR ", error.getMessage());
            }
        });
    }

    private void showValues(String username, String position){
        this.profile_username.setText(username);
        this.profile_position.setText(position);
    }
}