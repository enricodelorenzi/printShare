package com.androidexam.printshare;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;


public class LoginActivity extends Activity {

    EditText username_text;
    EditText password_text;
    TextView username_label;
    TextView password_label;
    Button login;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);
        username_label = findViewById(R.id.username_login_label);
        password_label = findViewById(R.id.password_login_label);
        username_text = findViewById(R.id.username_login);
        password_text = findViewById(R.id.password_login_text);
        login = findViewById(R.id.login_button);

        login.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO verificare l'esistenza dell'utente (username) e la correttezza della password.
            }
        });
    }
}
