package com.androidexam.printshare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.androidexam.printshare.adapters.MaterialsAdapter;
import com.androidexam.printshare.utilities.DbCommunication;
import com.androidexam.printshare.utilities.MaterialListItem;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONObject;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class AddPrinterActivity extends ActivityTemplate {

    private TextView height_label;
    private EditText printer_tag_input;
    private EditText length_input;
    private EditText height_input;
    private EditText width_input;
    private String model;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.add_printer_activity);

        TextView mText_2 = findViewById(R.id.add_printer_dimension_label);
        TextView length_label = findViewById(R.id.length_label);
        height_label = findViewById(R.id.height_label);
        TextView width_label = findViewById(R.id.width_label);
        TextView printer_tag_label = findViewById(R.id.add_printer_printer_tag_label);
        printer_tag_input = findViewById(R.id.add_printer_printer_tag_input);
        length_input = findViewById(R.id.length_input);
        height_input = findViewById(R.id.height_input);
        width_input = findViewById(R.id.width_input);
        Button confirm = findViewById(R.id.add_printer_confirm_button);
        Button cancel = findViewById(R.id.add_printer_close_button);

        ArrayList<MaterialListItem> mData = materialsInit();
        RecyclerView mRecycleView = findViewById(R.id.my_recycle_view);
        mRecycleView.setAdapter(new MaterialsAdapter(mData));

        //implementazione spinner (tipologie stampanti)
        Spinner model_spinner = findViewById(R.id.add_printer_models_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                                                R.array.printer_models, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        model_spinner.setAdapter(adapter);
        model_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                model = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                model = "";
            }
        });

        confirm.setOnClickListener((v) ->{
            if(check()) {
                String key = printer_tag_input.getText().toString();
                int length = Integer.parseInt(length_input.getText().toString());
                int height = Integer.parseInt(height_input.getText().toString());
                int width = Integer.parseInt(width_input.getText().toString());
                StringBuilder jsonInput = new StringBuilder();
                jsonInput.append("{\"model\":\"").append(model).append("\"");

                jsonInput.append(",\"dimensions\":{ \"L\":").append(length).append(",")
                        .append("\"H\":").append(height).append(",")
                        .append("\"W\":").append(width).append("}");
                jsonInput.append("}");
                String uid = FirebaseAuth.getInstance().getUid();

                materials_checked(uid, key, mData);

                List<Callable<JSONObject>> tasks = new ArrayList<>();
                tasks.add(() -> new DbCommunication(DbCommunication.OPERATIONS.PATCH).template("PATCH", FIREBASE_DB_ROOT_URL+"users/" + uid + "/printers/" + key, jsonInput.toString(), null));
                tasks.add(() -> new DbCommunication(DbCommunication.OPERATIONS.PATCH).template("PATCH", FIREBASE_DB_ROOT_URL+"printer_models/" + model, "{\"" + uid+"-"+key + "\":\"true\"}", null));
                tasks.add(() -> new DbCommunication(DbCommunication.OPERATIONS.PATCH).template("PATCH", FIREBASE_DB_ROOT_URL+"user_dim/" + uid+"-"+key, "{\"L\":" + length + "}", null));
                tasks.add(() -> new DbCommunication(DbCommunication.OPERATIONS.PATCH).template("PATCH", FIREBASE_DB_ROOT_URL+"user_dim/" + uid+"-"+key, "{\"H\":" + height + "}", null));
                tasks.add(() -> new DbCommunication(DbCommunication.OPERATIONS.PATCH).template("PATCH", FIREBASE_DB_ROOT_URL+"user_dim/" + uid+"-"+key, "{\"W\":" + width + "}", null));
                if(isConnected()) {
                    ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
                    try {
                        executor.invokeAll(tasks).forEach(future -> {
                            try {
                                future.get();
                            } catch (ExecutionException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
                        executor.shutdown();
                        executor.awaitTermination(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        if (!executor.isTerminated())
                            executor.shutdownNow();
                    }

                    Intent intent = new Intent(this, ProfileActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            .putExtra("FROM", "REDIRECT");
                    startActivity(intent);
                } else {
                    inputFailure(this,"Internet connection","There's no internet connection.");
                }
            } else {
                Toast.makeText(v.getContext(),"Insert all requested values and a unique printer tag.",Toast.LENGTH_LONG).show();
            }
        });
        cancel.setOnClickListener((v) -> {
            getSharedPreferences("add_printer_saved_values",MODE_PRIVATE).edit().clear().apply();
            finish();
        });
        super.onCreate(savedInstanceState);
    }

    private boolean check(){
        if(length_input.getText().toString().equals(""))
            return false;
        if(height_input.getText().toString().equals(""))
            return false;
        if(width_input.getText().toString().equals(""))
            return false;
        if(height_label.getText().toString().equals(""))
            return false;
        if(printer_tag_input.getText().toString().equals("")){
            return false;
        } else {
            String key = printer_tag_input.getText().toString();
            String uid = FirebaseAuth.getInstance().getUid();
            boolean result = true;

            Callable<Boolean> task = () -> {
                JSONObject obj =  new DbCommunication(DbCommunication.OPERATIONS.READ).template("GET",FIREBASE_DB_ROOT_URL+"users/"+uid+"/printers/"+key,null,null);

                if(obj.has("readed"))
                    return obj.isNull("readed");
                return false;
            };
            if(isConnected()) {
                ExecutorService executor = Executors.newFixedThreadPool(1);
                Future<Boolean> future = executor.submit(task);
                try {
                    result = future.get();
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
                executor.shutdown();
                try {
                    executor.awaitTermination(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    if (!executor.isTerminated())
                        executor.shutdownNow();
                }
            }
            return result;
        }
    }

    private void materials_checked(String uid, String printer, List<MaterialListItem> mData){
        ExecutorService executor;
        List<Callable<JSONObject>> tasks = new ArrayList<>();
        StringBuilder jsonInput = new StringBuilder();
        mData
                .forEach(material -> {
                    if(material.isChecked()){
                        //user has this material.
                        tasks.add(() -> new DbCommunication(DbCommunication.OPERATIONS.PATCH)
                                .template("PATCH",
                                        FIREBASE_DB_ROOT_URL+"materials/"+material.getMaterial(),
                                        "{\""+uid+"-"+printer+"\":\"true\"}",
                                        null));
                        if (jsonInput.length()==0)
                                jsonInput.append("{");
                        jsonInput.append("\"").append(material).append("\":\"true\",");
                    }
                });
        if (jsonInput.length() > 0) {
            jsonInput.deleteCharAt(jsonInput.lastIndexOf(","));
            jsonInput.append("}");
            tasks.add(() -> new DbCommunication(DbCommunication.OPERATIONS.PATCH)
                    .template("PATCH",
                            FIREBASE_DB_ROOT_URL+"users/"+uid+"/printers/"+printer+"/materials",
                            jsonInput.toString(),
                            null));
        }
        executor = Executors.newFixedThreadPool(tasks.size());
        try {
            executor.invokeAll(tasks);
            executor.shutdown();
            executor.awaitTermination(5,TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if(!executor.isTerminated())
                executor.shutdownNow();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //TODO save values.
    }

    @Override
    protected void onStart() {
        super.onStart();

        //TODO restore values.

        showOptions[0] = false;
        showOptions[1] = false;
        showOptions[2] = false;
        showOptions[3] = false;
    }
}
