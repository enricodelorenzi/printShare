package com.androidexam.printshare;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.widget.ViewSwitcher;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.androidexam.printshare.adapters.PrinterAdapter;
import com.androidexam.printshare.utilities.BackgroundNotify;
import com.androidexam.printshare.utilities.DbCommunication;
import com.androidexam.printshare.utilities.PrinterListItem;
import com.androidexam.printshare.utilities.SessionManager;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ProfileActivity extends ActivityTemplate {
    private SharedPreferences savedValues;

    private TextView profile_username;
    private TextView profile_position;
    private ImageButton profile_add_printer_button;
    private Button profile_modify_button;
    private Button profile_contact_button;
    private FirebaseAuth mAuth;
    private Intent intent;
    private RecyclerView recyclerView;
    private ViewSwitcher viewSwitcher;

    SessionManager sessionManager;
    BackgroundNotify backgroundNotify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);

        mAuth = FirebaseAuth.getInstance();

        savedValues = getSharedPreferences("ProfileSavedValues", MODE_PRIVATE);

        sessionManager = new SessionManager(this);

        profile_username = findViewById(R.id.profile_username);
        profile_position = findViewById(R.id.profile_position);
        profile_add_printer_button = findViewById(R.id.profile_add_printer_button);
        profile_modify_button = findViewById(R.id.profile_modify_button);
        profile_contact_button = findViewById(R.id.profile_contact_button);
        TextView empty_list = findViewById(R.id.empty_list_tv);
        viewSwitcher = findViewById(R.id.switcher);

        intent = getIntent();

        profile_add_printer_button.setOnClickListener((v)->{
            startActivity(new Intent(this, AddPrinterActivity.class));
        });

        profile_modify_button.setOnClickListener((v)->{
            startActivity((new Intent(this,ModifyProfileActivity.class)
                            .putExtra("username",profile_username.getText().toString())
                            .putExtra("position", profile_position.getText().toString())));
        });

        profile_contact_button.setOnClickListener((v) -> {
            DbCommunication db = new DbCommunication();
            db.addNotification(sessionManager.getUsername(), profile_username.getText().toString(),"contact", true, "NONE");
            new AlertDialog.Builder(this).setTitle("Notification sent")
                .setMessage("User contacted.")
                .setNeutralButton("ok", (dialog, which) -> {
                    startActivity(new Intent(this, SearchActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                }).show();
        });

        profile_add_printer_button.setVisibility(View.GONE);
        profile_modify_button.setVisibility(View.GONE);
        profile_contact_button.setVisibility(View.GONE);

        recyclerView = findViewById(R.id.my_recycle_view);

    }

    private ArrayList<PrinterListItem> initData(String uid){
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
                        mData.add(new PrinterListItem(key ,current.getString("model"), true));
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

    @Override
    protected void onStart() {
        super.onStart();
        if(!isConnected()){
            new AlertDialog.Builder(this)
                    .setTitle("Internet connection")
                    .setMessage("There's no internet connection.")
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setNegativeButton("Ok", (dialog, which) -> {
                        sessionManager.deleteSession();
                        this.startActivity(new Intent(this, MainActivity.class));
                    }).show();
        }
        if(intent.hasExtra("FROM")) {
            String user_uid;
            switch (intent.getStringExtra("FROM")) {
                case "LOGIN": {
                    user_uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                    reload(user_uid, true);
                }
                break;
                case "REG": {
                    profile_username.setText(intent.getStringExtra("USERNAME"));
                    profile_position.setText(intent.getStringExtra("POSITION"));
                    if (R.id.empty_list_tv == viewSwitcher.getNextView().getId())
                        viewSwitcher.showNext();
                    profile_modify_button.setVisibility(View.VISIBLE);
                    profile_add_printer_button.setVisibility(View.VISIBLE);
                }
                break;
                case "SEARCH": {
                    user_uid = usernameToUid(intent.getStringExtra("USERNAME"));
                    reload(user_uid, false);
                }
                break;
                case "MODIFY":{
                    profile_username.setText(intent.getStringExtra("NEW_USERNAME"));
                    profile_position.setText(intent.getStringExtra("NEW_POSITION"));
                    populateRV(sessionManager.getUid());
                }
                case "REDIRECT": {
                    redirected();
                }
                break;
                case "NONE":
                default:
                    break;
            }
            intent.removeExtra("FROM");
        } else {
            profile_username.setText(savedValues.getString("username",""));
            profile_position.setText(savedValues.getString("position",""));
            if(sessionManager.getUsername().equals(profile_username.getText().toString())) {
                ownerView();
                populateRV(sessionManager.getUid());
            }
            else {
                populateRV(usernameToUid(savedValues.getString("username","")));
                defaultView();
            }
        }
        showOptions[1] = true;
        showOptions[2] = true;
        showOptions[3] = true;
    }

    @Override
    protected void onPause() {
        Editor editor = savedValues.edit();
        editor.putString("username", profile_username.getText().toString());
        editor.putString("position", profile_position.getText().toString());
        editor.apply();

        super.onPause();
    }

    private void reload(String uid, boolean newSession) {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            executor.invokeAll(Arrays.asList(
                    () -> new DbCommunication(DbCommunication.OPERATIONS.READ, profile_username).template("GET",FIREBASE_DB_ROOT_URL+"users/" + uid + "/metadata/username",null, null),
                    () -> new DbCommunication(DbCommunication.OPERATIONS.READ, profile_position).template("GET",FIREBASE_DB_ROOT_URL+"users/" + uid + "/metadata/position",null, null)
            ));
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if(!executor.isTerminated())
                executor.shutdownNow();
            String username = profile_username.getText().toString();
            String position = profile_position.getText().toString();
            savedValues.edit().putString("username",username)
                    .putString("position", position).apply();
            if(newSession) {
                createNewSession(uid, username, position);
            }
            populateRV(uid);
        }

        //TODO quando premo back carica la schermata con i dati dell'ultimo profilo visualizzato
        //check user == owner of visualized profile
        if(sessionManager.getUid().equals(uid))
            ownerView();
        else
            defaultView();
    }

    private void redirected() {
        populateRV(sessionManager.getUid());
        this.profile_username.setText(sessionManager.getUsername());
        this.profile_position.setText(sessionManager.getOrigin());
        ownerView();
    }

    private void ownerView(){
        this.profile_modify_button.setVisibility(View.VISIBLE);
        this.profile_add_printer_button.setVisibility(View.VISIBLE);
        this.profile_contact_button.setVisibility(View.GONE);

        showOptions[0] = false;
    }

    private void defaultView(){
        this.profile_modify_button.setVisibility(View.GONE);
        this.profile_add_printer_button.setVisibility(View.GONE);
        this.profile_contact_button.setVisibility(View.VISIBLE);

        showOptions[0] = true;
    }

    private void populateRV(String uid){
        ArrayList<PrinterListItem> mData = printersInit(uid, false);
        if(Objects.isNull(mData)) {
            if (R.id.empty_list_tv == viewSwitcher.getNextView().getId())
                viewSwitcher.showNext();
        } else if(mData.size() > 0) {
            recyclerView.setAdapter(new PrinterAdapter(mData, this, null));
            if(R.id.my_recycle_view == viewSwitcher.getNextView().getId())
                viewSwitcher.showNext();
        } else {
            if(R.id.empty_list_tv == viewSwitcher.getNextView().getId())
                viewSwitcher.showNext();
        }
    }



    private void createNewSession(String uid, String username, String place){
            sessionManager.createSession(uid, username, place);
            backgroundNotify = new BackgroundNotify(this);
            backgroundNotify.start();
    }
}