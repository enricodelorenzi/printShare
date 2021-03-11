package com.androidexam.printshare;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    private static final String FIREBASE_DB_ROOT_URL = "https://printshare-77932-default-rtdb.firebaseio.com/";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button registration = findViewById(R.id.home_registration);
        Button login = findViewById(R.id.home_login);
        Button search = findViewById(R.id.search_button);
        Button test = findViewById(R.id.test_button);

        test.setOnClickListener(this);
        search.setOnClickListener(this);
        registration.setOnClickListener(this);
        login.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        String s = null;
        if(v instanceof Button) {
            s = ((Button) v).getText().toString().toLowerCase();
        }
        if (s != null) {
            Intent intent;
            switch (s) {
                case "registration": {
                    intent = new Intent(v.getContext(), RegisterActivity.class);
                    startActivity(intent);
                }break;
                case "login": {
                    intent = new Intent(v.getContext(), LoginActivity.class);
                    startActivity(intent);
                }break;
                case "search": {
                    intent = new Intent(v.getContext(), SearchActivity.class);
                    startActivity(intent);
                }break;
                case "test":{
                    anonymousProfileView();
                }break;
                default:
                    throw new IllegalStateException("Unexpected value: ");
            }
        }
    }

    public void anonymousProfileView(){
        FirebaseAuth.getInstance().signInAnonymously();
        startActivity(new Intent(this,ProfileActivity.class).putExtra("USERNAME","user_1"));
    }

    public void parseGcode(){
        try {
            InputStream in = this.getAssets().open("cube.gcode");

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String s;
            while((s = reader.readLine()) != null){
                //ignore operative command
                if(!s.startsWith(";"))
                    continue;
                Log.i("GCODE",s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void test(){
        DbCommunication db = new DbCommunication();
        for(int i = 1; i < 201; i++) {
            /*String user_data = "{" +
                    "\"metadata\":{" +
                    "\"username\":\"user_" + i + "\"" +
                    "}" +
                    "}";
            db.launchAsyncTask("POST","users",user_data);
            */
            String material = "{" +
                    "\"user_" + i + "\":\"true\"" +
                    "}";
            if(i%2==0){
                db.launchAsyncTask("PATCH","materials/material_2",material);
            } else {
                db.launchAsyncTask("PATCH","materials/material_1",material);
            }
        }
    }
    
    public void limitRead(){
        Executor executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler();
        ArrayList<String> queries_1 = new  ArrayList<>();
        queries_1.add("orderBy=\"$key\"");
        queries_1.add("limitToFirst=20");
        
        executor.execute(()->{
            JSONObject obj;
            String next_startAt_value = null;
            int i = 0;
            do {
                if(i == 0) {
                    obj = new DbCommunication(DbCommunication.OPERATIONS.READ).template("GET",
                            FIREBASE_DB_ROOT_URL+"materials/material_1", null,
                            queries_1);
                }else {
                    ArrayList<String> queries_2 = new  ArrayList<>();
                    queries_2.add("orderBy=\"$key\"");
                    queries_2.add("startAt=\""+ next_startAt_value +"\"");
                    queries_2.add("limitToFirst=10");
                    obj = new DbCommunication(DbCommunication.OPERATIONS.READ).template("GET",
                            FIREBASE_DB_ROOT_URL+"materials/material_1", null,
                            queries_2);
                }
                Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    next_startAt_value = keys.next();
                }
                i++;
            } while(i < 10);
        });

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
