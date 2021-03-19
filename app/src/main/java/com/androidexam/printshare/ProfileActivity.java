package com.androidexam.printshare;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.Executors;

public class ProfileActivity extends AppCompatActivity {
    private static final String FIREBASE_DB_ROOT_URL = "https://printshare-77932-default-rtdb.firebaseio.com/";

    private SharedPreferences savedValues;

    private ImageView profile_image;
    private TextView profile_username;
    private TextView profile_position;
    private ImageButton profile_add_printer_button;
    private Button profile_modify_button;
    private Button profile_contact_button;
    private FirebaseAuth mAuth;
    private Intent intent;
    private boolean fromRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);
        mAuth = FirebaseAuth.getInstance();

        savedValues = getSharedPreferences("ProfileSavedValues", MODE_PRIVATE);

        profile_image = findViewById(R.id.profile_image);
        profile_username = findViewById(R.id.profile_username);
        profile_position = findViewById(R.id.profile_position);
        profile_add_printer_button = findViewById(R.id.profile_add_printer_button);
        profile_modify_button = findViewById(R.id.profile_modify_button);
        profile_contact_button = findViewById(R.id.profile_contact_button);

        intent = this.getIntent();
        fromRegistration = intent.hasExtra("USERNAME") && intent.hasExtra("POSITION");

        if (!fromRegistration)
            reload();

        profile_add_printer_button.setOnClickListener((v)->{
            startActivity(new Intent(this, AddPrinterActivity.class));
        });

        profile_modify_button.setOnClickListener((v)->{
            startActivity((new Intent(this,ModifyProfileActivity.class)));
        });

        profile_add_printer_button.setVisibility(View.GONE);
        profile_modify_button.setVisibility(View.GONE);
        profile_contact_button.setVisibility(View.GONE);

    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser current_user = mAuth.getCurrentUser();
        if(current_user != null) {
            Handler handler = new Handler();
            Executors.newSingleThreadExecutor().execute(()->{
                ArrayList<String> queries = new ArrayList<>();
                Collections.addAll(queries,"orderBy=\"$value\"","equalTo\""+
                        savedValues.getString("username","")+"\"");
                JSONObject response = new DbCommunication(DbCommunication.OPERATIONS.READ).template("GET",
                      FIREBASE_DB_ROOT_URL+"user_uid",null, queries);
                handler.post(() -> {
                    if(!fromRegistration && response != null){
                            if(current_user.getUid().equals(response.keys().next())){
                                ownerView();
                            } else {
                                defaultView();
                            }
                    }
                });
            });
        }
    }

    @Override
    protected void onPause() {
        Editor editor = savedValues.edit();
        editor.putString("username", profile_username.getText().toString());
        editor.putString("position", profile_position.getText().toString());
        editor.apply();

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (fromRegistration) {
            profile_position.setText(intent.getStringExtra("POSITION"));
            profile_username.setText(intent.getStringExtra("USERNAME"));
            ownerView();
        } else {
            profile_username.setText(savedValues.getString("username", ""));
            profile_position.setText(savedValues.getString("position", ""));
        }
    }

    private void reload() {
        new DbCommunication(DbCommunication.OPERATIONS.READ, profile_image).launchAsyncTask("GET","default_profile_image",null);
        new DbCommunication(DbCommunication.OPERATIONS.READ, profile_username).launchAsyncTask("GET","users/" + mAuth.getUid() + "/metadata/username",null);
        new DbCommunication(DbCommunication.OPERATIONS.READ, profile_position).launchAsyncTask("GET","users/" + mAuth.getUid() + "/metadata/position",null);
    }

    private void ownerView(){
        this.profile_modify_button.setVisibility(View.VISIBLE);
        this.profile_add_printer_button.setVisibility(View.VISIBLE);
        this.profile_contact_button.setVisibility(View.GONE);
    }

    private void defaultView(){
        this.profile_modify_button.setVisibility(View.GONE);
        this.profile_add_printer_button.setVisibility(View.GONE);
        this.profile_contact_button.setVisibility(View.VISIBLE);
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