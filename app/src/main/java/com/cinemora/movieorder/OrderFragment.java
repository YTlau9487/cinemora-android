package com.cinemora.movieorder;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class OrderFragment extends Fragment {

    private boolean isLoaded = false;

    public OrderFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order, container, false);
        loadDataIfNeeded();
        return view;
    }

    public void loadDataIfNeeded() {
        if (!isLoaded && getView() != null) {
            // Load order data from Firebase here
            isLoaded = true;
        }
    }
}