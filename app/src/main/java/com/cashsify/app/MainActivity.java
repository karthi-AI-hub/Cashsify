package com.cashsify.app;

import static com.cashsify.app.Utils.showExitDialog;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.widget.TextView;

import com.google.android.gms.ads.MobileAds;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.cashsify.app.databinding.ActivityMainBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;


public class MainActivity extends AppCompatActivity {

    private TextView TvUserPhoneNumber, TvUserEmail;
    private String documentId;
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private AdHelper adHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

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

        TvUserPhoneNumber = headerView.findViewById(R.id.tvUserPhoneNumber);
        TvUserEmail = headerView.findViewById(R.id.tvUserEmail);

        TvUserEmail.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());
        TvUserPhoneNumber.setText(documentId);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            TvUserEmail.setText(currentUser.getEmail());
        } else {
            TvUserEmail.setText("Guest");
        }

        TvUserPhoneNumber.setText(documentId);

        if (documentId != null && !documentId.isEmpty() && !documentId.equals("No ID Found") && currentUser != null ) {
            SharedPreferences prefs = getSharedPreferences("UserCredits", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("documentId", documentId);
            editor.putString("userEmail", currentUser.getEmail());
            editor.apply();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("Users")
                    .whereEqualTo("Email", user.getEmail())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                            task.getResult().forEach(documentSnapshot -> {
                                documentId = documentSnapshot.getId();

                                db.collection("Users").document(documentId)
                                        .update("LastLogin", FieldValue.serverTimestamp());
                            });
                            initUI();
                        }else{
                            documentId = "No ID Found";
                            initUI();
                        }
                    });
        }else{
            documentId = "User Not Logged In";
            try{
                initUI();
                }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    @Override
    public void onBackPressed() {
        showExitDialog(this);
    }
}