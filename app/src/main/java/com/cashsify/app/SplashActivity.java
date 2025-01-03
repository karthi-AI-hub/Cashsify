package com.cashsify.app;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.pm.PackageManager;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.content.Intent;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DISPLAY_LENGTH = 2000;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.VIBRATE
    };
    String documentId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_splash);

        ImageView logoImage = findViewById(R.id.logoImage);
        TextView appNameText = findViewById(R.id.appNameText);
        TextView taglineText = findViewById(R.id.taglineText);

        // Start animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.logo_scale);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.scale_up);

        ObjectAnimator rotation = ObjectAnimator.ofFloat(logoImage, "rotationY", 180f, 720f);
        rotation.setDuration(1800);
        rotation.setInterpolator(new AccelerateDecelerateInterpolator());
        rotation.start();

        appNameText.startAnimation(slideUp);
        taglineText.startAnimation(fadeIn);

        new Handler().postDelayed(this::checkPermissions, SPLASH_DISPLAY_LENGTH);
    }

    private void checkPermissions() {
        boolean allPermissionsGranted = true;

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (allPermissionsGranted) {
            checkAppStatus();
        } else {
            requestPermissions();
        }
    }

    private void requestPermissions() {
        boolean shouldShowRationale = false;
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                shouldShowRationale = true;
                break;
            }
        }

        if (shouldShowRationale) {
            showPermissionRationaleDialog();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
        }
    }

    private void showPermissionRationaleDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("The app requires these permissions to function properly. Please grant them to continue using the app.")
                .setPositiveButton("Grant Permissions", (dialog, which) -> ActivityCompat.requestPermissions(SplashActivity.this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE))
                .setNegativeButton("Exit App", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                checkAppStatus();
            } else {
                showPermissionDeniedDialog();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Denied")
                .setMessage("Some permissions are required for the app to work properly. Without these permissions, certain features may not function. You can enable them from the app settings.")
                .setPositiveButton("Grant Permissions", (dialog, which) -> {
                    ActivityCompat.requestPermissions(SplashActivity.this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
                })
                .setNegativeButton("Go to Settings", (dialog, which) -> openAppSettings())
                .setCancelable(false)
                .show();
    }


    private void openAppSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(android.net.Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }


    private void checkAppStatus() {
        if (Utils.isNetworkConnected(this)) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("AppSettings").document("SplashSettings")
                    .get()
                    .addOnSuccessListener(task -> {
                        boolean isGlobalEnabled = task.getBoolean("Status") != null && task.getBoolean("Status");
                        if (isGlobalEnabled) {
                            checkUser();
                        } else {
                            Utils.intend(SplashActivity.this, MaintenanceActivity.class);
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        checkUser();
                    });
        } else {
            Utils.intend(SplashActivity.this, NoInternetActivity.class);
            Log.d("SplashActivity", "Navigating to Nointernet activity");
            finish();
        }
    }


    private void checkUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("Users")
                    .whereEqualTo("Email", user.getEmail())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                            task.getResult().forEach(documentSnapshot -> {
                                documentId = documentSnapshot.getId();
                                String userName = documentSnapshot.getString("Name");
                                Utils.setDocumentId(documentId);
                                Utils.setUserEmail(user.getEmail());
                                Utils.setUserName(userName);

                                db.collection("Users").document(documentId)
                                        .update("LastLogin", FieldValue.serverTimestamp())
                                        .addOnSuccessListener(aVoid -> {
                                            setTodayInFB(db, documentId);
                                        })
                                        .addOnFailureListener(e -> {
                                            Utils.intend(SplashActivity.this, MainActivity.class);
                                            finish();
                                        });
                            });
                        } else {
                            Log.e("SplashActivity", "User document not found or task failed.");
                            documentId = null;
                            Utils.intend(SplashActivity.this, LoginActivity.class);
                            finish();
                        }
                    });
        } else {
            Utils.intend(SplashActivity.this, RegisterActivity.class);
            finish();
        }
    }


    private void setTodayInFB(FirebaseFirestore db, String documentId) {
        db.collection("Users").document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Timestamp lastLoginObj = documentSnapshot.getTimestamp("LastLogin");
                        if (lastLoginObj != null) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
                            Date lastLoginDate = lastLoginObj.toDate();
                            String formattedDate = dateFormat.format(lastLoginDate);
                            updateFormattedDate(db, documentId, formattedDate);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.d("MainActivity", "Failed to fetch user data: " + e.getMessage()));

    }

    private void updateFormattedDate(FirebaseFirestore db, String documentId, String formattedDate) {
        Map<String, Object> data = new HashMap<>();
        data.put("CurrentDate", formattedDate);
        db.collection("Earnings").document(documentId)
                .set(data, SetOptions.merge()).addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        checkAndResetEarnings(this);
                    }
                });
    }


    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }


    public void checkAndResetEarnings(Context context) {
        FirebaseFirestore.getInstance().collection("Earnings")
                .document(Utils.getDocumentId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String CurrentDate = documentSnapshot.getString("CurrentDate") != null
                            ? documentSnapshot.getString("CurrentDate").trim()
                            : getCurrentDate();

                    String LastResetDate = documentSnapshot.getTimestamp("ResetTime") != null
                            ? formatDate(documentSnapshot.getTimestamp("ResetTime"))
                            : null;

                    if (!CurrentDate.equals(LastResetDate)) {
                        resetEarnings(context);
                    } else {
                        Utils.intend(SplashActivity.this, MainActivity.class);
                        finish();
                        Log.d("ResetEarningsWorker", "No need to reset earnings.");
                    }
                })
                .addOnFailureListener(e -> Log.e("ResetEarningsWorker", "Error fetching ResetTime", e));
    }


    public void resetEarnings(Context context) {
        WorkManager.getInstance(context).enqueue(new OneTimeWorkRequest.Builder(ResetEarningsWorker.class).build());
        Utils.intend(SplashActivity.this, MainActivity.class);
        finish();
    }

    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
        return sdf.format(new Date()).trim();
    }

    public static String formatDate(Timestamp timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
        Date date = timestamp.toDate();
        return sdf.format(date).trim();
    }
}
