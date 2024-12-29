package com.cashsify.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;

import androidx.navigation.fragment.NavHostFragment;

import com.cashsify.app.databinding.FragmentHomeBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private final String TAG = "HomeFragment";
    public FragmentHomeBinding binding;
    private int cashWalletTotal = 0;
    private int referWalletTotal = 0;
    private int cashWalletToday = 0;
    private int referWalletToday = 0;
    private int completedTask = 0;
    private String today;
    private final String documentId = Utils.getDocumentId();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration listenerRegistration;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        TextView tvdailyTask = root.findViewById(R.id.tvDailyTask);
        tvdailyTask.setOnClickListener(v -> navigateToAds());

        fetchDataFromFirestore();
        setupRealTimeListener();
        return root;
    }

    private void navigateToAds() {
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.action_home_to_ads);
    }

    private void setupRealTimeListener() {

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        if (documentId == null) {
            Log.e(TAG, "Document ID is null. Cannot set up real-time listener." + documentId);
            return;
        }
        listenerRegistration = db.collection("Earnings").document(documentId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening to Firestore updates", e);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {

                        cashWalletTotal = documentSnapshot.getLong("cashTotal") != null ? documentSnapshot.getLong("cashTotal").intValue() : 0;
                        cashWalletToday = documentSnapshot.getLong("cashToday") != null ? documentSnapshot.getLong("cashToday").intValue() : 0;

                        referWalletTotal = documentSnapshot.getLong("referTotal") != null ? documentSnapshot.getLong("referTotal").intValue() : 0;
                        referWalletToday = documentSnapshot.getLong("referToday") != null ? documentSnapshot.getLong("referToday").intValue() : 0;

                        completedTask = documentSnapshot.getLong("completedTasks") != null ? documentSnapshot.getLong("completedTasks").intValue() : 0;

                        today = documentSnapshot.getString("CurrentDate") != null ? documentSnapshot.getString("CurrentDate") : "";
                        if (completedTask >= 20) {
                            updateCompletedTaskUI();
                        } else {
                            updateUI();
                        }
                    } else {
                        createEarningsDocument();
                    }
                });
    }

    private void fetchDataFromFirestore() {
        if (documentId == null) {
            Log.e(TAG, "Document ID is null. Cannot fetch data." + documentId);
                return;
        }

        db.collection("Earnings").document(documentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        cashWalletTotal = documentSnapshot.getLong("cashTotal") != null ? documentSnapshot.getLong("cashTotal").intValue() : 0;
                        cashWalletToday = documentSnapshot.getLong("cashToday") != null ? documentSnapshot.getLong("cashToday").intValue() : 0;

                        referWalletTotal = documentSnapshot.getLong("referTotal") != null ? documentSnapshot.getLong("referTotal").intValue() : 0;
                        referWalletToday = documentSnapshot.getLong("referToday") != null ? documentSnapshot.getLong("referToday").intValue() : 0;

                        completedTask = documentSnapshot.getLong("completedTasks") != null ? documentSnapshot.getLong("completedTasks").intValue() : 0;

                        today = documentSnapshot.getString("CurrentDate") != null ? documentSnapshot.getString("CurrentDate") : "";


                        if (completedTask >= 20) {
                            updateCompletedTaskUI();
                        } else {
                            updateUI();
                        }
                    } else {
                        createEarningsDocument();
                    }
                }).addOnFailureListener(e -> Log.e("Firestore", "Error fetching data", e));
    }

    public void createEarningsDocument() {
        Map<String, Object> newEarnings = new HashMap<>();
        newEarnings.put("cashTotal", 0);
        newEarnings.put("cashToday", 0);
        newEarnings.put("referTotal", 0);
        newEarnings.put("referToday", 0);
        newEarnings.put("completedTasks", 0);
        newEarnings.put("ResetTime", FieldValue.serverTimestamp());
        newEarnings.put("CurrentDate", binding.tvDate.getText());


        db.collection("Earnings").document(documentId).set(newEarnings)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Earnings document created successfully for new user.");
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error creating earnings document", e));
    }
    private void updateCompletedTaskUI() {
        binding.tvDate.setText("("+today+")");
        binding.tvCompletedTask.setText("Completed ✅");
        binding.tvTotalTask.setVisibility(View.GONE);
        binding.tvDailyTask.setVisibility(View.GONE);
        binding.tvTotalCashWallet.setText("Cash Wallet\nRs. " + cashWalletTotal);
        Utils.setTotalCash(cashWalletTotal);
        binding.tvTotalCashRefer.setText("Refer Wallet\nRs. " + referWalletTotal);

        cashWalletToday = 5;
        binding.tvTodayCashWallet.setText("Cash Wallet\nRs. " + cashWalletToday);
        binding.tvTodayReferWallet.setText("Refer Wallet\nRs. " + referWalletToday);

        db.collection("Earnings").document(documentId).update("cashToday", cashWalletToday);
        Log.d(TAG, "User has completed all tasks for the day.");

    }

    private void updateUI() {
        binding.tvDate.setText("("+today+")");

        binding.tvTotalCashWallet.setText("Cash Wallet\nRs. " + cashWalletTotal);
        Utils.setTotalCash(cashWalletTotal);
        binding.tvTotalCashRefer.setText("Refer Wallet\nRs. " + referWalletTotal);

        binding.tvTodayCashWallet.setText("Cash Wallet\nRs. " + cashWalletToday);
        binding.tvTodayReferWallet.setText("Refer Wallet\nRs. " + referWalletToday);

        binding.tvCompletedTask.setText(String.valueOf(completedTask));
        binding.tvTotalTask.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
        binding = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
    }

}