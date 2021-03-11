package com.androidexam.printshare;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class SearchActivity extends AppCompatActivity{

    private static final String LOG_TAG = SearchActivity.class.getSimpleName();
    private ConnectivityManager connManager;
    private TextView result_view;
    private EditText origin;
    private EditText search_user_input;
    private TextView search_printer_label;
    private TextView search_user_label;
    private TextView search_temp_label;
    private TextView search_place_label;
    private EditText search_printer_input;
    private EditText search_temp_input;
    private Button send;
    private SharedPreferences prefs;
    private boolean orderResults;

    //SHA1:   96:E0:B2:6A:90:A2:66:09:AE:62:D1:1D:5E:FE:04:F3:62:55:40:F2
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);
        result_view = findViewById(R.id.results_textview);
        origin = findViewById(R.id.origin_editable_text);
        send = findViewById(R.id.send_request_button);
        search_place_label = findViewById(R.id.search_place_label);
        search_printer_label = findViewById(R.id.search_printer_label);
        search_temp_label = findViewById(R.id.search_temp);
        search_temp_input = findViewById(R.id.search_temp_edit);
        search_printer_input = findViewById(R.id.search_printer_input);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        send.setOnClickListener(v -> {
            String origin_input = origin.getText().toString();
            int origin_length = origin_input.length();
            String printer_input = search_printer_input.getText().toString();
            int printer_length = printer_input.length();
            String temp_input = search_temp_input.getText().toString();

            //hide soft-keyword
            InputMethodManager inputManager = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);

            if (inputManager != null ) {
                inputManager.hideSoftInputFromWindow(v.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            }

            connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = null;
            if(connManager !=null)
                networkInfo = connManager.getActiveNetworkInfo();

            if(networkInfo != null && networkInfo.isConnected()) {
                //TODO eleborare la richiesta.

                new DbCommunication(null, result_view).launchAsyncTask("READ","materials/material_1",null);

                //startActivity(new Intent(this,SearchedProfileActivity.class)
                //                    .putExtra("SEARCHED_USER",search_user_input.getText().toString()));


                result_view.setText(R.string.loading);
            }
            else if (!networkInfo.isConnected())
                result_view.setText(R.string.no_internet_connection);
        });
    }

    @Override
    protected void onStart() {
        orderResults = prefs.getBoolean("pref_order_result_by_distance",false);
        boolean wasVisible = origin.getVisibility() == View.VISIBLE;
        if(!orderResults && wasVisible) {
            origin.setVisibility(View.GONE);
            search_place_label.setVisibility(View.GONE);
        } else if(orderResults && !wasVisible){
            origin.setVisibility(View.VISIBLE);
            search_place_label.setVisibility(View.VISIBLE);
        }
        super.onStart();
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
