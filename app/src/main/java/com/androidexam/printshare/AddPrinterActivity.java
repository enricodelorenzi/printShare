package com.androidexam.printshare;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class AddPrinterActivity extends AppCompatActivity {
    private static final String FIREBASE_DB_ROOT_URL = "https://printshare-77932-default-rtdb.firebaseio.com/";

    private TextView height_label;
    private EditText printer_tag_input;
    private EditText length_input;
    private EditText height_input;
    private EditText width_input;
    private String model;

    private SharedPreferences savedValues;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.add_printer_activity);
        savedValues = getSharedPreferences("add_printer_saved_values", MODE_PRIVATE);

        TextView mText_2 = findViewById(R.id.add_printer_dimension_label);
        TextView length_label = findViewById(R.id.add_printer_length_label);
        height_label = findViewById(R.id.add_printer_height_label);
        TextView width_label = findViewById(R.id.add_printer_width_label);
        TextView printer_tag_label = findViewById(R.id.add_printer_printer_tag_label);
        printer_tag_input = findViewById(R.id.add_printer_printer_tag_input);
        length_input = findViewById(R.id.add_printer_length_input);
        height_input = findViewById(R.id.add_printer_height_input);
        width_input = findViewById(R.id.add_printer_width_input);
        Button confirm = findViewById(R.id.add_printer_confirm_button);
        Button cancel = findViewById(R.id.add_printer_close_button);

        //inserimento RecycleView
        getFragmentManager().beginTransaction().replace(R.id.fragment_to_replace, new MaterialsFragment()).commit();

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
                //materials_checked(uid, key);      <--problema con la recycleview (ogni checkbox è collegata a un'altra)
                new DbCommunication(DbCommunication.OPERATIONS.PATCH).launchAsyncTask("PATCH", "users/" + uid + "/printers/" + key, jsonInput.toString());
                new DbCommunication(DbCommunication.OPERATIONS.PATCH).launchAsyncTask("PATCH", "printer_models/" + model, "{\"" + uid + "\":\"true\"}");
                new DbCommunication(DbCommunication.OPERATIONS.PATCH).launchAsyncTask("PATCH", "user_dim/" + uid+"-"+key, "{\"L\":" + length + "}");
                new DbCommunication(DbCommunication.OPERATIONS.PATCH).launchAsyncTask("PATCH", "user_dim/" + uid+"-"+key, "{\"H\":" + height + "}");
                new DbCommunication(DbCommunication.OPERATIONS.PATCH).launchAsyncTask("PATCH", "user_dim/" + uid+"-"+key, "{\"W\":" + width + "}");
                finish();
            } else {
                Toast.makeText(v.getContext(),"Insert all requested values and a unique printer tag.",Toast.LENGTH_LONG).show();
            }
        });

        cancel.setOnClickListener((v) -> {
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
                if(!executor.isTerminated())
                    executor.shutdownNow();
            }
            return result;
        }
    }

    //TODO Aggiornamento db in relazione ai materiali selezionati per la nuova stampante. (Da implementare)
    private void materials_checked(String uid, String printer){
        ExecutorService executor;
        List<Callable<JSONObject>> tasks = new ArrayList<>();
        StringBuilder jsonInput = new StringBuilder();
        Arrays.asList(getResources().getStringArray(R.array.materials))
                .forEach(material -> {
                    if(savedValues.getBoolean(material,false)){
                        //user has this material.
                        tasks.add(() -> new DbCommunication(DbCommunication.OPERATIONS.PATCH)
                                .template("PATCH",
                                        FIREBASE_DB_ROOT_URL+"materials/"+material,
                                        "{\""+uid+"\":\"true\"}",
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

}
