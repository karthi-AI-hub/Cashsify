package com.cashsify.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Utils {

    public static final String ERROR_NO_INTERNET = "No Internet Connection. Make sure you are connected to Internet .";
    public static final String ERROR_INVALID_EMAIL = "Invalid Email Address";
    public static final String ERROR_INVALID_PHONE = "Invalid Phone Number";
    public static final String ERROR_INVALID_PASSWORD = "Invalid Password";
    private static String documentId;
    private static String userEmail;
    private static String userName;
    private static int totalCash = 0;
    private static FirebaseFirestore db;

    public static void intend(Context context, Class<?> cls) {
        if (context != null && cls != null) {
            Intent intent = new Intent(context, cls);
            context.startActivity(intent);
        }
    }

    public static void showSnackBar(View view, String message, String duration) {
        if (view != null && message != null && duration != null) {
            int snackbarDuration = Snackbar.LENGTH_SHORT;
            if (duration.equalsIgnoreCase("long")) {
                snackbarDuration = Snackbar.LENGTH_LONG;
            }
            Snackbar.make(view, message, snackbarDuration).show();
        }
    }

    public static void showToast(Context context, String message) {
        if (context != null && message != null) {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    public static void HideKeyboard(AppCompatActivity activity, View view) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void togglePasswordVisibility(EditText passwordEditText, ImageView ivTogglePassword) {
        boolean isPasswordVisible = passwordEditText.getTransformationMethod() instanceof PasswordTransformationMethod;
        passwordEditText.setTransformationMethod(isPasswordVisible ? null : new PasswordTransformationMethod());
        ivTogglePassword.setImageResource(isPasswordVisible ? R.drawable.ic_visibility : R.drawable.ic_visibility_off);
        ivTogglePassword.setContentDescription(isPasswordVisible ? "Hide password" : "Show password");
        passwordEditText.setSelection(passwordEditText.length());
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } else {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
    }

    public static void showExitDialog(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Exit Application")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    activity.finishAffinity();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    public static void setError(EditText et, String str) {
        et.setError(str);
        et.requestFocus();
    }

    public static void resetError(EditText et) {
        et.setError(null);
    }

    public static void init(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("UserCredits", Context.MODE_PRIVATE);
        documentId = prefs.getString("documentId", null);
        userEmail = prefs.getString("userEmail", null);

        if (documentId == null || documentId.isEmpty()) {
            Log.e("Utils", "Document ID is null or empty!");
            return;
        }
        if (userEmail == null || userEmail.isEmpty()) {
            Log.e("Utils", "User Email is null or empty!");
            return;
        }

        db = FirebaseFirestore.getInstance();
        db.collection("Users").document(documentId).get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userName = documentSnapshot.getString("Name");
                        if (TextUtils.isEmpty(userName)) {
                            userName = "Guest";
                        }
                    } else {
                        userName = "Guest";
                    }
                })
                .addOnFailureListener(e -> userName = "Guest");
    }

    public static String getDocumentId() {
        return documentId;
    }

    public static void setDocumentId(String documentId) {
        Utils.documentId = documentId;
    }

    public static String getUserEmail() {
        return userEmail;
    }

    public static FirebaseFirestore getFirestoreInstance() {
        return db;
    }

    public static String getUserName() {
        return userName;
    }

    public static void setTotalCash(int newTotalCash){
        totalCash = newTotalCash;
        db.collection("Earnings").document(documentId)
                .update("cashTotal", totalCash);
    }
    public static int getTotalCash() {
        return totalCash;
    }

    public static void getCurrentDate(final OnLastLoginFetchedListener listener) {
        db.collection("Users").document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Timestamp timestamp = documentSnapshot.getTimestamp("LastLogin");
                        if (timestamp != null) {
                            Date lastLoginDate = timestamp.toDate();
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
                            String formattedDate = dateFormat.format(lastLoginDate);
                            listener.onLastLoginFetched(formattedDate);
                        }else {
                            listener.onError("No LastLogin timestamp found.");
                        }
                    } else {
                        listener.onError("User document not found.");
                    }
                })
                .addOnFailureListener(e -> listener.onError("Error fetching LastLogin: " + e.getMessage()));
    }
    public interface OnLastLoginFetchedListener {
        void onLastLoginFetched(String lastLoginDate);
        void onError(String errorMessage);
    }


}