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

/**
 * <p>DbCommunication: class which purpose is to interface with Firebase Realtime Database.</p>
 * <p><title><h1>Structure of our Db:</h1></title>
 *  <pre>
 *      <i>root</i>
 *      |
 *      +---<i>users</i>
 *      |   |
 *      |   +---<i>uid</i>
 *      |       |
 *      |       +---<i>metadata<i/>
 *      |           |
 *      |           +---<i>username</i>
 *      |           |
 *      |           +---<i>mail</i>
 *      |           |
 *      |           +---<i>location</i>
 *      |           |
 *      |           +---<i>printers</i>
 *      |               |
 *      |               +---<i>printer_id</i>
 *      |                   |
 *      |                   +---<i>materials</i>
 *      |                   |
 *      |                   +---<i>printer_volume</i>
 *      |                   |
 *      |                   +---<i>heated_bed</i>
 *      |                   |
 *      |                   +---<i>model</i>
 *      |
 *      +---<i>user_pos</i>
 *      |
 *      +---<i>user_model</i>
 *      |
 *      +---<i>user_material</i>
 *      |
 *      +---<i>user_printer_volume</i>
 *      |
 *      +---<i>user_uid</i>
 *  </pre>
 * </p>
 *
 */
public class DbCommunication extends ConnectionTemplate{
    private static final String LOG_TAG = DbCommunication.class.getSimpleName();
    private static final String FIREBASE_DB_ROOT_URL = "https://printshare-77932-default-rtdb.firebaseio.com/";

    public enum OPERATIONS  {PATCH, POST, PUT, READ, READ_PRINTER}
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
                launchAsyncTask("PATCH","user_uid","{\""+uid+"\":\""+username+"\"}");
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
                if(operation != null)
               ((TextView) mLabel.get()).setText("Something wrong..."+operation.toString());
                else
                    ((TextView) mLabel.get()).setText("Something wrong...");
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
        if(operation != null) {
            switch (operation) {
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
                case PUT:
                    break;
                case READ:
                default:
                    try {
                        output = response.getString("readed");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
            }
        }
        return output;
    }

    @Override
    boolean isLimitedRead() {
        return false;
    }
}
