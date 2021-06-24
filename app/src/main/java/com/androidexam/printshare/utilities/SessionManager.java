package com.androidexam.printshare.utilities;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    private final String SHARED_PREFERENCES_TAG = "SESSION";
    private final String SESSION_UID = "SESSION_UID";
    private final String SESSION_USERNAME = "SESSION_USERNAME";
    private final String SESSION_ORIGIN = "SESSION_ORIGIN";

    public SessionManager(Context c){
        this.sharedPreferences = c.getSharedPreferences(SHARED_PREFERENCES_TAG, Context.MODE_PRIVATE);
        this.editor = sharedPreferences.edit();

    }

    public void createSession(String uid, String username, String origin){
        editor.putString(SESSION_UID, uid)
                .putString(SESSION_USERNAME, username)
                .putString(SESSION_ORIGIN, origin)
                .apply();
    }

    public String getUid(){
        return sharedPreferences.getString(SESSION_UID, null);
    }

    public String getUsername(){
        return sharedPreferences.getString(SESSION_USERNAME, null);
    }

    public String getOrigin(){
        return sharedPreferences.getString(SESSION_ORIGIN, null);
    }

    public void deleteSession(){
        editor.putString(SESSION_UID,"")
                .putString(SESSION_ORIGIN,"")
                .putString(SESSION_USERNAME,"").apply();
    }

    public void setUsername(String username){
        editor.putString(SESSION_USERNAME,username).apply();
    }

    public void setPosition(String origin){
        editor.putString(SESSION_ORIGIN,origin).apply();
    }
}
