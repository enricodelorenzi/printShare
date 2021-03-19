package com.androidexam.printshare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

public class MaterialsFragment extends Fragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.test_recycle_view, container, false);
        RecyclerView mRecycleView = rootView.findViewById(R.id.my_recycle_view);
        MaterialsAdapter mAdapter = new MaterialsAdapter(getResources().getStringArray(R.array.materials));
        mRecycleView.setAdapter(mAdapter);
        return rootView;
    }
}
