package com.cashsify.app;

import static com.cashsify.app.Utils.*;
import android.content.Intent;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private ProgressBar progressBar;
    private Button btnMvToLogin;
    private EditText et_PwdReset;
    private ImageView ivTogglePassword;
    private TextView forgetPassword;
    private LinearLayout ll_Container, ll_Pwreset;
    private String ReEmail;
    private TextView BackToLogin;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
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
        btnMvToLogin = findViewById(R.id.btnSignUp);
        forgetPassword = findViewById(R.id.forgetpassword);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        et_PwdReset = findViewById(R.id.pwdResetEmail);
        BackToLogin = findViewById(R.id.backToLogin);
        ll_Container = findViewById(R.id.ll_container);
        ll_Pwreset = findViewById(R.id.ll_pwreset);
        mAuth = FirebaseAuth.getInstance();


        loginButton.setEnabled(true);
        if (!Utils.isNetworkConnected(LoginActivity.this)) {
            showSnackBar(findViewById(android.R.id.content), ERROR_NO_INTERNET, "short");
        }else{
            showSnackBar(findViewById(android.R.id.content), "You Can Login Now", "short");
        }
    }

    private void setOnClicks() {
        emailEditText.requestFocus();

        BackToLogin.setOnClickListener(v -> {
            HideKeyboard(LoginActivity.this, findViewById(android.R.id.content));
            showProgressBar(true);
            ll_Container.setVisibility(View.VISIBLE);
            ll_Pwreset.setVisibility(View.GONE);
            showProgressBar(false);
        });

        forgetPassword.setOnClickListener(v -> {
            HideKeyboard(LoginActivity.this, findViewById(android.R.id.content));
            showProgressBar(true);
            ll_Container.setVisibility(View.GONE);
            ll_Pwreset.setVisibility(View.VISIBLE);
            showProgressBar(false);
        });

        btnMvToLogin.setOnClickListener(v -> {
            showProgressBar(true);
            HideKeyboard(LoginActivity.this, findViewById(android.R.id.content));
            intend(LoginActivity.this, RegisterActivity.class);
            overridePendingTransition(0, 0);
            showProgressBar(false);
            finish();
        });

        loginButton.setOnClickListener(v -> {
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
                                                            updateVerificationStatusInFirestore(email, true);
                                                            updateUI(user);
                                                        } else {
                                                            user.sendEmailVerification();
                                                            showAlertDialog();
                                                        }
                                                    } else {
                                                        Log.e(TAG, "User reload failed: " + reloadTask.getException().getMessage());
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
                                            Log.w(TAG, "SignInWithEmailAndPassword failed: " + task.getException().getMessage());
                                            showToast(LoginActivity.this, "Authentication failed. Check your credentials.");
                                            showProgressBar(false);
                                            loginButton.setEnabled(true);
                                        }
                                    });
                        } else {
                            loginButton.setEnabled(true);
                            showProgressBar(false);
                        }
                    }else{
                        Utils.showSnackBar(findViewById(android.R.id.content),"No internet, Ensure your internet is connected", "short");
                        showProgressBar(false);
                        loginButton.setEnabled(true);
                    }
                });


        ivTogglePassword.setOnClickListener(v -> {
            HideKeyboard(LoginActivity.this, findViewById(android.R.id.content));
            togglePasswordVisibility(passwordEditText, ivTogglePassword);
        });

        findViewById(R.id.signUpPrompt).setOnClickListener(v -> {
            HideKeyboard(LoginActivity.this, findViewById(android.R.id.content));
            intend(LoginActivity.this, RegisterActivity.class);
            overridePendingTransition(0, 0);
        });

        findViewById(R.id.btn_PwReset).setOnClickListener(v -> {
            HideKeyboard(LoginActivity.this, findViewById(android.R.id.content));
            showProgressBar(true);
            validateEmailAndSendReset();
        });

    }

    private boolean validateForm(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            setError(emailEditText, ERROR_INVALID_EMAIL);
            return false;
        } else if (TextUtils.isEmpty(password)) {
            resetError(emailEditText);
            setError(passwordEditText, "Enter Vslid password");
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
            Utils.intend(LoginActivity.this, MainActivity.class);
            finish();
            showProgressBar(false);
            loginButton.setEnabled(true);
        } else {
            showProgressBar(false);
            emailEditText.setText("");
            passwordEditText.setText("");
            loginButton.setEnabled(true);
        }
    }

    private void updateVerificationStatusInFirestore(String email, boolean isVerified) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Users")
                .whereEqualTo("Email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        task.getResult().forEach(documentSnapshot -> {
                            String documentId = documentSnapshot.getId(); // Get the document ID

                            // Update the "isVerified" field
                            db.collection("Users").document(documentId)
                                    .update("isVerified", isVerified)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Verification status updated successfully.");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error updating verification status: " + e.getMessage());
                                    });
                        });
                    } else {
                        Log.e(TAG, "No matching user found or error occurred: " + task.getException());
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
                                .setMessage("A password reset link has been sent to " + email +". Do you want to set up a new password?")
                                .setPositiveButton("Yes", (dialog, which) -> {
                                    redirectToEmailApp();
                                })
                                .setNegativeButton("No", (dialog, which) -> switchToLoginView(true))
                                .show();
                    } else {
                        showErrorAndResetProgress("Unable to send reset link. Please try again later.");
                        Log.e(TAG, "Error sending reset email: " + task.getException());
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


    private void showProgressBar(boolean isVisible) {
        progressBar.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        emailEditText.setEnabled(!isVisible);
        passwordEditText.setEnabled(!isVisible);
        loginButton.setEnabled(!isVisible);
        loginButton.setText(isVisible ? "Logging in..." : "LOGIN");
    }

}