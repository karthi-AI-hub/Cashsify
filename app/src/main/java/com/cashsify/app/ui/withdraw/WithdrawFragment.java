package com.cashsify.app.ui.withdraw;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.cashsify.app.databinding.FragmentWithdrawBinding;


public class WithdrawFragment extends Fragment {

    private FragmentWithdrawBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        WithdrawViewModel withdrawViewModel =
                new ViewModelProvider(this).get(WithdrawViewModel.class);

        binding = FragmentWithdrawBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textWithdraw;
        withdrawViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}