package com.androidexam.printshare;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;



public class MainActivity extends Activity {

    Database db;

    EditText username_text;
    EditText position_text;
    TextView username_label;
    TextView position_label;
    Button register;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        username_label = (TextView) findViewById(R.id.username_label);
        username_text = (EditText) findViewById(R.id.username_text);
        position_label = (TextView) findViewById(R.id.position_label);
        position_text = (EditText) findViewById(R.id.position_text);
        register = (Button) findViewById(R.id.register);

        db = new Database(FirebaseDatabase.getInstance());

        db.clean();
        db.register(new User("my_username", "Genova"));
        db.register(new User("second_username", "Palermo"));
        db.register(new User("third_username", "Innsbruck"));
        db.register(new User("fourth_username", "Linz"));
        db.readUserFromDb("second_username");

    }
}