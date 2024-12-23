package com.cashsify.app;

import android.util.Log;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

public class CurrencyUtils {

    private static final String TAG = "AdLogs";
    private final FirebaseFirestore db;

    public CurrencyUtils() {
        db = Utils.getFirestoreInstance();
    }

    public boolean incrementRewardForUser(String documentId, int increment) {
        if (documentId == null || documentId.isEmpty()) {
            Log.e(TAG, "Invalid document ID.");
            return false;
        }

        DocumentReference userDocRef = db.collection("Earnings").document(documentId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            Long currentReward = transaction.get(userDocRef).getLong("completedTasks");
            if (currentReward == null) {
                currentReward = 0L;
            }

            transaction.update(userDocRef, "completedTasks", currentReward + increment);
            return null;
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Reward incremented successfully.");
            } else {
                Log.e(TAG, "Failed to increment reward: ", task.getException());
            }
        });

        return true;
    }
}
