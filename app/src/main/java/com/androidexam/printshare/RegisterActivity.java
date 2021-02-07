package com.androidexam.printshare;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.database.FirebaseDatabase;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RegisterActivity extends Activity {


    private SharedPreferences savedValues;
    private EditText username_text;
    private EditText position_text;
    private EditText email_text;
    private EditText password_text;
    private EditText confirm_password_text;
    private TextView email_label;
    private TextView password_label;
    private TextView confirm_label;
    private TextView username_label;
    private TextView position_label;
    private Button register;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);

        savedValues = getSharedPreferences("RegistrationSavedValues", MODE_PRIVATE);

        username_label =  findViewById(R.id.username_label);
        username_text =  findViewById(R.id.username_text);
        position_label =  findViewById(R.id.position_label);
        position_text =  findViewById(R.id.position_text);
        register =  findViewById(R.id.register);
        email_label =  findViewById(R.id.email_label);
        password_label =  findViewById(R.id.password_label);
        confirm_label =  findViewById(R.id.confirm_label);
        email_text =  findViewById(R.id.email_text);
        password_text =  findViewById(R.id.password_text);
        confirm_password_text = findViewById(R.id.confirm_password_text);

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Database(FirebaseDatabase.getInstance()).createAccount(email_text.getText().toString(), password_text.getText().toString(),
                            username_text.getText().toString(), position_text.getText().toString(),
                            v.getContext());
            }
        });
    }


    @Override
    protected void onPause() {
        Editor editor = savedValues.edit();
        editor.putString("username", username_text.getText().toString());
        editor.putString("email", email_text.getText().toString());
        editor.putString("position", position_text.getText().toString());
        editor.commit();

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        username_text.setText(savedValues.getString("username", ""));
        email_text.setText(savedValues.getString("email", ""));
        position_text.setText(savedValues.getString("position", ""));
    }

    private boolean checkValues(String email, String password, String username, String position){
        //TODO check sulla validità della posizione, sulla validità della password.
        Pattern email_pattern = Pattern.compile("^[\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$");
        Matcher m = email_pattern.matcher(email);
        return password != null && username != null && position != null && m.matches();
    }

}