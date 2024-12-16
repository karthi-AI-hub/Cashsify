package com.cashsify.app;

import static com.cashsify.app.Utils.*;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.sql.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import android.content.SharedPreferences;


public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private EditText phoneEditText, emailEditText;
    private EditText passwordEditText, confirmPasswordEditText;
    private Button registerButton;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private Button btnMoveToLogin;
    private ImageView ivTogglePassword, ivTogglePassword2;
    private TextView tvPrompt;
    private SharedPreferences sharedPref;
    private String phone;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_register);
        InitUI();
        setOnClicks();
    }

    private void InitUI() {
        phoneEditText = findViewById(R.id.phoneEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.registerButton);
        confirmPasswordEditText = findViewById(R.id.confirmpasswordEditText);
        progressBar = findViewById(R.id.regProgressBar);
        btnMoveToLogin = findViewById(R.id.btnLogin);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivTogglePassword2 = findViewById(R.id.ivTogglePassword2);
        tvPrompt = findViewById(R.id.loginPrompt);
        mAuth = FirebaseAuth.getInstance();

        if (!Utils.isNetworkConnected(RegisterActivity.this)) {
            showSnackBar(findViewById(android.R.id.content), ERROR_NO_INTERNET, "short");
        }else{
            showSnackBar(findViewById(android.R.id.content),"You Can Register Now", "short");
        }
    }

    private void setOnClicks() {
        phoneEditText.requestFocus();
        sharedPref = getSharedPreferences("UserPref", Context.MODE_PRIVATE);

        View.OnClickListener loginClickListener = v -> {
            HideKeyboard(RegisterActivity.this, findViewById(android.R.id.content));
            progressBar.setVisibility(View.VISIBLE);
            intend(RegisterActivity.this, LoginActivity.class);
            overridePendingTransition(0, 0);
            progressBar.setVisibility(View.GONE);
            finish();
        };
        btnMoveToLogin.setOnClickListener(loginClickListener);
        tvPrompt.setOnClickListener(loginClickListener);

        registerButton.setOnClickListener(v -> {
            resetErrors();
            if (Utils.isNetworkConnected(RegisterActivity.this)) {
                registerButton.setEnabled(true);
                registerButton.setText("Registering ...");
                progressBar.setVisibility(View.VISIBLE);
                phone = phoneEditText.getText().toString().trim();
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();
                String confirmPassword = confirmPasswordEditText.getText().toString().trim();

                if (validateForm(phone, email, password, confirmPassword)) {
                    checkIfExistingPhone(phone, phoneExists -> {
                        progressBar.setVisibility(View.GONE);
                        if (!phoneExists) {
                            createAccount(email, password);
                            registerButton.setEnabled(false);

                        } else {
                            phoneEditText.setError("Phone number already enrolled.");
                            showToast(RegisterActivity.this, "Phone number already enrolled.");
                            phoneEditText.setBackgroundResource(R.drawable.edit_text_red);
                            registerButton.setEnabled(true);
                        }
                    });
                }
            } else {
                showSnackBar(findViewById(android.R.id.content), ERROR_NO_INTERNET, "short");
                registerButton.setEnabled(false);
            }
            new Handler(Looper.getMainLooper()).postDelayed(() -> registerButton.setEnabled(true), 5000);
            registerButton.setText("REGISTER");
        });

        ivTogglePassword.setOnClickListener(v -> {
            HideKeyboard(RegisterActivity.this, findViewById(android.R.id.content));
            Utils.togglePasswordVisibility(passwordEditText, ivTogglePassword);
        });

        ivTogglePassword2.setOnClickListener(v -> {
            HideKeyboard(RegisterActivity.this, findViewById(android.R.id.content));
            Utils.togglePasswordVisibility(confirmPasswordEditText, ivTogglePassword2);
        });
    }

    private boolean validateForm(String phone, String email, String password, String confirmPassword) {

        if(phone.isEmpty() || !isValidPhoneNumber(phone) || !Patterns.PHONE.matcher(phone).matches()){
            phoneEditText.setError("Enter Valid Phone No");
            phoneEditText.setBackgroundResource(R.drawable.edit_text_red);
            phoneEditText.requestFocus();
            progressBar.setVisibility(View.GONE);
            return false;
        }else if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter Valid Email address.");
            emailEditText.setBackgroundResource(R.drawable.edit_text_red);
            emailEditText.requestFocus();
            progressBar.setVisibility(View.GONE);
            return false;
        } else if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Required.");
            passwordEditText.setBackgroundResource(R.drawable.edit_text_red);
            passwordEditText.requestFocus();
            progressBar.setVisibility(View.GONE);
            return false;
        } else if (password.length() < 6) {
            passwordEditText.setError("Password should be at least 6 characters.");
            passwordEditText.setBackgroundResource(R.drawable.edit_text_red);
            passwordEditText.requestFocus();
            progressBar.setVisibility(View.GONE);
            return false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match.");
            passwordEditText.setBackgroundResource(R.drawable.edit_text_red);
            confirmPasswordEditText.requestFocus();
            confirmPasswordEditText.setBackgroundResource(R.drawable.edit_text_red);
            progressBar.setVisibility(View.GONE);
            return false;
        } else {
            progressBar.setVisibility(View.GONE);
            resetErrors();
            return true;
        }
    }

    private void createAccount(String email, String password) {
        progressBar.setVisibility(View.VISIBLE);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        progressBar.setVisibility(View.GONE);
                        saveUserData(phoneEditText.getText().toString(),email,password);
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification();
                        }
                        updateUI(user);
                    } else {
                        handleError(task.getException());
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Utils.intend(RegisterActivity.this, LoginActivity.class);
            finish();
        }else{
        clearInputFields();
        }
        progressBar.setVisibility(View.GONE);
    }

    private void handleError(Exception exception) {
        if (exception instanceof FirebaseAuthUserCollisionException) {
            showToast(this, "Email is already in use.");
        } else if (exception instanceof FirebaseAuthException) {
            Log.e(TAG, "Authentication failed: ", exception);
            showToast(this, "Authentication failed.");
        } else {
            Log.e(TAG, "Authentication failed: ", exception);
            showToast(this, "Authentication failed.");
        }
        progressBar.setVisibility(View.GONE);
        updateUI(null);
    }

    private void saveUserData(String phone, String email, String password) {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> userData = new HashMap<>();
        userData.put("Phone", phone);
        userData.put("Email", email);
        userData.put("Password", password);

        db.collection("Users").document(phone.toUpperCase())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    showSnackBar(findViewById(android.R.id.content),"Registration successful","short");
                    clearInputFields();
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("phone", phone);
                    editor.apply();
                })
                .addOnFailureListener(e ->{
                    progressBar.setVisibility(View.GONE);
                showSnackBar(findViewById(android.R.id.content),"Registration Failed","short");
                });
    }

    private void checkIfExistingPhone(String phone, OnPhoneCheckListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference phoneDocRef = db.collection("Users").document(phone.toUpperCase());

        phoneDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Log.d("CheckPhone", "Phone number already enrolled.");
                    listener.onCheckComplete(true);
                } else {
                    Log.d("CheckPhone", "Phone number is not enrolled.");
                    listener.onCheckComplete(false);
                }
            } else {
                Log.e("CheckPhone", "Failed to validate phone number", task.getException());
                showToast(this, "Failed to Validate Phone Number");
                listener.onCheckComplete(false);
            }
        });
    }


    public interface OnPhoneCheckListener {
        void onCheckComplete(boolean phoneExists);
    }

    private boolean isValidPhoneNumber(String phoneNo) {
        if (phoneNo.startsWith("+91")) {
            phone = phoneNo.substring(3);
            return phoneNo.length() == 13
                    && (phoneNo.charAt(3) == '9' || phoneNo.charAt(3) == '8' || phoneNo.charAt(3) == '7' || phoneNo.charAt(3) == '6')
                    && TextUtils.isDigitsOnly(phoneNo.substring(1));
        } else if (phoneNo.startsWith("9") || phoneNo.startsWith("8") || phoneNo.startsWith("7") || phoneNo.startsWith("6")) {
            return phoneNo.length() == 10 && TextUtils.isDigitsOnly(phoneNo);
        } else {
            return false;
        }
    }

    private void clearInputFields() {
        emailEditText.getText().clear();
        passwordEditText.getText().clear();
        confirmPasswordEditText.getText().clear();
        emailEditText.clearFocus();
        passwordEditText.clearFocus();
        confirmPasswordEditText.clearFocus();
    }


    private void resetErrors(){
        resetError(phoneEditText);
        resetError(emailEditText);
        resetError(passwordEditText);
        resetError(confirmPasswordEditText);
    }
}