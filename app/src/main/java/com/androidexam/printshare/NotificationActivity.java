package com.androidexam.printshare;

import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.androidexam.printshare.adapters.NotificationAdapter;
import com.androidexam.printshare.utilities.BackgroundNotify;
import com.androidexam.printshare.utilities.DbCommunication;
import com.androidexam.printshare.utilities.NotificationListItem;
import com.androidexam.printshare.utilities.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class NotificationActivity extends ActivityTemplate {

    private RecyclerView rv;
    private SessionManager sessionManager;
    private BackgroundNotify backgroundNotify;
    private Button sync_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        sessionManager = new SessionManager(this);
        backgroundNotify = new BackgroundNotify(this);

        sync_button = findViewById(R.id.sync_button);
        sync_button.setOnClickListener(v -> {
            backgroundNotify.sync();
            rv.setAdapter(new NotificationAdapter(dataInit(), this));
        });

        rv = findViewById(R.id.notification_rv).findViewById(R.id.my_recycle_view);
        rv.setAdapter(new NotificationAdapter(dataInit(), this));
    }

    @Override
    protected void onResume() {
        showOptions[0] = true;
        showOptions[1] = false;
        showOptions[2] = false;
        showOptions[3] = false;
        super.onResume();
    }

    private ArrayList<NotificationListItem> dataInit(){
        ArrayList<NotificationListItem> mData = new ArrayList<>();
        Callable<JSONObject> task = () -> new DbCommunication(DbCommunication.OPERATIONS.READ)
                .template("GET", FIREBASE_DB_ROOT_URL + "notify/" + sessionManager.getUsername(), null, null);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            Future<JSONObject> future = executor.submit(task);
            try {
                JSONObject obj = future.get();
                obj.keys().forEachRemaining( key -> {
                    try {
                        JSONObject current = obj.getJSONObject(key);
                        mData.add(new NotificationListItem(current.getString("type"), key, current.getString("content")));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if(!executor.isTerminated())
                executor.shutdownNow();
        }
        return mData;
    }
}