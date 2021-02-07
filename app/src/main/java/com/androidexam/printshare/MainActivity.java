package com.androidexam.printshare;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends Activity implements OnClickListener {

    private Button registration;
    private Button login;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registration = findViewById(R.id.home_registration);
        login = findViewById(R.id.home_login);

        registration.setOnClickListener(this);
        login.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()){
            case R.id.home_registration : {
                intent = new Intent(v.getContext() , RegisterActivity.class);
            }; break;
            case R.id.home_login : {
                intent = new Intent(v.getContext(), LoginActivity.class);
            }; break;
            default:
                throw new IllegalStateException("Unexpected value: " + v.getId());
        }
        startActivity(intent);
    }
}
