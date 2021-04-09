package com.androidexam.printshare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


public class RegisterActivity extends AppCompatActivity{

    private static String apiKey = "AIzaSyAAOOVTL6XHetI4hpeIAzYuSU3B_JVPajw";
    private static final String FIREBASE_DB_ROOT_URL = "https://printshare-77932-default-rtdb.firebaseio.com/";

    private SharedPreferences savedValues;
    private EditText username_text;
    private EditText position_text;
    private EditText email_text;
    private EditText password_text;
    private boolean checked[] = new boolean[3];

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);

        savedValues = getSharedPreferences("RegistrationSavedValues", MODE_PRIVATE);

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade);

        TextView username_label = findViewById(R.id.username_text);
        username_text =  findViewById(R.id.username_text);
        TextView position_label = findViewById(R.id.position_text);
        position_text =  findViewById(R.id.position_text);
        Button register = findViewById(R.id.register);
        TextView email_label = findViewById(R.id.email_text);
        TextView password_label = findViewById(R.id.password_text);
        TextView confirm_label = findViewById(R.id.confirm_password_text);
        email_text =  findViewById(R.id.email_text);
        password_text =  findViewById(R.id.password_text);
        EditText confirm_password_text = findViewById(R.id.confirm_password_text);

        register.setOnClickListener(v -> {

                String email = email_text.getText().toString();
                String password = password_text.getText().toString();
                String username = username_text.getText().toString();
                String place_name = position_text.getText().toString();

                if(checkValues(email,password,username)) {
                    placeValidation(place_name);
                    v.startAnimation(animation);
                    startActivity(new Intent(this, ProfileActivity.class).putExtra("USERNAME", username)
                            .putExtra("POSITION", place_name));
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
                    }
                }
            }
        });
        username_text.setOnFocusChangeListener((v,hasFocus)->{
            if(!hasFocus){
                String s = ((EditText)v).getText().toString();
                Handler handler = new Handler();
                List<String> queries = new ArrayList<>();
                Collections.addAll(queries, "orderBy=\"metadata/username\"","equalTo=\""+s+"\"");
                Executors.newSingleThreadExecutor().execute(()->{
                    //TODO verificare se impedisce il click.
                    register.setActivated(false);
                    JSONObject result = new DbCommunication().template("READ",FIREBASE_DB_ROOT_URL+"users",null, queries);
                    boolean check = result.length() == 0;
                    handler.post(()->{
                        checked[0] = check;
                        if(checked[0]){
                            username_label.setTextColor(Color.parseColor("#00ff00"));
                        } else {
                            username_label.setTextColor(Color.parseColor("#ff0000"));
                        }
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

    //TODO remove NetworkUtils
    private void placeValidation(final String place){
        Executor executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler();
        executor.execute(()->{
            String result = NetworkUtils.findPlace(place);
            handler.post(()->{
               if(result != null) {
                   try {
                       writeNewUser(new JSONObject(result));
                   } catch (JSONException e) {
                       e.printStackTrace();
                   }
               }
            });
        });
    }

    private void writeNewUser(JSONObject place){
        try {
            if(place.getString("status").equals("OK")) {
                JSONObject candidate = place.getJSONArray("candidates").getJSONObject(0);
                new DbCommunication(DbCommunication.OPERATIONS.PATCH).newUserRegistration(email_text.getText().toString(), password_text.getText().toString(),
                        username_text.getText().toString(),candidate.getString("name"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean checkValues(String email, String password, String username){
        if(email.equals("") || password.equals("") || username.equals(""))
            return false;
        boolean passCheck = true;
        for(boolean bit :checked){
            passCheck = passCheck && bit;
        }
        return passCheck;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_home :
                startActivity(new Intent(this,MainActivity.class));
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this,SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}