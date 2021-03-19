package com.androidexam.printshare;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.ArraySet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class SearchActivity extends AppCompatActivity{
    private static final String FIREBASE_DB_ROOT_URL = "https://printshare-77932-default-rtdb.firebaseio.com/";

    private static final String LOG_TAG = SearchActivity.class.getSimpleName();
    private TextView result_view;
    private EditText origin;
    private TextView search_place_label;
    private SharedPreferences prefs;

    private CheckBox[] checkBoxesIds;
    private String model;
    private int height;
    private int length;
    private int width;
    private boolean first_loop;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);

        //to show results
        result_view = findViewById(R.id.results_textview);

        //to order by distance
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        search_place_label = findViewById(R.id.search_place_label);
        origin = findViewById(R.id.origin_editable_text);

        //search by model
        TextView search_printer_label = findViewById(R.id.search_printer_label);
        Spinner search_printer_input = findViewById(R.id.search_printer_model_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.printer_models, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        search_printer_input.setAdapter(adapter);
        search_printer_input.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                model = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //search by materials
        CheckBox material_1 = findViewById(R.id.search_check_material_1);
        CheckBox material_2 = findViewById(R.id.search_check_material_2);
        checkBoxesIds = new CheckBox[]{material_1, material_2};

        //search by dimensions
        TextView height_label = findViewById(R.id.search_height_label);
        TextView length_label = findViewById(R.id.search_length_label);
        TextView width_label = findViewById(R.id.search_width_label);
        EditText height_input = findViewById(R.id.search_height_input);
        EditText length_input = findViewById(R.id.search_length_input);
        EditText width_input = findViewById(R.id.search_width_input);

        height= -1;
        length = -1;
        width = -1;

        height_input.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId == EditorInfo.IME_ACTION_NEXT)
                try {
                    String input = v.getText().toString();
                    if(!input.equals(""))
                        height = Integer.parseInt(input);
                    else
                        height = 0;
                    height_label.setTextColor(Color.rgb(0,255,0));
                    length_input.requestFocus();
                } catch (NumberFormatException e){
                    height_label.setTextColor(Color.rgb(255,0,0));
                }
            return true;
        });

        length_input.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId == EditorInfo.IME_ACTION_NEXT)
                try {
                    String input = v.getText().toString();
                    if(!input.equals(""))
                        length = Integer.parseInt(input);
                    else
                        length = 0;
                    length_label.setTextColor(Color.rgb(0,255,0));
                    width_input.requestFocus();
                } catch (NumberFormatException e){
                    length_label.setTextColor(Color.rgb(255,0,0));
                }
            return true;
        });

        width_input.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId == EditorInfo.IME_ACTION_NEXT)
                try {
                    String input = v.getText().toString();
                    if(!input.equals(""))
                        width = Integer.parseInt(input);
                    else
                        width = 0;
                    width_label.setTextColor(Color.rgb(0,255,0));
                    hideSoftKeyboard(v);
                } catch (NumberFormatException e){
                    width_label.setTextColor(Color.rgb(255,0,0));
                }
            return true;
        });

        Button send = findViewById(R.id.send_request_button);
        send.setOnClickListener(v -> {

            if(isConnected()) {
                searchLogic(result_view);
            }
            else {
                result_view.setText(R.string.no_internet_connection);
            }
        });
    }

    private void searchLogic(TextView result_view){
        ExecutorService executor;
        List<Callable<JSONObject>> tasks = new ArrayList<>();
        Callable<JSONObject> printer_model_task = searchByPrinterModel();
        if(printer_model_task != null)
            tasks.add(printer_model_task);
        tasks.addAll(searchByDimensions());
        Set<String> results = preliminarSearch();

        //discerne tra results vuoto a inizio ricerca e result vuoto per asseza di risultati.
        first_loop = results.isEmpty();
        if(!tasks.isEmpty()) {
            result_view.setText(R.string.loading);
            executor = Executors.newFixedThreadPool(tasks.size());
            try{
                executor.invokeAll(tasks)
                        .forEach(future -> {
                            try {
                                if(results.isEmpty()) {
                                    if(first_loop) {
                                        first_loop = false;
                                        future.get().keys().forEachRemaining(key -> {
                                            if (key.contains("-"))
                                                results.add(key.split("-")[0]);
                                            else
                                                results.add(key);
                                        });
                                    }
                                }
                                else{
                                    ArrayList<String> tmp = new ArrayList<>();
                                    future.get().keys().forEachRemaining(key -> {
                                        if(key.contains("-"))
                                            tmp.add(key.split("-")[0]);
                                        else
                                            tmp.add(key);
                                    });
                                    results.retainAll(tmp);
                                }
                            } catch (ExecutionException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
                executor.shutdown();
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e){
                e.printStackTrace();
            } finally {
                if(!executor.isTerminated())
                    executor.shutdownNow();
            }
        }
        if(results.isEmpty())
            result_view.setText("No users found.");
        else{
            StringBuilder builder = new StringBuilder();
            results.forEach(user -> {
                builder.append(user).append("\n");
            });
            result_view.setText(builder.toString());
        }
    }

    //ricerca per materiali (OR)
    private Set<String> preliminarSearch(){
        List<Callable<JSONObject>> tasks = searchByMaterials();
        Set<String> preliminar_result = new ArraySet<>();
        if(!tasks.isEmpty()) {
            ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
            try{
                executor.invokeAll(tasks).forEach(future -> {
                    try {
                        future.get().keys().forEachRemaining(preliminar_result::add);
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                executor.shutdown();
                executor.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException e){
                e.printStackTrace();
            } finally {
                if(!executor.isTerminated())
                    executor.shutdownNow();
            }
            return preliminar_result;
        } else {
            return  new ArraySet<>();
        }
    }

    private List<Callable<JSONObject>> searchByMaterials(){
        List<Callable<JSONObject>> tasks = new ArrayList<>();
        for(CheckBox check : checkBoxesIds){
            if (check.isChecked()){
                String material = check.getText().toString();
                tasks.add(createNewTask("materials/"+material));
            }
        }
        return tasks;
    }

    //ricerca per altri parametri (AND)
    private Callable<JSONObject> searchByPrinterModel(){
        return this.model!=null ?
                createNewTask("printer_models/"+this.model) :
                null;
    }

    private List<Callable<JSONObject>> searchByDimensions(){
        List<Callable<JSONObject>> tasks = new ArrayList<>();
        if(height > 0)
            tasks.add(createNewTask("H",height));
        if(length > 0)
            tasks.add(createNewTask("L",length));
        if(width > 0)
            tasks.add(createNewTask("W",width));
        return tasks;
    }

    private Callable<JSONObject> createNewTask(String tag, int param){
        return () -> {
            List<String> queries = new ArrayList<>();
            Collections.addAll(queries,"orderBy=\""+tag+"\"","startAt="+param);
            return new DbCommunication().template("GET",FIREBASE_DB_ROOT_URL+"user_dim",
                    null, queries);
        };
    }

    private Callable<JSONObject> createNewTask(String path){
        return () -> new DbCommunication().template("GET",FIREBASE_DB_ROOT_URL+path,
                null, null);
    }

    private void hideSoftKeyboard(View v){
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);

        if (inputManager != null ) {
            inputManager.hideSoftInputFromWindow(v.getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private boolean isConnected(){
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo;
        if(connManager !=null)
            networkInfo = connManager.getActiveNetworkInfo();
        else
            return false;
        return networkInfo != null && networkInfo.isConnected();
    }

    @Override
    protected void onStart() {
        boolean orderResults = prefs.getBoolean("pref_order_result_by_distance", false);
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
