package com.cashsify.app;

import static com.cashsify.app.Utils.*;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


public class LoginActivity extends AppCompatActivity {


    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private ProgressBar progressBar;
    private Button btnMvToRegister;
    private EditText et_PwdReset;
    private ImageView ivTogglePassword;
    private TextView forgetPassword;
    private TextView tv_header;
    private LinearLayout ll_Container, ll_Pwreset;
    private String ReEmail;
    private TextView BackToLogin;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_login);

        initUI();
        setOnClicks();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (ll_Pwreset.getVisibility() == View.VISIBLE) {
                    et_PwdReset.setText("");
                    resetError(et_PwdReset);
                    ll_Container.setVisibility(View.VISIBLE);
                    tv_header.setText("Ca$hsify\nLogin to earn money");
                    ll_Pwreset.setVisibility(View.GONE);
                } else {
                    finish();
                }
            }
        });

    }

    private void initUI() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.progressBar);
        btnMvToRegister = findViewById(R.id.btnSignUp);
        forgetPassword = findViewById(R.id.forgetpassword);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        et_PwdReset = findViewById(R.id.pwdResetEmail);
        BackToLogin = findViewById(R.id.backToLogin);
        ll_Container = findViewById(R.id.ll_container);
        ll_Pwreset = findViewById(R.id.ll_pwreset);
        tv_header = findViewById(R.id.header);
        mAuth = FirebaseAuth.getInstance();


        loginButton.setEnabled(true);
        if (!Utils.isNetworkConnected(LoginActivity.this)) {
            showSnackBar(findViewById(android.R.id.content), ERROR_NO_INTERNET, "short");
        } else {
            showSnackBar(findViewById(android.R.id.content), "You Can Login Now", "short");
        }
    }

    private void setOnClicks() {
        emailEditText.requestFocus();

        BackToLogin.setOnClickListener(v -> {
            HideKeyboard(LoginActivity.this, findViewById(android.R.id.content));
            showProgressBar(true);
            et_PwdReset.setText("");
            resetError(et_PwdReset);
            ll_Container.setVisibility(View.VISIBLE);
            tv_header.setText("Ca$hsify\nLogin to earn money");
            ll_Pwreset.setVisibility(View.GONE);
            showProgressBar(false);
        });

        forgetPassword.setOnClickListener(v -> {
            HideKeyboard(LoginActivity.this, findViewById(android.R.id.content));
            showProgressBar(true);
            ll_Container.setVisibility(View.GONE);
            tv_header.setText("Ca$hsify\nReset Password");
            ll_Pwreset.setVisibility(View.VISIBLE);
            showProgressBar(false);
        });

        btnMvToRegister.setOnClickListener(v -> {
            showProgressBar(true);
            HideKeyboard(LoginActivity.this, findViewById(android.R.id.content));
            intend(LoginActivity.this, RegisterActivity.class);
            overridePendingTransition(R.anim.slide_out_right_login, R.anim.slide_in_left_login);
            showProgressBar(false);
            finish();
        });

        loginButton.setOnClickListener(v -> handleLogin());


        ivTogglePassword.setOnClickListener(v -> {
            HideKeyboard(LoginActivity.this, findViewById(android.R.id.content));
            togglePasswordVisibility(passwordEditText, ivTogglePassword);
        });

        findViewById(R.id.btn_PwReset).setOnClickListener(v -> {
            HideKeyboard(LoginActivity.this, findViewById(android.R.id.content));
            showProgressBar(true);
            validateEmailAndSendReset();
        });

        findViewById(R.id.forgetmail).setOnClickListener(v -> {
            handlePhone();
        });

        findViewById(R.id.fab).setOnClickListener(v ->{
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

    }

    private void handleLogin() {
        {
            HideKeyboard(LoginActivity.this, findViewById(android.R.id.content));
            loginButton.setEnabled(false);
            showProgressBar(true);
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            if (isNetworkConnected(LoginActivity.this)) {
                loginButton.setEnabled(true);
                if (validateForm(email, password)) {
                    mAuth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    if (user != null) {
                                        user.reload().addOnCompleteListener(reloadTask -> {
                                            if (reloadTask.isSuccessful()) {

                                                if (user.isEmailVerified()) {
                                                    updateVerificationStatusInFirestore(email, password, true);
                                                    updateUI(user);
                                                } else {
                                                    updateVerificationStatusInFirestore(email, password, false);
                                                    updateUI(user);
//                                                    user.sendEmailVerification();
//                                                    showAlertDialog();
                                                }
                                            } else {
                                                showToast(LoginActivity.this, "Failed to refresh user data. Please try again.");
                                            }
                                            showProgressBar(false);
                                            loginButton.setEnabled(true);
                                        });
                                    } else {
                                        showToast(LoginActivity.this, "Failed to retrieve user data. Please try again.");
                                        showProgressBar(false);
                                        loginButton.setEnabled(true);
                                    }
                                } else {
                                    showToast(LoginActivity.this, "Email or Password is incorrect.");
                                    showProgressBar(false);
                                    loginButton.setEnabled(true);
                                }
                            });
                } else {
                    loginButton.setEnabled(true);
                    showProgressBar(false);
                }
            } else {
                Utils.showSnackBar(findViewById(android.R.id.content), "No internet, Ensure your internet is connected", "short");
                showProgressBar(false);
                loginButton.setEnabled(true);
            }
        }
    }

    private boolean validateForm(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            setError(emailEditText, ERROR_INVALID_EMAIL);
            return false;
        } else if (TextUtils.isEmpty(password)) {
            resetError(emailEditText);
            setError(passwordEditText, "Enter Valid password");
            return false;
        } else {
            resetError(emailEditText);
            resetError(passwordEditText);
            return true;
        }
    }

    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setTitle("Email Not Verified");
        builder.setMessage("Verify your Email now. You can't login without verification");

        builder.setPositiveButton("Continue", (dialogInterface, i) -> {
            redirectToEmailApp();
            showProgressBar(false);
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        loginButton.setEnabled(true);
    }


    private void updateUI(FirebaseUser user) {
        if (user != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("Users")
                    .whereEqualTo("Email", user.getEmail())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                            task.getResult().forEach(documentSnapshot -> {
                                String documentId = documentSnapshot.getId();
                                String UsName = documentSnapshot.getString("Name");
                                Utils.setDocumentId(documentId);
                                Utils.setUserEmail(user.getEmail());
                                Utils.setUserName(UsName);

                                db.collection("Users").document(documentId)
                                        .update("LastLogin", FieldValue.serverTimestamp())
                                        .addOnSuccessListener(aVoid -> {
                                            setTodayInFB(db, documentId);
                                            loginButton.setEnabled(true);
                                        }).addOnFailureListener(e -> {
                                            loginButton.setEnabled(true);
                                            showToast(LoginActivity.this, "Something went wrong. Please try again.");
                                        });
                            });
                        }
                    });
        } else {
            showProgressBar(false);
            emailEditText.setText("");
            passwordEditText.setText("");
            loginButton.setEnabled(true);
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
                        }else{
                            showProgressBar(false);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.d("MainActivity", "Failed to fetch user data: " + e.getMessage());
                    showProgressBar(false);
                });

    }

    private void updateFormattedDate(FirebaseFirestore db, String documentId, String formattedDate) {
        Map<String, Object> data = new HashMap<>();
        data.put("CurrentDate", formattedDate);
        db.collection("Earnings").document(documentId)
                .set(data, SetOptions.merge()).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        checkAndResetEarnings(this, documentId);
                    }else{
                        showProgressBar(false);
                    }
                })
                .addOnFailureListener(v -> showProgressBar(false));
    }
    public void checkAndResetEarnings(Context context, String documentId) {
        FirebaseFirestore.getInstance().collection("Earnings")
                .document(documentId)
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
                        showProgressBar(false);
                        Log.d("ResetEarningsWorker", "No need to reset earnings.");
                        Utils.intend(LoginActivity.this, MainActivity.class);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ResetEarningsWorker", "Error fetching ResetTime", e);
                    showProgressBar(false);
                });
    }
    public void resetEarnings(Context context) {
        showProgressBar(false);
        WorkManager.getInstance(context).enqueue(new OneTimeWorkRequest.Builder(ResetEarningsWorker.class).build());
        Utils.intend(LoginActivity.this, MainActivity.class);
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
    private void updateVerificationStatusInFirestore(String email, String passwd, boolean isVerified) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Users")
                .whereEqualTo("Email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        task.getResult().forEach(documentSnapshot -> {
                            String documentId = documentSnapshot.getId();
                            String currentPassword = documentSnapshot.getString("Password");

                            db.collection("Users").document(documentId)
                                    .update("isVerified", isVerified);
                            if (!currentPassword.equals(passwd)) {
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("Password", passwd);
                                updates.put("NewPasswd", new Date());

                                db.collection("Users").document(documentId)
                                        .update(updates);
                            }
                        });
                    } else {
                        showToast(LoginActivity.this, "Unable to update verification status. Please try again.");
                    }
                });
    }

    private void validateEmailAndSendReset() {
        showProgressBar(true);
        ReEmail = et_PwdReset.getText().toString().trim();

        if (TextUtils.isEmpty(ReEmail)) {
            showErrorAndResetProgress("Please enter your email.");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(ReEmail).matches()) {
            showErrorAndResetProgress("Invalid email format.");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users").whereEqualTo("Email", ReEmail).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                showProgressBar(false);
                showResetEmailConfirmationDialog(ReEmail);
            } else {
                showProgressBar(false);
                showErrorAndResetProgress("Email not found in records.");
            }
        });
    }

    private void sendResetPasswordEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showToast(LoginActivity.this, "Reset link sent to " + email);
                        new AlertDialog.Builder(this)
                                .setTitle("Email Sent")
                                .setMessage("A password reset link has been sent to " + email + ". Do you want to set up a new password?")
                                .setPositiveButton("Yes", (dialog, which) -> {
                                    redirectToEmailApp();
                                })
                                .setNegativeButton("No", (dialog, which) -> switchToLoginView(true))
                                .show();
                    } else {
                        showErrorAndResetProgress("Unable to send reset link. Please try again later.");
                    }
                });
    }

    private void switchToLoginView(boolean toLogin) {
        ll_Container.setVisibility(toLogin ? View.VISIBLE : View.GONE);
        ll_Pwreset.setVisibility(toLogin ? View.GONE : View.VISIBLE);
        if (toLogin) {
            et_PwdReset.setText("");
        }
        showProgressBar(false);
    }


    public void redirectToEmailApp() {
        Intent emailIntent = new Intent(Intent.ACTION_MAIN);
        emailIntent.addCategory(Intent.CATEGORY_APP_EMAIL);
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(Intent.createChooser(emailIntent, "Open email app"));
    }

    private void showErrorAndResetProgress(String message) {
        setError(et_PwdReset, message);
        showToast(LoginActivity.this, message);
        showProgressBar(false);
    }


    private void showResetEmailConfirmationDialog(String ReEmail) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");
        builder.setMessage("Do you want to send a password reset link to " + ReEmail + "?");

        builder.setPositiveButton("Yes", (dialog, which) -> sendResetPasswordEmail(ReEmail));
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void handlePhone() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Phone Number to Verify");
        builder.setIcon(android.R.drawable.ic_menu_help);
        final EditText input = new EditText(this);
        input.setHint("Phone Number");
        builder.setView(input);

        builder.setPositiveButton("Verify", (dialog1, which) -> {
            String phoneNumber = input.getText().toString().trim();
            if (RegisterActivity.isValidPhoneNumber(phoneNumber)) {
                retrieveEmailFromFirestore(RegisterActivity.phone);
                dialog1.dismiss();
            } else {
                showToast(LoginActivity.this, "Please enter valid phone number.");
            }
        });

        builder.setNegativeButton("Cancel", (dialog2, which) -> {
            dialog2.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void retrieveEmailFromFirestore(String phoneNumber) {
        showProgressBar(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users").whereEqualTo("Phone", phoneNumber).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            for (DocumentSnapshot documentSnapshot : task.getResult()) {
                                String email = documentSnapshot.getString("Email");
                                String password = documentSnapshot.getString("Password");
                                if (email != null) {
                                    showProgressBar(false);
                                    showEmailToUser(email, phoneNumber, password);
                                } else {
                                    showProgressBar(false);
                                    showToast(LoginActivity.this, "No email found for this phone number.");

                                }
                            }
                        } else {
                            showProgressBar(false);
                            showToast(LoginActivity.this, "No user found with this phone number.");
                        }
                    } else {
                        showProgressBar(false);
                        showToast(LoginActivity.this, "Unable to retrieve email. Please try again later.");
                    }
                });
    }


    private void showEmailToUser(String email, String phoneNo, String password) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Email", email);
        clipboard.setPrimaryClip(clip);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Your Email is :");
        builder.setMessage(email);
        builder.setPositiveButton("Copy", (dialog, which) -> {
            showToast(LoginActivity.this, "Email copied to clipboard.");
        });
        builder.setNegativeButton("Close", (dialog2, which) -> dialog2.dismiss());
        builder.setIcon(android.R.drawable.ic_dialog_email);
        builder.show();
    }

    private void showProgressBar(boolean isVisible) {
        progressBar.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
        emailEditText.setEnabled(!isVisible);
        passwordEditText.setEnabled(!isVisible);
        loginButton.setEnabled(!isVisible);
        loginButton.setText(isVisible ? "Logging in..." : "LOGIN");
    }
    @Override
    public void onBackPressed() {
        showExitDialog(this);
    }

}