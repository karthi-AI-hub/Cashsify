package com.cashsify.app;

import static com.cashsify.app.Utils.showExitDialog;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MaintenanceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_maintenance);

    }
    @Override
    public void onBackPressed() {
        showExitDialog(this);
    }
}