package com.cashsify.app;

import static com.cashsify.app.Utils.*;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import android.content.SharedPreferences;
import android.widget.TextView;


public class RegisterActivity extends AppCompatActivity {


    private EditText phoneEditText, emailEditText;
    private EditText passwordEditText, confirmPasswordEditText;
    private TextView termsText;
    private Button registerButton;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private Button btnMoveToLogin;
    private ImageView ivTogglePassword, ivTogglePassword2;
    private SharedPreferences sharedPref;
    public static String phone;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
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
        mAuth = FirebaseAuth.getInstance();
        termsText = findViewById(R.id.termsText);

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
            overridePendingTransition(R.anim.slide_in_right_register, R.anim.slide_out_left_register);
            progressBar.setVisibility(View.INVISIBLE);
            finish();
        };
        btnMoveToLogin.setOnClickListener(loginClickListener);

        registerButton.setOnClickListener(v -> {
            Utils.HideKeyboard(RegisterActivity.this, findViewById(android.R.id.content));
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
                        progressBar.setVisibility(View.INVISIBLE);
                        if (!phoneExists) {
                            createAccount(email, password);
                            registerButton.setEnabled(false);

                        } else {
                            phoneEditText.setError("Phone number already enrolled.");
                            phoneEditText.getText().clear();
                            showToast(RegisterActivity.this, "Phone number already enrolled.");
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

        String text = "By registering, you confirm that you accept Cashsify's Terms & Conditions and Privacy Policy.";

        SpannableString spannableString = new SpannableString(text);

        int termsStart = text.indexOf("Terms & Conditions");
        int termsEnd = termsStart + "Terms & Conditions".length();
        spannableString.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/cashsify/terms-and-conditions/"));
                startActivity(browserIntent);
            }
        }, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.white)), termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        int policyStart = text.indexOf("Privacy Policy");
        int policyEnd = policyStart + "Privacy Policy".length();
        spannableString.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/cashsify/privacy-policies/"));
                startActivity(browserIntent);
            }
        }, policyStart, policyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.white)), policyStart, policyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        termsText.setText(spannableString);
        termsText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private boolean validateForm(String phone, String email, String password, String confirmPassword) {

        if(phone.isEmpty() || !isValidPhoneNumber(phone) || !Patterns.PHONE.matcher(phone).matches()){
            phoneEditText.setError("Enter Valid Phone No");
            phoneEditText.requestFocus();
            progressBar.setVisibility(View.INVISIBLE);
            return false;
        }else if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter Valid Email address.");
            emailEditText.requestFocus();
            progressBar.setVisibility(View.INVISIBLE);
            return false;
        } else if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Required.");
            passwordEditText.requestFocus();
            progressBar.setVisibility(View.INVISIBLE);
            return false;
        } else if (password.length() < 6) {
            passwordEditText.setError("Password should be at least 6 characters.");
            passwordEditText.requestFocus();
            progressBar.setVisibility(View.INVISIBLE);
            return false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match.");
            confirmPasswordEditText.requestFocus();
            progressBar.setVisibility(View.INVISIBLE);
            return false;
        } else {
            progressBar.setVisibility(View.INVISIBLE);
            resetErrors();
            return true;
        }
    }

    private void createAccount(String email, String password) {
        progressBar.setVisibility(View.VISIBLE);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        progressBar.setVisibility(View.INVISIBLE);
                        saveUserData(phone,email,password);
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
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void handleError(Exception exception) {
        if (exception instanceof FirebaseAuthUserCollisionException) {
            showToast(this, "Email is already in use.");
        } else if (exception instanceof FirebaseAuthException) {
            showToast(this, "Something went wrong. Please try again.");
        } else {
            showToast(this, "Something went wrong. Please try again.");
        }
        progressBar.setVisibility(View.INVISIBLE);
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
                    progressBar.setVisibility(View.INVISIBLE);
                    showSnackBar(findViewById(android.R.id.content),"Registration successful","short");
                    clearInputFields();
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("phone", phone);
                    editor.apply();
                })
                .addOnFailureListener(e ->{
                    progressBar.setVisibility(View.INVISIBLE);
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

    public static boolean isValidPhoneNumber(String phoneNo) {
        if (phoneNo.startsWith("+91")) {
            phone = phoneNo.substring(3);
            return phoneNo.length() == 13
                    && (phoneNo.charAt(3) == '9' || phoneNo.charAt(3) == '8' || phoneNo.charAt(3) == '7' || phoneNo.charAt(3) == '6')
                    && TextUtils.isDigitsOnly(phoneNo.substring(1));
        } else if (phoneNo.startsWith("9") || phoneNo.startsWith("8") || phoneNo.startsWith("7") || phoneNo.startsWith("6")) {
            phone = phoneNo;
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

    @Override
    public void onBackPressed() {
        showExitDialog(this);
    }
}