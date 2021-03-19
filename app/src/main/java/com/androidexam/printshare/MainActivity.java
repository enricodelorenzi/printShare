package com.androidexam.printshare;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.ArraySet;
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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class MainActivity extends AppCompatActivity implements OnClickListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button registration = findViewById(R.id.home_registration);
        Button login = findViewById(R.id.home_login);
        Button search = findViewById(R.id.home_search_button);

        search.setOnClickListener(this);
        registration.setOnClickListener(this);
        login.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
            Intent intent;
            switch (v.getId()) {
                case R.id.home_registration: {
                    intent = new Intent(v.getContext(), RegisterActivity.class);
                    startActivity(intent);
                }break;
                case R.id.home_login: {
                    intent = new Intent(v.getContext(), LoginActivity.class);
                    startActivity(intent);
                }break;
                case R.id.home_search_button: {
                    intent = new Intent(v.getContext(), SearchActivity.class);
                    startActivity(intent);
                }break;
                default:
                    throw new IllegalStateException("Unexpected value: ");
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
