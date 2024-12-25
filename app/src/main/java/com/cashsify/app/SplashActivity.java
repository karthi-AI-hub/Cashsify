package com.cashsify.app;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.Manifest;
import android.content.Intent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

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

        // Check permissions after animations
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
        // Check for individual permissions and explain rationale if needed
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
            // Request permissions without rationale if it's not needed
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
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Utils.intend(SplashActivity.this, MainActivity.class);
            finish();
        } else {
            Utils.intend(SplashActivity.this, RegisterActivity.class);
            finish();
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
