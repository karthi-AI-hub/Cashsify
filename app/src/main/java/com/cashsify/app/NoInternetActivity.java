package com.cashsify.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class NoInternetActivity extends AppCompatActivity {

    private Button retryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_no_internet);

        retryButton = findViewById(R.id.retryButton);
        retryButton.setOnClickListener(v -> {
            showNetworkStatusDialog();
        });
    }

    private void showNetworkStatusDialog() {
        if (!Utils.isNetworkConnected(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("No Internet Connection")
                    .setMessage("It seems like you're not connected to the internet. Please check your Wi-Fi or mobile data settings.")
                    .setPositiveButton("OK", null)
                    .setNegativeButton("Turn On Wi-Fi", (dialog, which) -> {
                        startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
                    })
                    .setNeutralButton("Turn On Mobile Data", (dialog, which) -> {
                        startActivity(new Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS));
                    })
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Internet Connected")
                    .setMessage("Your internet connection is active. Continue your Earnings!")
                    .setPositiveButton("CONTINUE", (dialog, which) -> {
                        Utils.intend(NoInternetActivity.this, SplashActivity.class);
                        finish();
                    })
                    .show();
        }
    }

}