package com.androidexam.printshare;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ModifyProfileActivity extends AppCompatActivity {

    private Button confirm;
    private Button cancel;
    private TextView new_username_label;
    private TextView new_position_label;
    private EditText new_username_edit;
    private EditText new_position_edit;
    private SharedPreferences savedValues;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        savedValues = getSharedPreferences("ProfileSavedValues", MODE_PRIVATE);

        setContentView(R.layout.modify_profile_activity);

        new_username_label = findViewById(R.id.modify_profile_username_label);
        new_position_label = findViewById(R.id.modify_profile_position_label);
        new_username_edit = findViewById(R.id.modify_profile_username_edit);
        new_position_edit = findViewById(R.id.modify_profile_position_edit);
        confirm = findViewById(R.id.modify_profile_confirm_button);
        cancel = findViewById(R.id.modify_profile_cancel_button);

        confirm.setOnClickListener((v) ->{
            String new_username = new_username_edit.getText().toString();
            String new_position = new_position_edit.getText().toString();
            new DbCommunication(DbCommunication.OPERATIONS.PATCH).launchAsyncTask("PATCH", "users/"+FirebaseAuth.getInstance().getUid()+"/metadata",
                    "{\"username\":\""+new_username+"\"}");
            new DbCommunication(DbCommunication.OPERATIONS.PATCH).launchAsyncTask("PATCH", "users/"+FirebaseAuth.getInstance().getUid()+"/metadata",
                    "{\"position\":\""+new_position+"\"}");
            new DbCommunication(DbCommunication.OPERATIONS.PATCH).launchAsyncTask("PATCH", "user_pos",
                    "{\""+FirebaseAuth.getInstance().getUid()+"\":\""+new_position+"\"}");
            new DbCommunication(DbCommunication.OPERATIONS.PATCH).launchAsyncTask("PATCH", "user_uid",
                    "{\""+FirebaseAuth.getInstance().getUid()+"\":\""+new_username+"\"}");

            //set new values for Profile.onResume() call
            savedValues.edit()
                    .putString("username", new_username)
                    .putString("position", new_position)
                    .apply();
            finish();
        });

        cancel.setOnClickListener((v) -> {
            finish();
        });
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
