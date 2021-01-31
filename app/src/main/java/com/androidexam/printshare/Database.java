package com.androidexam.printshare;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;


public class Database {
    private DatabaseReference database_root;
    private DatabaseReference specific_location;

    Database(){}

    Database(FirebaseDatabase db){
        database_root = db.getReference();
    }

    Database(FirebaseDatabase db, String child_path){
        database_root = db.getReference();
        specific_location = db.getReference(child_path);
    }

    DatabaseReference getDatabaseRoot(){
        return database_root;
    }


    //elimina tutti i dati dal db.
    public void clean(){
        database_root.removeValue();
    }

    //Utility

    public User readUserFromDb(final String username){
        final ArrayList<User> usrs = new ArrayList<>();
        specific_location = database_root.child("users"); //.child(username);
        specific_location.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //TODO read position of current user from db and return a new User object;
                for (DataSnapshot pair : snapshot.getChildren()){
                    String read_username = pair.getKey();
                    String read_position = pair.child("position").getValue(String.class);
                    Log.i("User info ",read_username + ", " + read_position);
                    usrs.add(new User("pippo","Parigi"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        //TODO synchronous callback.
        return new User();
    }

    public void register(User user){
        database_root.child("users").child(user.getUsername()).child("position").setValue(user.getPosition());
    }

    public boolean existId(String where_to_start){
        //TODO check if a username is alredy used.
        return true;
    }


}
