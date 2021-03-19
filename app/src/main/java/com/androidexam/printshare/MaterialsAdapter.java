package com.androidexam.printshare;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


public class MaterialsAdapter extends RecyclerView.Adapter<MaterialsAdapter.ViewHolder> {

    private final String[] materials;


    MaterialsAdapter(String[] materials){

        this.materials = materials;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        private final CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.material_checkbox);
            //checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> buttonView.getContext().getSharedPreferences("add_printer_saved_values", Context.MODE_PRIVATE)
              //      .edit().putBoolean(buttonView.getText().toString(), isChecked).apply());
            checkBox.setOnClickListener(view -> view.getContext().getSharedPreferences("add_printer_saved_values", Context.MODE_PRIVATE)
                    .edit().putBoolean(((CheckBox)view).getText().toString(), ((CheckBox)view).isChecked()).apply());
        }

        public CheckBox getCheckBox(){
            return checkBox;
        }
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.material_checkboxes,parent,false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.getCheckBox().setText(materials[position]);
    }

    @Override
    public int getItemCount() {
        return materials.length;
    }
}
