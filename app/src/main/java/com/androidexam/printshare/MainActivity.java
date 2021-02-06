package com.androidexam.printshare;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.List;
import java.util.Locale;


public class MainActivity extends Activity {

    Database db;

    EditText username_text;
    EditText position_text;
    TextView username_label;
    TextView position_label;
    TextView country_name;
    Button register;
    Button get_location;
    FusedLocationProviderClient fusedLocationProviderClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        username_label = (TextView) findViewById(R.id.username_label);
        username_text = (EditText) findViewById(R.id.username_text);
        position_label = (TextView) findViewById(R.id.position_label);
        country_name = (TextView) findViewById(R.id.country_name);
        position_text = (EditText) findViewById(R.id.position_text);
        register = (Button) findViewById(R.id.register);
        get_location = (Button) findViewById(R.id.get_location);

        //initialize fusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        get_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    //if permission granted
                    fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            Location location = task.getResult();
                           if (location != null) {
                                try {
                                    Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                                    //Address
                                    List<Address> address = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                    position_text.setText(address.get(0).getAddressLine(0));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
                }
            }

        });

        //db part
        db = new Database(FirebaseDatabase.getInstance());

        db.clean();

        register.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                db.register(new User(username_text.getText().toString(), position_text.getText().toString()));
            }
        });

        db.readUserFromDb("second_username");

    }


}