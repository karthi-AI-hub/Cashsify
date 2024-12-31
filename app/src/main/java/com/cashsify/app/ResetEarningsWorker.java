package com.cashsify.app;

import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ResetEarningsWorker extends Worker {

    private static final String TAG = "ResetEarningsWorker";

    public ResetEarningsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "ResetEarningsWorker started.");
        String documentId = Utils.getDocumentId();
        if (documentId == null) {
            Log.e(TAG, "Document ID is null. Reset aborted.");
            return Result.failure();
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Earnings").document(documentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        int cashToday = documentSnapshot.getLong("cashToday") != null
                                ? documentSnapshot.getLong("cashToday").intValue()
                                : 0;

                        int referToday = documentSnapshot.getLong("referToday") != null
                                ? documentSnapshot.getLong("referToday").intValue()
                                : 0;

                        int cashTotal = documentSnapshot.getLong("cashTotal") != null
                                ? documentSnapshot.getLong("cashTotal").intValue()
                                : 0;

                        int referTotal = documentSnapshot.getLong("referTotal") != null
                                ? documentSnapshot.getLong("referTotal").intValue()
                                : 0;


                        Map<String, Object> updates = new HashMap<>();
                        updates.put("cashTotal", cashTotal + cashToday);
                        updates.put("referTotal", referTotal + referToday);
                        updates.put("cashToday", 0);
                        updates.put("referToday", 0);
                        updates.put("completedTasks", 0);
                        updates.put("ResetTime", FieldValue.serverTimestamp());

                        db.collection("Earnings").document(documentId).update(updates)
                                .addOnSuccessListener(aVoid -> {})
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to reset earnings.", e));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching user earnings document.", e));

        return Result.success();
    }
}
