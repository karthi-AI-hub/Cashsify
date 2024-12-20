package com.cashsify.app.ui.refer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;


import com.cashsify.app.databinding.FragmentReferBinding;


public class ReferFragment extends Fragment {

    private FragmentReferBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ReferViewModel referViewModel =
                new ViewModelProvider(this).get(ReferViewModel.class);

        binding = FragmentReferBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textRefer;
        referViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}