package com.androidexam.printshare.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.androidexam.printshare.utilities.MaterialListItem;
import com.androidexam.printshare.R;

import java.util.List;


public class MaterialsAdapter extends RecyclerView.Adapter<MaterialsAdapter.ViewHolder> {

    private final List<MaterialListItem> materials;

    public MaterialsAdapter(List<MaterialListItem> materials){
        this.materials = materials;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        private final CheckBox newCheckBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            newCheckBox = itemView.findViewById(R.id.material_checkbox);
        }

        public CheckBox getNewCheckBox(){
            return newCheckBox;
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
        CheckBox mCheckBox = holder.getNewCheckBox();
        mCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            materials.get(position).setChecked(isChecked);
        });

        mCheckBox.setText(materials.get(position).getMaterial());
        mCheckBox.setChecked(materials.get(position).isChecked());
    }

    @Override
    public int getItemCount() {
        return materials.size();
    }

}
