package com.androidexam.printshare;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Base64;

public class SearchedProfileActivity extends AppCompatActivity {
    private String profile_uid;

    private SharedPreferences savedValues;

    private ImageView searched_profile_image;
    private TextView searched_profile_username;
    private TextView searched_profile_position;
    private Button searched_profile_contact_button;
    private FirebaseAuth mAuth;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.searched_profile_activity);
        mAuth = FirebaseAuth.getInstance();

        savedValues = getSharedPreferences("ProfileSavedValues", MODE_PRIVATE);

        searched_profile_image = findViewById(R.id.search_profile_image);
        searched_profile_username = findViewById(R.id.search_profile_username);
        searched_profile_position = findViewById(R.id.search_profile_position);
        searched_profile_contact_button = findViewById(R.id.contact_button);

        intent = this.getIntent();
        searched_profile_contact_button.setOnClickListener((v)->{});

    }

    @Override
    protected void onStart() {
        super.onStart();
        new DbCommunication(DbCommunication.OPERATIONS.READ, searched_profile_image).launchAsyncTask("READ","default_profile_image", null);
        new DbCommunication(DbCommunication.OPERATIONS.READ_USERNAME, searched_profile_username).launchAsyncTask("READ","users" ,
                null,
                "orderBy=\"metadata/username\"",
                "equalTo=\""+intent.getStringExtra("SEARCHED_USER")+"\"");
        new DbCommunication(DbCommunication.OPERATIONS.READ_POSITION, searched_profile_position).launchAsyncTask("READ","users",
                null,
                "orderBy=\"metadata/username\"",
                "equalTo=\""+intent.getStringExtra("SEARCHED_USER")+"\"");
    }

    @Override
    protected void onPause() {
        Editor editor = savedValues.edit();
        editor.putString("username", searched_profile_username.getText().toString());
        editor.putString("position", searched_profile_position.getText().toString());
        editor.apply();

        super.onPause();
    }

    //ATTENZIONE: onResume() viene chiamata dopo onStart()!!!
    @Override
    protected void onResume() {
        super.onResume();
        searched_profile_username.setText(savedValues.getString("username", ""));
        searched_profile_position.setText(savedValues.getString("position", ""));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_home :
                startActivity(new Intent(this,MainActivity.class));
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this,SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}