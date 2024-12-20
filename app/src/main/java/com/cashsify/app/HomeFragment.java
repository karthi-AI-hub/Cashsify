package com.cashsify.app;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;


import com.cashsify.app.databinding.FragmentHomeBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
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

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Utils.init(requireActivity());
        documentId = Utils.getDocumentId();
        db = Utils.getFirestoreInstance();
        fetchDataFromFirestore();
        setupRealTimeListener();
        binding.btnNextAd.setOnClickListener(v -> incrementCompletedTask());
        binding.btnNextDay.setOnClickListener(v -> {
            binding.btnNextAd.setVisibility(View.VISIBLE);
            simulateNextDayForTesting();
        });
        return root;
    }
    private void setupRealTimeListener() {
        if (documentId == null) {
            Log.e(TAG, "Document ID is null. Cannot set up real-time listener.");
            return;
        }

        // Add a real-time listener to the document
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

                        if (completedTask == 20) {
                            updateCompletedTaskUI();
                        } else {
                            updateUI();
                        }

                        resetDailyDataIfNeeded(documentSnapshot.getTimestamp("lastUpdated"));
                    } else {
                        Log.e(TAG, "Document does not exist in Firestore.");
                    }
                });
    }

    private void simulateNextDayForTesting() {
        // Simulate a timestamp for the previous day
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        Date simulatedDate = calendar.getTime();
        Timestamp simulatedTimestamp = new Timestamp(simulatedDate);

        // Call resetDailyDataIfNeeded with the simulated timestamp
        resetDailyDataIfNeeded(simulatedTimestamp);

        Log.d(TAG, "Simulated Next Day triggered. Timestamp: " + simulatedTimestamp.toDate());
    }
    private void incrementCompletedTask() {
        if (documentId == null) {
            Log.e(TAG, "Document ID is null. Cannot increment completed tasks.");
            return;
        }
        completedTask++;

        binding.tvCompletedTask.setText(String.valueOf(completedTask));

        Map<String, Object> updates = new HashMap<>();
        updates.put("completedTasks", completedTask);

        db.collection("Earnings").document(documentId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Completed Task incremented successfully in Firestore.");
                    if (completedTask >= 20) {
                        updateCompletedTaskUI();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error incrementing completed tasks", e));
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


                        if (completedTask == 20) {
                            updateCompletedTaskUI();
//                            resetTodayEarnings();
                        } else {
                            updateUI();
                        }
                        resetDailyDataIfNeeded(documentSnapshot.getTimestamp("lastUpdated"));
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
        binding.btnNextAd.setVisibility(View.GONE);
        binding.tvTotalCashWallet.setText("Cash Wallet\nRs. " + cashWalletTotal);
        binding.tvTotalCashRefer.setText("Refer Wallet\nRs. " + referWalletTotal);

        cashWalletToday = 5;
        binding.tvTodayCashWallet.setText("Cash Wallet\nRs. " + cashWalletToday);
        binding.tvTodayReferWallet.setText("Refer Wallet\nRs. " + referWalletToday);

        Log.d(TAG, "User has completed all tasks for the day.");

    }

//    private void resetTodayEarnings() {
//        Map<String, Object> updates = new HashMap<>();
//        updates.put("cashToday", 0);
//        updates.put("referToday", 0);
//        updates.put("completedTasks", 0);
//
//        db.collection("Earnings").document(documentId).update(updates)
//                .addOnSuccessListener(aVoid -> Log.d(TAG, "Earnings and tasks reset successfully."))
//                .addOnFailureListener(e -> Log.e(TAG, "Error resetting earnings and tasks", e));
//    }



    private void updateUI() {
        binding.tvTotalCashWallet.setText("Cash Wallet\nRs. " + cashWalletTotal);
        binding.tvTotalCashRefer.setText("Refer Wallet\nRs. " + referWalletTotal);

        binding.tvTodayCashWallet.setText("Cash Wallet\nRs. " + cashWalletToday);
        binding.tvTodayReferWallet.setText("Refer Wallet\nRs. " + referWalletToday);

        binding.tvCompletedTask.setText(String.valueOf(completedTask));
        binding.tvTotalTask.setVisibility(View.VISIBLE);
    }


    private void resetDailyDataIfNeeded(Timestamp lastUpdated) {
        if (lastUpdated != null && isDifferentDay(lastUpdated)) {
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
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Daily data reset successfully."))
                    .addOnFailureListener(e -> Log.e(TAG, "Error resetting daily data", e));
        }
    }

    private boolean isDifferentDay(Timestamp lastUpdated) {
        LocalDate lastUpdateDate = lastUpdated.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate today = LocalDate.now();

        return !today.equals(lastUpdateDate);
    }


    @Override
    public void onStart() {
        super.onStart();
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