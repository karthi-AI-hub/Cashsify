package com.cashsify.app.ui.ads;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.cashsify.app.databinding.FragmentAdsBinding;

public class AdsFragment extends Fragment {

    private @NonNull FragmentAdsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        AdsViewModel adsViewModel =
                new ViewModelProvider(this).get(AdsViewModel.class);

        binding = FragmentAdsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textAds;
        adsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}