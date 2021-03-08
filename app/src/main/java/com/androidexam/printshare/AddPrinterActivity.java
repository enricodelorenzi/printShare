package com.androidexam.printshare;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class AddPrinterActivity extends AppCompatActivity {

    private Spinner model_spinner;
    private TextView mText_1;
    private TextView mText_2;
    private EditText mEditText;
    private CheckBox mCheck_1;
    private CheckBox mCheck_2;
    private Button confirm;
    private Button cancel;
    private String model;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.add_printer_activity);

        mText_1 = findViewById(R.id.add_printer_materials_label);
        mText_2 = findViewById(R.id.add_printer_dimension_label);
        mEditText = findViewById(R.id.add_printer_dimension_edit);
        mCheck_1 = findViewById(R.id.add_printer_material_1);
        mCheck_2 = findViewById(R.id.add_printer_material_2);
        confirm = findViewById(R.id.add_printer_confirm_button);
        cancel = findViewById(R.id.add_printer_close_button);
        model_spinner = findViewById(R.id.add_printer_models_spinner);
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
                //Do something?
            }
        });

        confirm.setOnClickListener((v) ->{
            String dimension = (mEditText.getText().toString().length() == 0) ? "no dim" : mEditText.getText().toString();
            StringBuilder jsonInput = new StringBuilder();
            jsonInput.append("{\"model\":\""+model+"\"");
            jsonInput.append(",\"dimension\":\""+dimension+"\"");
            if(mCheck_1.isChecked() && mCheck_2.isChecked())
                jsonInput.append(",\"materials\":{\"0\":\"material_1\",\"1\":\"material_2\"}");
            else if(mCheck_1.isChecked() && !mCheck_2.isChecked())
                jsonInput.append(",\"materials\":\"material_1\"");
            else if(!mCheck_1.isChecked() && mCheck_2.isChecked())
                jsonInput.append(",\"materials\":\"material_2\"");
            jsonInput.append("}");
            String uid = FirebaseAuth.getInstance().getUid();
            new DbCommunication(DbCommunication.OPERATIONS.POST).launchAsyncTask("POST","users/"+uid+"/printers", jsonInput.toString());
            new DbCommunication(DbCommunication.OPERATIONS.PATCH).launchAsyncTask("PATCH","printer_models/"+model,"{\""+uid+"\":\"true\"}");
            finish();
        });

        cancel.setOnClickListener((v) -> {
            finish();
        });
        super.onCreate(savedInstanceState);
    }
}
