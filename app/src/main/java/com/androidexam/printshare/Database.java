package com.androidexam.printshare;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;



public class Database{
    private DatabaseReference database_root;
    private DatabaseReference path;
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    Database(FirebaseDatabase db){
        database_root = db.getReference();
    }

    public void createAccount(final String email, String password, final String username, final String position, final Context context){
        //TODO validate email and password.
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Intent intent = new Intent(context,ProfileActivity.class);
                    Log.d("Success ", "createUserWithEmail:success");
                    FirebaseUser user = mAuth.getCurrentUser();
                    path = database_root.child("users").child(user.getUid()).child("metadata");
                    path.child("email").setValue(email);
                    path.child("username").setValue(username);
                    path.child("position").setValue(position);
                    path.child("printer").setValue("no printer");
                    context.startActivity(intent);
                } else {
                    Log.w("Something wrong ", "createUserWithEmail:failure", task.getException());
                }
            }
        });
    }

    public void logIn (String email, String password, final Context context){
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Intent intent = new Intent(context,ProfileActivity.class);
                            Log.d("Sign in ", "signInWithEmail:success");
                            context.startActivity(intent);
                        } else {
                            Log.d("Something wrong ","signInWithEmail:failure");
                        }
                    }
                });
    }

    //elimina tutti i dati dal db.
    public void clean(){
        database_root.removeValue();
    }
}
