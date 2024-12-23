package com.cashsify.app;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
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

    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private int cashWalletTotal = 0;
    private int referWalletTotal = 0;
    private int cashWalletToday = 0;
    private int referWalletToday = 0;
    private int completedTask = 0;
    private String documentId;
    private FirebaseFirestore db;
    private ListenerRegistration listenerRegistration;
    private String toDay;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            Utils.init((Activity) context);
        }
    }


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        documentId = Utils.getDocumentId();
        db = Utils.getFirestoreInstance();
        Utils.getCurrentDate(new Utils.OnLastLoginFetchedListener() {
            @Override
            public void onLastLoginFetched(String lastLoginDate) {
                toDay = lastLoginDate;
                binding.tvDate.setText("(" + toDay + ")");
            }

            @Override
            public void onError(String errorMessage) {
                binding.tvDate.setText("");
            }
        });

        TextView tvdailyTask = root.findViewById(R.id.tvDailyTask);
        tvdailyTask.setOnClickListener(v -> navigateToAds());

        return root;
    }

    private void navigateToAds() {
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.action_home_to_ads);
    }

    private void setupRealTimeListener() {

        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }

        if (documentId == null) {
            Log.e(TAG, "Document ID is null. Cannot set up real-time listener.");
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

                        if (completedTask >= 20) {
                            updateCompletedTaskUI();
                        } else {
                            updateUI();
                        }
                        resetDailyDataIfNeeded(documentSnapshot.getTimestamp("lastUpdated"), toDay);
                    } else {
                        Log.e(TAG, "Document does not exist in Firestore.");
                    }
                });
    }

    private void fetchDataFromFirestore() {
        if (documentId == null) {
            Log.e(TAG, "Document ID is null. Cannot fetch data.");
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


                        if (completedTask >= 20) {
                            updateCompletedTaskUI();
                        } else {
                            updateUI();
                        }
                        resetDailyDataIfNeeded(documentSnapshot.getTimestamp("lastUpdated"), toDay);
                    }else {
                        createEarningsDocument();
                    }
                }) .addOnFailureListener(e -> Log.e("Firestore", "Error fetching data", e));
    }

    private void createEarningsDocument() {
        Map<String, Object> newEarnings = new HashMap<>();
        newEarnings.put("cashTotal", 0);
        newEarnings.put("cashToday", 0);
        newEarnings.put("referTotal", 0);
        newEarnings.put("referToday", 0);
        newEarnings.put("completedTasks", 0);
        newEarnings.put("lastUpdated", FieldValue.serverTimestamp());

        db.collection("Earnings").document(documentId).set(newEarnings)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Earnings document created successfully for new user.");
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error creating earnings document", e));
    }
    private void updateCompletedTaskUI() {
        binding.tvCompletedTask.setText("Completed ✅");
        binding.tvTotalTask.setVisibility(View.GONE);
        binding.tvDailyTask.setVisibility(View.GONE);
        binding.tvTotalCashWallet.setText("Cash Wallet\nRs. " + cashWalletTotal);
        Utils.setTotalCash(cashWalletTotal);
        binding.tvTotalCashRefer.setText("Refer Wallet\nRs. " + referWalletTotal);

        cashWalletToday = 5;
        binding.tvTodayCashWallet.setText("Cash Wallet\nRs. " + cashWalletToday);
        binding.tvTodayReferWallet.setText("Refer Wallet\nRs. " + referWalletToday);

        Log.d(TAG, "User has completed all tasks for the day.");

    }

    private void updateUI() {
        binding.tvTotalCashWallet.setText("Cash Wallet\nRs. " + cashWalletTotal);
        Utils.setTotalCash(cashWalletTotal);
        binding.tvTotalCashRefer.setText("Refer Wallet\nRs. " + referWalletTotal);

        binding.tvTodayCashWallet.setText("Cash Wallet\nRs. " + cashWalletToday);
        binding.tvTodayReferWallet.setText("Refer Wallet\nRs. " + referWalletToday);

        binding.tvCompletedTask.setText(String.valueOf(completedTask));
        binding.tvTotalTask.setVisibility(View.VISIBLE);
    }

    private void resetDailyData() {

        int updatedCashTotal = cashWalletTotal + cashWalletToday;
        int updatedReferTotal = referWalletTotal + referWalletToday;

        Map<String, Object> updates = new HashMap<>();
        updates.put("cashTotal", updatedCashTotal);
        updates.put("cashToday", 0);

        updates.put("referTotal", updatedReferTotal);
        updates.put("referToday", 0);

        updates.put("completedTasks", 0);
        updates.put("lastUpdated", FieldValue.serverTimestamp());

        db.collection("Earnings").document(documentId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Daily data reset successfully.");
                    Utils.setTotalCash(updatedCashTotal);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error resetting daily data", e));
    }

    private void resetDailyDataIfNeeded(Timestamp lastUpdated, String toDay) {
        if (lastUpdated == null || toDay == null) {
            Log.e(TAG, "Invalid parameters for resetDailyDataIfNeeded.");
            return;
        }

        db.collection("Earnings").document(documentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String lastResetDate = documentSnapshot.getString("lastResetDate");
                        Log.d(TAG, "Last Reset Date: " + lastResetDate);
                        Log.d(TAG, "Today's Date: " + toDay);

                        if (lastResetDate == null || !lastResetDate.equals(toDay)) {
                            Log.d(TAG, "Resetting daily data for a new day.");
                            resetDailyData();
                            updateLastResetDate(toDay);
                        } else {
                            Log.d(TAG, "Daily data already reset for today.");
                        }
                    } else {
                        Log.e(TAG, "Document not found for resetDailyDataIfNeeded.");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching last reset date", e));
    }


    private void updateLastResetDate(String toDay) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastResetDate", toDay);
        db.collection("Earnings").document(documentId).update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Last reset date updated successfully."))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating last reset date", e));
    }


    @Override
    public void onStart() {
        super.onStart();
        fetchDataFromFirestore();
        setupRealTimeListener();

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
}