package com.androidexam.printshare;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

public class DbCommunication extends ConnectionTemplate{
    private static final String LOG_TAG = DbCommunication.class.getSimpleName();
    private static final String FIREBASE_DB_ROOT_URL = "https://printshare-77932-default-rtdb.firebaseio.com/";

    public enum OPERATIONS  {PATCH, POST, READ, READ_POSITION, READ_PRINTER, READ_TEMP, READ_USERNAME, REGISTRATION, NULL}
    private WeakReference<View> mLabel;
    private OPERATIONS operation;
    private int temp;
    private Handler handler;

    {
        String thread_name = Thread.currentThread().getName();
        if(thread_name.equals("main"))
            handler = new Handler();
    }

    DbCommunication(){}

    DbCommunication(OPERATIONS op){
        this.operation = op;
    }

    DbCommunication(OPERATIONS op, View view1){
        this.operation = op;
        mLabel = new WeakReference<>(view1);
    }

    public void launchAsyncTask(String operation, String path, String data,String... inputs_queries){
        List<String> queries = new ArrayList<>();
        Collections.addAll(queries, inputs_queries);
        if(this.operation == OPERATIONS.READ_TEMP) {
            //queries{orderBy=\"[order_key]\",[...],equalTo=[temp]
            temp = Integer.parseInt(queries.get(queries.size() - 1).split("=")[1]);
        }
        Executors.newFixedThreadPool(1).execute(()->{
            JSONObject response;
                if(queries.size() == 0) {
                    response = template(operation,FIREBASE_DB_ROOT_URL + path,data,null);
                } else {
                    response = template(operation, FIREBASE_DB_ROOT_URL + path, data, queries);
                }
                String output = post_exec(response);
                handler.post(()->{
                    if(showResult())
                        update(output);
                });
        });
    }

    private String retrieveUserByTemp(int temp, JSONObject obj) {
        Set<String> s = new HashSet<>();
        StringBuilder result = new StringBuilder();
        obj.keys().forEachRemaining(cur_key->{
            try {
                if (temp <= obj.getJSONObject(cur_key).getInt("max")) {
                    obj.getJSONObject(cur_key).getJSONObject("materials").keys().forEachRemaining(material ->
                            template("READ",FIREBASE_DB_ROOT_URL+"materials/"+material,null,null).keys().forEachRemaining(s::add));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
        s.forEach(string -> result.append(string).append("\n"));
        return result.toString();
    }

    private String JSONToString(JSONObject obj){
        if(obj != null) {
            StringBuilder s = new StringBuilder();
            obj.keys().forEachRemaining(cur_key -> {
                try {
                    s.append(obj.getString(cur_key)).append("\n");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
            return s.toString();
        }
        return null;
    }

    //TODO adapt to new ConnectionTemplate
    public void newUserRegistration(String email, String password, String username, String place_name){
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = mAuth.getCurrentUser().getUid();
                String relative_path = "users/"+uid+"/metadata";

                launchAsyncTask("PATCH",relative_path,"{\"email\":\""+email+"\"," +
                        "\"position\":\""+place_name+"\"," +
                        "\"username\":\""+username+"\"}");
                launchAsyncTask("PATCH","user_pos","{\""+uid+"\":\""+place_name+"\"}");
            } else {
                if(task.getException() instanceof FirebaseAuthWeakPasswordException) {
                    Log.e(LOG_TAG, "Password non soddisfacente i criteri.");
                    task.getException().printStackTrace();
                }
            }
        });
    }

    public void logIn (String email, String password){
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("Sign in ", "signInWithEmail:success");
                    } else {
                        Log.d("Something wrong ","signInWithEmail:failure");
                    }
                });
    }

    @Override
    boolean showResult() {
        return mLabel != null;
    }

    @Override
    void update(String output) {
        if(mLabel.get() instanceof TextView){
            if(output != null)
                ((TextView) mLabel.get()).setText(output);
            else
               ((TextView) mLabel.get()).setText("Something wrong..."+operation.toString());
        }
        else if (mLabel.get() instanceof ImageView){
            byte[] decodedString = Base64.decode(output, Base64.DEFAULT);
            Bitmap myBitMap = BitmapFactory.decodeByteArray(decodedString,0,decodedString.length);
            ((ImageView) mLabel.get()).setImageBitmap(myBitMap);
        }
    }

    @Override
    String post_exec(JSONObject response) {
        String output = null;
        switch (operation){
            case READ_TEMP:
                output = retrieveUserByTemp(temp, response);
                break;
            case READ_POSITION:
                try {
                    output =  response.getJSONObject(response.keys().next()).getJSONObject("metadata").getString("position");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case READ_USERNAME:
                try {
                    output =  response.getJSONObject(response.keys().next()).getJSONObject("metadata").getString("username");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case READ_PRINTER:
                StringBuilder s = new StringBuilder();
                try {
                    response.getJSONObject(response.keys().next()).keys().forEachRemaining(cur_key -> s.append(cur_key).append("\n"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                output = s.toString();
                break;
            case PATCH:
            case POST:
                break;
            default:
                try {
                    output = response.getString("readed");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
        }
        return output;
    }
}
