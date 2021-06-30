package com.androidexam.printshare;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.androidexam.printshare.utilities.DbCommunication;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


public class RegisterActivity extends ActivityTemplate{

    //private static String apiKey = "AIzaSyAAOOVTL6XHetI4hpeIAzYuSU3B_JVPajw";

    private SharedPreferences savedValues;
    private EditText username_text;
    private EditText position_text;
    private EditText email_text;
    private EditText password_text;
    private boolean checked[] = new boolean[3];
    private TextView username_label;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);

        savedValues = getSharedPreferences("RegistrationSavedValues", MODE_PRIVATE);

        username_label = findViewById(R.id.username_label);
        username_text =  findViewById(R.id.username_text);
        TextView position_label = findViewById(R.id.position_label);
        position_text =  findViewById(R.id.position_text);
        Button register = findViewById(R.id.register);
        TextView email_label = findViewById(R.id.email_label);
        TextView password_label = findViewById(R.id.password_label);
        email_text =  findViewById(R.id.email_text);
        password_text =  findViewById(R.id.password_text);

        register.setFocusable(true);
        register.setFocusableInTouchMode(true);
        register.setOnClickListener(v -> {

                String email = email_text.getText().toString();
                String password = password_text.getText().toString();
                String username = username_text.getText().toString();
                String place_name = position_text.getText().toString();

                if(checkValues(email,password,username)) {
                    if(isConnected())
                    new DbCommunication(DbCommunication.OPERATIONS.PATCH).newUserRegistration(email, password, username,
                            place_name, this);
                    else
                        inputFailure(this,"Internet connection","There's no internet connection.");
                } else {
                    inputFailure(this, "Some inputs are incorrect.","Some fields are still empty.");
                }
        });

        password_text.setOnFocusChangeListener((v,hasFocus)->{
            if(!hasFocus) {
                String s = ((EditText)v).getText().toString();
                String[] patterns = {".*[\\d].*", ".*[A-Z].*", ".*[a-z].*"};
                boolean length = s.length() >= 6;
                checked[2] = Stream.of(patterns).map(m -> Pattern.compile(m).matcher(s).matches()).reduce((p1, p2) -> p1 && p2).get() && length;
                if(checked[2]){
                    password_label.setTextColor(Color.parseColor("#00ff00"));
                } else {
                    password_label.setTextColor(Color.parseColor("#ff0000"));
                    password_text.setText("");
                    inputFailure(this,
                            "Some inputs are incorrect.",
                            "Your password must have at least: 1 Uppercase, 1 lowercase and 1 digit.");
                }
            }
        });
        email_text.setOnFocusChangeListener((v, hasFocus) -> {
            if(!hasFocus){
                if(!email_text.hasFocus()) {
                    String cleaned_email = ((EditText) v).getText().toString();
                    Pattern email_pattern = Pattern.compile("^[\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$");
                    Matcher m = email_pattern.matcher(cleaned_email);
                    checked[1] = m.matches();
                    if(checked[1]){
                        email_label.setTextColor(Color.parseColor("#00ff00"));
                    } else {
                        email_label.setTextColor(Color.parseColor("#ff0000"));
                        inputFailure(this,
                                "Some inputs are incorrect.",
                                "The format of your email is incorrect.");
                    }
                }
            }
        });

        email_text.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE)
                register.requestFocus();
            return true;
        });

        username_text.setOnFocusChangeListener((v,hasFocus)->{
            String cleaned_username = username_text.getText().toString().trim();
            Pattern username_pattern = Pattern.compile("^[\\S]+");
            Matcher m = username_pattern.matcher(cleaned_username);
            checked[0] = m.matches();
                if (!hasFocus) {
                    String s = ((EditText) v).getText().toString();
                    Handler handler = new Handler();
                    List<String> queries = new ArrayList<>();
                    Collections.addAll(queries, "orderBy=\"metadata/username\"", "equalTo=\"" + s + "\"");
                    Executors.newSingleThreadExecutor().execute(() -> {
                        register.setActivated(false);
                        JSONObject result = new DbCommunication().template("GET", FIREBASE_DB_ROOT_URL + "users", null, queries);
                        boolean check = result.length() == 0;
                        handler.post(() -> {
                            checked[0] = check || checked[0];
                            register.setActivated(true);
                        });
                    });
                }
        });
    }


    @Override
    protected void onPause() {
        Editor editor = savedValues.edit();
        editor.putString("username", username_text.getText().toString());
        editor.putString("email", email_text.getText().toString());
        editor.putString("position", position_text.getText().toString());
        editor.apply();

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        username_text.setText(savedValues.getString("username", ""));
        email_text.setText(savedValues.getString("email", ""));
        position_text.setText(savedValues.getString("position", ""));

    }

    private boolean checkValues(String email, String password, String username){
        if(email.equals("") || password.equals("") || username.equals(""))
            return false;
        if (checked[0]) {
            username_label.setTextColor(Color.parseColor("#00ff00"));
        } else {
            inputFailure(this,
                    "Some inputs are incorrect.",
                    "Username can't contain whitespace.");
            username_label.setTextColor(Color.parseColor("#ff0000"));
            username_text.setText("");
            username_text.setHint("username already in use.");
            inputFailure(this,
                    "Some inputs are incorrect.",
                    "Username already in use.");
        }
        boolean passCheck = true;
        for(boolean bit :checked){
            passCheck = passCheck && bit;
        }
        return passCheck;
    }


}