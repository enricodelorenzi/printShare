package com.androidexam.printshare;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.androidexam.printshare.adapters.PrinterAdapter;
import com.androidexam.printshare.utilities.BackgroundNotify;
import com.androidexam.printshare.utilities.DbCommunication;
import com.androidexam.printshare.utilities.MaterialListItem;
import com.androidexam.printshare.utilities.PrinterListItem;
import com.androidexam.printshare.utilities.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ActivityTemplate extends AppCompatActivity {
    protected static final String FIREBASE_DB_ROOT_URL = "https://printshare-77932-default-rtdb.firebaseio.com/";

    protected boolean[] showOptions = new boolean[5];

    protected void setParentLayoutOnFocusHideKeyboard(View v){
        v.setOnFocusChangeListener((view, hasFocus) -> {
            if(hasFocus)
                hideSoftKeyboard(view);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        for (int pos = 0;pos<4;pos++){
            menu.getItem(pos).setVisible(showOptions[pos]);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_home :
                startActivity(new Intent(this,ProfileActivity.class)
                .putExtra("FROM","REDIRECT"));
                return true;
            case R.id.menu_search:
                startActivity(new Intent(this,SearchActivity.class));
                return true;
            case R.id.menu_log_out: {
                startActivity(new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                new SessionManager(this).deleteSession();
                new BackgroundNotify(this).stop();
            } return true;
            case R.id.menu_notification: {
                startActivity(new Intent(this, NotificationActivity.class));
            } return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void hideSoftKeyboard(View v){
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);

        if (inputManager != null ) {
            inputManager.hideSoftInputFromWindow(v.getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    protected boolean isConnected(){
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo;
        if(connManager !=null)
            networkInfo = connManager.getActiveNetworkInfo();
        else
            return false;
        return networkInfo != null && networkInfo.isConnected();
    }

    protected String usernameToUid(String username){
        List<Callable<JSONObject>> tasks = new ArrayList<>();
        String[] uid = new String[1];
        tasks.add(usernameToUidTask(username));
        ExecutorService executor = Executors.newFixedThreadPool(1);

        try{
            executor.invokeAll(tasks).forEach(obj -> {
                try {
                    //ottengo json con chiave-valore: uid-username
                    uid[0] = (obj.get().keys().next());
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            });
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e){
            e.printStackTrace();
        } finally {
            if(!executor.isTerminated())
                executor.shutdownNow();
        }
        return uid[0];
    }

    private Callable<JSONObject> usernameToUidTask(String param){
        return () -> {
            List<String> queries = new ArrayList<>();
            Collections.addAll(queries,"orderBy=\""+ "$value" +"\"","equalTo="+"\""+param+"\"");
            return new DbCommunication().template("GET",FIREBASE_DB_ROOT_URL+"user_uid",
                    null, queries);
        };
    }

    protected void saveValueOnSharedPreferences(SharedPreferences sp, String key, String value){
        sp.edit().putString(key, value).apply();
    }

    protected String getFromOnSharedPreferences(SharedPreferences sp, String key){
        return  sp.getString(key,"");
    }

    protected ArrayList<MaterialListItem> materialsInit(){
        ArrayList<MaterialListItem> mData = new ArrayList<>();
        Arrays.stream(getResources().getStringArray(R.array.materials))
                .forEach(material -> {
                    mData.add(new MaterialListItem(material));
                });
        return mData;
    }

    protected ArrayList<PrinterListItem> printersInit(String uid , boolean editable){
        ArrayList<PrinterListItem> mData = new ArrayList<>();
        Callable<JSONObject> task = () -> new DbCommunication(DbCommunication.OPERATIONS.READ)
                .template("GET", FIREBASE_DB_ROOT_URL + "users/" + uid + "/printers", null, null);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            Future<JSONObject> future = executor.submit(task);
            try {
                JSONObject obj = future.get();

//////////////////preparing user's printers list items. [START]
                obj.keys().forEachRemaining( key -> {
                    try {
                        JSONObject current = obj.getJSONObject(key);
                        mData.add(new PrinterListItem(key ,current.getString("model"), editable));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
//////////////////preparing user's printers list items [END]
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

    protected void inputFailure(Context context, String title, String message){
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setNegativeButton("Ok", null).show();
    }

}
