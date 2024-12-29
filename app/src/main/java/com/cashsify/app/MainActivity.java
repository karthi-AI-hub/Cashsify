package com.cashsify.app;

import static com.cashsify.app.Utils.getDocumentId;
import static com.cashsify.app.Utils.showExitDialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.widget.TextView;

import com.google.android.gms.ads.MobileAds;
import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.cashsify.app.databinding.ActivityMainBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    private TextView TvUserPhoneNumber, TvUserEmail;
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private AdHelper adHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MainActivity", "MainActivity OnCreate called");
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        MobileAds.initialize(this, initializationStatus -> Log.d("AdLogs", "AdMob initialized."));

        setSupportActionBar(binding.appBarMain.toolbar);

        binding.appBarMain.fab.setOnClickListener(view -> {
            String email = "cashsify@gmail.com";
            String subject = "Enter your Issues or Quires here. ";
            String body = "Dear Cashsify Team,\n\nI have some queries in Cashsify Application. Please assist me with this Queries.\n\n\n\n[YOUR_QUERIES]\n\n\n\nThank you,\n[YOUR PHONE_NUMBER]";

            String mailto = "mailto:" + email +
                    "?subject=" + Uri.encode(subject) +
                    "&body=" + Uri.encode(body);

            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);

            emailIntent.setData(Uri.parse(mailto));
            startActivity(Intent.createChooser(emailIntent, "Send email"));
        });


        setupNavigation();

        adHelper = new AdHelper(this);
        scheduleResetEarningsWorker(this);
        checkAndResetEarnings();
    }

    public void scheduleResetEarningsWorker(Context context) {
        FirebaseFirestore.getInstance().collection("Users").document(Utils.getDocumentId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Timestamp serverTimestamp = documentSnapshot.getTimestamp("LastLogin");
                    if (serverTimestamp != null) {
                        long nextMidnightMillis = getNextMidnightMillis(serverTimestamp);
                        long currentTimeMillis = System.currentTimeMillis();
                        long initialDelay = nextMidnightMillis - currentTimeMillis;

                        Constraints constraints = new Constraints.Builder()
                                .setRequiresBatteryNotLow(false)
                                .setRequiresDeviceIdle(false)
                                .build();

                        PeriodicWorkRequest resetEarningsWork = new PeriodicWorkRequest.Builder(
                                ResetEarningsWorker.class, 1, TimeUnit.DAYS)
                                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                                .setConstraints(constraints)
                                .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
                                .build();

                        WorkManager.getInstance(MainActivity.this).enqueueUniquePeriodicWork(
                                "ResetEarningsTask",
                                ExistingPeriodicWorkPolicy.REPLACE,
                                resetEarningsWork
                        );
                        Log.d("MainActivity", "WorkManager scheduled for daily earnings reset.");
                    }
                })
                .addOnFailureListener(e -> Log.e("MainActivity", "Failed to fetch server time", e));
    }

    private long getNextMidnightMillis(Timestamp serverTimestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(serverTimestamp.toDate());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTimeInMillis();
    }

    public AdHelper getAdHelper() {
        return adHelper;
    }

    private void setupNavigation(){
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_profile, R.id.nav_ads, R.id.nav_withdraw, R.id.nav_refer, R.id.nav_help)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        if (getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main) instanceof NavHostFragment) {
            NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
            NavigationUI.setupWithNavController(navigationView, navController);
        }


        navigationView.setNavigationItemSelectedListener(item -> {
            boolean handled = false;

            if (item.getItemId() == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
                startActivity(intent);
                finish();
                handled = true;
                return true;
            } else if (item.getItemId() == R.id.nav_aboutUs) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/cashsify/home/"));
                startActivity(browserIntent);
                handled = true;
            }else {
                handled = NavigationUI.onNavDestinationSelected(item, navController);
            }
            if (handled) {
                binding.drawerLayout.closeDrawer(GravityCompat.START);
            }
            return handled;
        });
    }

    private void initUI() {
        NavigationView navigationView = binding.navView;
        View headerView = navigationView.getHeaderView(0);

        TvUserPhoneNumber = headerView.findViewById(R.id.tv_UserPhoneNumber);
        TvUserEmail = headerView.findViewById(R.id.tv_UserEmail);

        String documentId = Utils.getDocumentId();
        TvUserPhoneNumber.setText(documentId != null ? documentId : "Guest");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        TvUserEmail.setText(currentUser != null ? Utils.getUserEmail() : "N/A");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void checkAndResetEarnings() {
        FirebaseFirestore.getInstance().collection("Earnings")
                .document(Utils.getDocumentId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String CurrentDate = documentSnapshot.getString("CurrentDate") != null
                            ? documentSnapshot.getString("CurrentDate").trim()
                            : getCurrentDate();

                    String LastResetDate = documentSnapshot.getTimestamp("ResetTime") != null
                            ? formatDate(documentSnapshot.getTimestamp("ResetTime"))
                            : null ;

                    if (CurrentDate != null && !CurrentDate.equals(LastResetDate)) {
                        Log.d("ResetEarningsWorker", "CurrentDate : " + CurrentDate + "\nLastResetDate : " + LastResetDate);
                        resetEarnings();
                    } else {
                        Log.d("ResetEarningsWorker", "No need to reset earnings.");
                    }
                })
                .addOnFailureListener(e -> Log.e("ResetEarningsWorker", "Error fetching ResetTime", e));
    }


    private void resetEarnings() {
        Log.d("ResetEarningsWorker", "Earnings reset triggered.");
        WorkManager.getInstance(this).enqueue(new OneTimeWorkRequest.Builder(ResetEarningsWorker.class).build());
        Log.d("ResetEarningsWorker", "Reset earnings work has been enqueued.");

    }
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
        return sdf.format(new Date()).trim();
    }

    private String formatDate(Timestamp timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
        Date date = timestamp.toDate();
        return sdf.format(date).trim();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("MainActivity", "MainActivity OnStart called");
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.addAuthStateListener(firebaseAuth -> {
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            if (currentUser != null) {
                initUI();
            } else {
                Log.d("MainActivity", "User not authenticated.");
            }
        });
    }

    @Override
    public void onBackPressed() {
        showExitDialog(this);
    }
}