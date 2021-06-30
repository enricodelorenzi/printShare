package com.androidexam.printshare;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.androidexam.printshare.adapters.MaterialsAdapter;
import com.androidexam.printshare.adapters.PrinterAdapter;
import com.androidexam.printshare.utilities.DbCommunication;
import com.androidexam.printshare.utilities.MaterialListItem;
import com.androidexam.printshare.utilities.PrinterListItem;
import com.google.gson.annotations.JsonAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class SearchActivity extends ActivityTemplate{
    private static final String FIREBASE_DB_ROOT_URL = "https://printshare-77932-default-rtdb.firebaseio.com/";
    private static final String TAG = SearchActivity.class.getName();
    private static final int PICK_GCODE_FILE = 1;
    private static final int LOAD_SUCCESS = 1;

    private static final String LOG_TAG = SearchActivity.class.getSimpleName();
    private RecyclerView result_view;
    private RecyclerView material_recyclerView;
    private TextView no_result_view;
    private String model;
    private Button send;
    private int height;
    private int length;
    private int width;
    private boolean first_loop;

    private EditText height_input;
    private EditText length_input;
    private EditText width_input;

    private ArrayList<MaterialListItem> mData;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        showOptions[1] = false;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);
        send = findViewById(R.id.send_request_button);

        //to show results
        result_view = findViewById(R.id.results_rv).findViewById(R.id.my_recycle_view);
        no_result_view = findViewById(R.id.no_results);

        ArrayList<PrinterListItem> no_data = new ArrayList<>();
        no_data.add(new PrinterListItem("Init","Result will be shown here.", false));
        result_view.setAdapter(new PrinterAdapter(no_data, this, null));

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
                String selected_model = parent.getItemAtPosition(position).toString();
                model = selected_model.equals("Select model") ? null : selected_model;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                model = null;
            }
        });

        //search by materials
        material_recyclerView = findViewById(R.id.search_materials_rv).findViewById(R.id.my_recycle_view);
        mData = materialsInit();
        material_recyclerView.setAdapter(new MaterialsAdapter(mData));

        //search by dimensions
        TextView height_label = findViewById(R.id.height_label);
        TextView length_label = findViewById(R.id.length_label);
        TextView width_label = findViewById(R.id.width_label);
        height_input = findViewById(R.id.height_input);
        length_input = findViewById(R.id.length_input);
        width_input = findViewById(R.id.width_input);

        height= -1;
        length = -1;
        width = -1;

        height_input.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId == EditorInfo.IME_ACTION_NEXT)
                responseToInput(v,length_input,height_label,false,true);
            return true;
        });

        length_input.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId == EditorInfo.IME_ACTION_NEXT)
                responseToInput(v,width_input,length_label,false,true);
            return true;
        });

        width_input.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE)
                responseToInput(v,send,width_label,true,true);
            return true;
        });

        send.setOnClickListener(v -> {

            if(isConnected()) {
                searchLogic();
            }
            else {

                no_result_view.setText(R.string.no_internet_connection);
            }
        });

        setParentLayoutOnFocusHideKeyboard(findViewById(R.id.search_main_layout));
    }

    private void responseToInput(TextView current_view, View next_view, TextView label, boolean hideKeyboard, boolean nextFocus){
        try {
            int value;
            String input = current_view.getText().toString();
            if(!input.equals(""))
                value = Integer.parseInt(input);
            else
                value = 0;

            switch (label.getText().toString()){
                case "H":height = value; break;
                case "L":length = value; break;
                case "W":width = value; break;
                default: break;
            }

            label.setTextColor(Color.rgb(0,255,0));
            if(hideKeyboard)
                hideSoftKeyboard(current_view);
            if(nextFocus)
                next_view.requestFocus();
        } catch (NumberFormatException e){
            label.setTextColor(Color.rgb(255,0,0));
        }
    }

    //////////////////////SEARCH/////////////////////
    private void searchLogic(){
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
            executor = Executors.newFixedThreadPool(tasks.size());
            try{
                executor.invokeAll(tasks)
                        .forEach(future -> {
                            try {
                                if(results.isEmpty()) {
                                    if(first_loop) {
                                        first_loop = false;
                                        future.get().keys().forEachRemaining(key ->{
                                            if(!key.equals("readed"))
                                                results.add(key);
                                        });
                                    }
                                }
                                else{
                                    ArrayList<String> tmp = new ArrayList<>();
                                    future.get().keys().forEachRemaining(tmp::add);
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
        if(results.isEmpty()) {
            ArrayList<PrinterListItem> no_data = new ArrayList<>();
            no_data.add(new PrinterListItem("No data","No users found", false));
            result_view.setAdapter(new PrinterAdapter(no_data, this, null));
        }
        else{
            ArrayList<PrinterListItem> mData = new ArrayList<>();
            Set<String> uids = uniqueUid(results);
            uids.forEach(uid -> {
                String username = uidToUsername(uid);
                mData.add(new PrinterListItem(uid, username,false));
            });
            result_view.setAdapter(new PrinterAdapter(mData, this, new ProfileActivity()));
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
        }
        return preliminar_result;
    }

    private List<Callable<JSONObject>> searchByMaterials(){
        List<Callable<JSONObject>> tasks = new ArrayList<>();
        mData
                .forEach(material -> {
                    if(material.isChecked()){
                        tasks.add(createNewTask("materials/"+material.getMaterial()));
                    }
                });
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
        else if(!height_input.getText().toString().isEmpty())
            tasks.add(createNewTask("H",Integer.parseInt(height_input.getText().toString())));
        if(length > 0)
            tasks.add(createNewTask("L",length));
        else if(!length_input.getText().toString().isEmpty())
            tasks.add(createNewTask("H",Integer.parseInt(length_input.getText().toString())));
        if(width > 0)
            tasks.add(createNewTask("W",width));
        else if(!width_input.getText().toString().isEmpty())
            tasks.add(createNewTask("H",Integer.parseInt(width_input.getText().toString())));
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


    private Set<String> uniqueUid(Set<String> uid_printer){
        Set<String> result = new ArraySet<>();
        uid_printer.stream()
                .map(s-> s.split("-")[0])
                .forEach(result::add);
        return result;
    }

    private String uidToUsername(String uid){
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Callable<JSONObject> task = createNewTask("user_uid/"+uid);
        String username = null;
        try{
            Future<JSONObject> future = executor.submit(task);
                try {
                    username = future.get().getString("readed");
                } catch (JSONException | InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            executor.shutdown();
            executor.awaitTermination(5,TimeUnit.SECONDS);
        } catch (InterruptedException e){
            e.printStackTrace();
        } finally {
            if(!executor.isTerminated())
                executor.shutdownNow();
        }
        return username;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        showOptions[0] = true;
        showOptions[1] = false;
        showOptions[3] = true;
        super.onResume();
    }
}
