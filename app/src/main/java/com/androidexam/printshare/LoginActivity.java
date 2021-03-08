package com.androidexam.printshare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

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

        login.setOnClickListener(v -> {
            new DbCommunication().logIn(email_login_text.getText().toString(),password_text.getText().toString());
            startActivity(new Intent(v.getContext(),ProfileActivity.class));
        });
    }

    @Override
    protected void onPause() {
        Editor editor = savedValues.edit();
        editor.putString("email", email_login_text.getText().toString());
        editor.apply();

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        email_login_text.setText(savedValues.getString("email", ""));
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
