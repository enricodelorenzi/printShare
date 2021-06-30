package com.androidexam.printshare;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.androidexam.printshare.adapters.PrinterAdapter;
import com.androidexam.printshare.utilities.BackgroundNotify;
import com.androidexam.printshare.utilities.DbCommunication;
import com.androidexam.printshare.utilities.PrinterListItem;
import com.androidexam.printshare.utilities.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ModifyProfileActivity extends ActivityTemplate {

    private static final String FIREBASE_DB_ROOT_URL = "https://printshare-77932-default-rtdb.firebaseio.com/";
    private Button confirm;
    private ImageButton cancel;
    private Button delete_account;
    private EditText username_et;
    private EditText position_et;
    private SharedPreferences savedValues;

    private SessionManager session;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        savedValues = getSharedPreferences("ProfileSavedValues", MODE_PRIVATE);
        session = new SessionManager(this);


        Intent receivedIntent = getIntent();
        savedValues.edit().putString("username",receivedIntent.getStringExtra("username"))
                .putString("position", receivedIntent.getStringExtra("position")).apply();

        setContentView(R.layout.modify_profile_activity);
        setParentLayoutOnFocusHideKeyboard(findViewById(R.id.modify_main_layout));

        username_et = findViewById(R.id.username_et);
        position_et = findViewById(R.id.position_et);
        confirm = findViewById(R.id.modify_profile_confirm_button);
        cancel = findViewById(R.id.modify_profile_cancel_button);
        delete_account = findViewById(R.id.delete_account_button);

        username_et.setOnFocusChangeListener((v, hasFocus) -> {
            if(!hasFocus)
                hideSoftKeyboard(v);
        });

        position_et.setOnFocusChangeListener((v, hasFocus) -> {
            if(!hasFocus)
                hideSoftKeyboard(v);
        });

        delete_account.setOnClickListener( v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Delete account")
                    .setMessage("Do you really want to delete your account?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                            removeAccount(session.getUid()))
                    .setNegativeButton(android.R.string.no, null).show();
        });

        recyclerView = findViewById(R.id.modify_printers_rv).findViewById(R.id.my_recycle_view);
        populateRV();

        confirm.setOnClickListener((v) ->{
            String new_username = username_et.getText().toString();
            String new_position = position_et.getText().toString();
            String uid = session.getUid();
            ExecutorService executor = Executors.newFixedThreadPool(4);
            try {
                executor.invokeAll(Arrays.asList(
                        () -> new DbCommunication(DbCommunication.OPERATIONS.PATCH).template("PATCH", FIREBASE_DB_ROOT_URL+"users/"+uid+"/metadata",
                                "{\"username\":\""+new_username+"\"}",null),
                        () -> new DbCommunication(DbCommunication.OPERATIONS.PATCH).template("PATCH", FIREBASE_DB_ROOT_URL+"users/"+uid+"/metadata",
                                "{\"position\":\""+new_position+"\"}",null),
                        () -> new DbCommunication(DbCommunication.OPERATIONS.PATCH).template("PATCH", FIREBASE_DB_ROOT_URL+"user_pos",
                                "{\""+uid+"\":\""+new_position+"\"}",null),
                        () -> new DbCommunication(DbCommunication.OPERATIONS.PATCH).template("PATCH", FIREBASE_DB_ROOT_URL+"user_uid",
                                "{\""+uid+"\":\""+new_username+"\"}",null)
                ));
                executor.shutdown();
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                if(!executor.isTerminated())
                    executor.shutdownNow();
            }

            session.setUsername(new_username);
            session.setPosition(new_position);
            startActivity(new Intent(this, ProfileActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .putExtra("FROM","MODIFY")
                    .putExtra("NEW_USERNAME", new_username)
                    .putExtra("NEW_POSITION", new_position));
        });

        cancel.setOnClickListener((v) -> {
            finish();
        });
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();

        showOptions[0] = false;
        showOptions[1] = false;
        showOptions[2] = false;
        showOptions[3] = false;

        username_et.setText(savedValues.getString("username",""));
        position_et.setText(savedValues.getString("position",""));
        populateRV();
    }

    @Override
    protected void onStop() {
        super.onStop();
        savedValues.edit().putString("username", username_et.getText().toString())
                .putString("position", position_et.getText().toString())
                .apply();
    }

    private void populateRV(){
        String uid = session.getUid();
        ArrayList<PrinterListItem> mData = printersInit(uid, true);
        if(mData.size() > 0) {
            recyclerView.setAdapter(new PrinterAdapter(mData,this,true));
            recyclerView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.GONE);
        }
    }

    private void removeAccount(String uid){
        DbCommunication db = new DbCommunication();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        String username = session.getUsername();
        try {
            Future<JSONObject> future = executor.submit(() ->
                    db.template("GET",FIREBASE_DB_ROOT_URL+"users/"+uid+"/printers",null,null));
            try {
                future.get().keys().forEachRemaining(printer_id -> {
                    if(!printer_id.equals("readed")) {
                        String id = uid + "-" + printer_id;
                        //remove data from materials
                        Arrays.stream(getResources().getStringArray(R.array.materials))
                                .forEach(material -> {
                                    db.remove("materials/" + material + "/" + id);
                                });
                        //remove data from printer_models
                        try {
                            String model = future.get().getJSONObject(printer_id).getString("model");
                            db.remove("printer_models/" + model + "/" + id);
                        } catch (JSONException | ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }
                        db.remove("user_dim/" + id);
                    }
                });
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            db.remove("notify/"+username);
            db.remove("user_pos/"+uid);
            db.remove("user_uid/"+uid);
            db.remove("users/"+uid);
            executor.shutdown();
            executor.awaitTermination(5,TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if(!executor.isTerminated())
                executor.shutdownNow();
        }
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        String email = user.getEmail();
        user.delete();
        Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).delete();
        session.deleteSession();
        new BackgroundNotify(this).stop();
        startActivity(new Intent(this, MainActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }
}
