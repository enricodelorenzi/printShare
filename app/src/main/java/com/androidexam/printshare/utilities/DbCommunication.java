package com.androidexam.printshare.utilities;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidexam.printshare.ProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import org.json.JSONException;
import org.json.JSONObject;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    private Context mContext;

    public DbCommunication(){}

    public DbCommunication(Context c){ this.mContext = c;}

    public DbCommunication(OPERATIONS op){
        this.operation = op;
    }

    public DbCommunication(OPERATIONS op, View view1){
        this.operation = op;
        mLabel = new WeakReference<>(view1);
    }

    public void newUserRegistration(String email, String password, String username, String place_name, Context mContext){
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = mAuth.getCurrentUser().getUid();
                String relative_path = "users/"+uid+"/metadata";
///////////////create session/////////////////////
                new SessionManager(mContext).createSession(uid, username, place_name);
                new BackgroundNotify(mContext).start();
///////////////Save data into db//////////////////
                ExecutorService executor = Executors.newFixedThreadPool(3);
                try {
                    executor.invokeAll(Arrays.asList(() -> template("PATCH",FIREBASE_DB_ROOT_URL+relative_path,"{\"email\":\""+email+"\"," +
                            "\"position\":\""+place_name+"\"," +
                            "\"username\":\""+username+"\"}", null),
                            ()->template("PATCH",FIREBASE_DB_ROOT_URL+"user_pos","{\""+uid+"\":\""+place_name+"\"}",null),
                            ()->template("PATCH",FIREBASE_DB_ROOT_URL+"user_uid","{\""+uid+"\":\""+username+"\"}", null)));
                    executor.shutdown();
                    executor.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    if(!executor.isTerminated())
                        executor.shutdownNow();
                }
                Intent intent = new Intent(mContext, ProfileActivity.class).putExtra("USERNAME", username)
                        .putExtra("POSITION", place_name)
                        .putExtra("FROM","REG")
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                mContext.startActivity(intent);
            } else {
                if(task.getException() instanceof FirebaseAuthUserCollisionException){
                    Toast.makeText(mContext, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
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
                        mContext.startActivity(new Intent(mContext, ProfileActivity.class)
                                .putExtra("FROM","LOGIN")
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        Log.d("Sign in ", "signInWithEmail:success");
                    } else {
                        new AlertDialog.Builder(mContext)
                                .setTitle("Login failed.")
                                .setMessage("Password or email is incorrect.")
                                .setIcon(android.R.drawable.ic_dialog_info)
                                .setNegativeButton("Ok", null).show();
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

    public String usernameToUid(String username){
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

    public void addNotification(String from, String to_user, String type, boolean flag, String param){
        ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            executor.invokeAll(Collections.singletonList(
                    () -> template("PATCH", FIREBASE_DB_ROOT_URL + "notify/"+ to_user+"/"+from, "{\"type\":\""+type+"\"" +
                            ",\"flag\":\""+flag+"\"" +
                            ",\"content\":\""+param+"\"}", null)));
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if(!executor.isTerminated())
                executor.shutdownNow();
        }
    }

    public void remove(String path) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            executor.submit(() -> template("DELETE", FIREBASE_DB_ROOT_URL + path, null, null));
            executor.shutdown();
            executor.awaitTermination(5,TimeUnit.SECONDS);
        } catch (InterruptedException e){
            e.printStackTrace();
        } finally {
            if(!executor.isTerminated())
                executor.shutdownNow();
        }
    }
}
