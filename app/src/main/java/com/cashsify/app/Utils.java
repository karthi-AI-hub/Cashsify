package com.cashsify.app;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

public class Utils {

    public static final String ERROR_NO_INTERNET = "No Internet Connection. Make sure you are connected to Internet .";
    public static final String ERROR_INVALID_EMAIL = "Invalid Email Address";
    public static final String ERROR_INVALID_PHONE = "Invalid Phone Number";
    public static final String ERROR_INVALID_PASSWORD = "Invalid Password";

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

    public static void setError(EditText et, String str){
        et.setError(str);
        et.requestFocus();
    }

    public static void resetError(EditText et){
        et.setError(null);
    }



}


