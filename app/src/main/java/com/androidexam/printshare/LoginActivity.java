package com.androidexam.printshare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.database.FirebaseDatabase;


public class LoginActivity extends Activity {

    private SharedPreferences savedValues;

    EditText email_login_text;
    EditText password_text;
    TextView email_login_label;
    TextView password_label;
    Button login;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        savedValues = getSharedPreferences("LoginSavedValues", MODE_PRIVATE);

        setContentView(R.layout.login_activity);
        email_login_label = findViewById(R.id.email_login_label);
        password_label = findViewById(R.id.password_login_label);
        email_login_text = findViewById(R.id.email_login);
        password_text = findViewById(R.id.password_login_text);
        login = findViewById(R.id.login_button);

        login.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                new Database(FirebaseDatabase.getInstance()).logIn(email_login_text.getText().toString(),
                                                                    password_text.getText().toString(), v.getContext());
            }
        });
    }

    @Override
    protected void onPause() {
        Editor editor = savedValues.edit();
        editor.putString("email", email_login_text.getText().toString());
        editor.commit();

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        email_login_text.setText(savedValues.getString("email", ""));
    }
}
