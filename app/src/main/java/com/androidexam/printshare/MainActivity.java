package com.androidexam.printshare;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.androidexam.printshare.utilities.SessionManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Objects;

public class MainActivity extends ActivityTemplate implements OnClickListener {

    private static final String CHANNEL_ID ="printShare";
    private SessionManager sessionManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button registration = findViewById(R.id.home_registration);
        Button login = findViewById(R.id.home_login);

        sessionManager = new SessionManager(this);

        registration.setOnClickListener(this);
        login.setOnClickListener(this);
////////////////////////////////////////////////////////////////////////
        createNotificationChannel();
////////////////////////////////////////////////////////////////////////
    }

    @Override
    protected void onStart() {
        super.onStart();
        //prima installazione: non esiste il file -> nullPointerException
        if(Objects.nonNull(sessionManager.getUid()))
            if(!sessionManager.getUid().equals(""))
                startActivity(new Intent(this, ProfileActivity.class)
                                    .putExtra("FROM","REDIRECT"));
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
                default:
                    throw new IllegalStateException("Unexpected value: ");
            }
    }

    @Override
    protected void onResume() {
        showOptions[0] = false;
        showOptions[1] = false;
        showOptions[2] = false;
        super.onResume();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
