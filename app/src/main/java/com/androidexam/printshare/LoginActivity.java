package com.androidexam.printshare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    private SharedPreferences savedValues;

    EditText email_login_text;
    EditText password_text;
    TextView email_login_label;
    TextView password_label;
    Button login;
    Button forgot_password;
    TextView email_incorrect;
    TextView email_not_found;
    TextView email_sent;

    private boolean checked;
    FirebaseAuth firebaseAuth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        savedValues = getSharedPreferences("LoginSavedValues", MODE_PRIVATE);
        setContentView(R.layout.login_activity);

        email_login_label = findViewById(R.id.email_text);
        password_label = findViewById(R.id.password_text);
        email_login_text = findViewById(R.id.email_login);
        password_text = findViewById(R.id.password_login_text);
        login = findViewById(R.id.login_button);
        forgot_password = findViewById((R.id.forgot_password));
        email_incorrect = findViewById(R.id.email_incorrect);
        email_not_found = findViewById(R.id.email_not_found);
        email_sent = findViewById(R.id.email_sent);
        email_not_found.setVisibility(View.INVISIBLE);
        email_incorrect.setVisibility(View.INVISIBLE);
        email_sent.setVisibility(View.INVISIBLE);

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade);
        Animation animation_slow = AnimationUtils.loadAnimation(this, R.anim.fade_slow);

        firebaseAuth=FirebaseAuth.getInstance();


        login.setOnClickListener(v -> {
            new DbCommunication().logIn(email_login_text.getText().toString(), password_text.getText().toString());

            startActivity(new Intent(v.getContext(), ProfileActivity.class));
        });

        forgot_password.setOnClickListener(v -> {
            String cleaned_email = (email_login_text).getText().toString();
            Pattern email_pattern = Pattern.compile("^[\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$");
            Matcher m = email_pattern.matcher(cleaned_email);
            checked = m.matches();
            if (checked) {
                firebaseAuth.sendPasswordResetEmail(email_login_text.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            email_sent.setVisibility(View.VISIBLE);
                            email_sent.startAnimation(animation_slow);
                            email_sent.setVisibility(View.INVISIBLE);
                        } else {
                            email_not_found.setVisibility(View.VISIBLE);
                            email_not_found.startAnimation(animation_slow);
                            email_not_found.setVisibility(View.INVISIBLE);
                        }
                    }
                });
            } else {
                email_incorrect.setVisibility(View.VISIBLE);
                email_incorrect.startAnimation(animation_slow);
                email_incorrect.setVisibility(View.INVISIBLE);
            }
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
