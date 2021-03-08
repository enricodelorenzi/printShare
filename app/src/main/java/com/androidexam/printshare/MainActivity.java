package com.androidexam.printshare;

import android.content.Intent;
import android.content.res.AssetManager;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements OnClickListener {

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
                    test_1();
                }break;
                default:
                    throw new IllegalStateException("Unexpected value: ");
            }
        }
    }

    private void test_1(){
        Handler handler = new Handler();
        Executors.newFixedThreadPool(1).execute(()->{
            HttpURLConnection urlConnection = null;
            try {
                urlConnection = (HttpURLConnection) new URL("https://printshare-77932-default-rtdb.firebaseio.com/user_pos.json").openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (urlConnection != null) {
                    urlConnection.setRequestMethod("PATCH");
                }
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
            if (urlConnection != null) {
                urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.setRequestProperty("Content-Type", "application/json; utf-8");
                urlConnection.setDoOutput(true);
            }

            try {
                urlConnection.connect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String data = "{\"USERNAME\":\"LOCATION\"}";

            OutputStream outputStream = null;
            try {
                outputStream = urlConnection.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            byte[] input = data.getBytes(StandardCharsets.UTF_8);
            try {
                outputStream.write(input, 0, input.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                InputStream in = urlConnection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null) {
                    Log.d("READED", line);
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        });
        //new DbCommunication().launchAsyncTask("PATCH","user_pos", "{\"USERNAME\":\"PLACE\"}");
    }

    private void test(){
        Handler handler = new Handler();
        Executors.newSingleThreadExecutor().execute(()-> {
            BufferedReader reader = null;
            InputStream input = null;
            StringBuilder builder = new StringBuilder();
            try {
                HttpURLConnection urlConnection = (HttpURLConnection) new URL("https://printshare-77932-default-rtdb.firebaseio.com/users.json?" +
                        "orderBy=\"metadata/username\"&equalTo=\"NEWREGISTRATION\"").openConnection();
                urlConnection.connect();
                input = urlConnection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(input));
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (reader != null)
                        reader.close();
                    if (input != null)
                        input.close();
                } catch(IOException e){
                    e.printStackTrace();
                }
            }
            handler.post(() -> {
                builder.toString();
            });
        });
    }

    public void parseGcode(String file_path){
        try {
            InputStream in = this.getAssets().open("cube_2.gcode");
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
