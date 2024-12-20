package com.cashsify.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.cashsify.app.databinding.FragmentProfileBinding;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

public class ProfileFragment extends Fragment {

    public final String TAG = "ProfileFragment";
    private FragmentProfileBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        initUI();
        setUpActions();
        return root;
    }

    private void initUI() {
        Utils.init(requireActivity());
        binding.tvUserEmail.setText(Utils.getUserEmail());
        binding.tvUserPhone.setText(Utils.getDocumentId());
        binding.tvUserName.setText(Utils.getUserName());
    }

    private void setUpActions() {
        binding.tvEditName.setOnClickListener(v -> changeUserDetails(binding.tvEditName));
        binding.ivSettings.setOnClickListener(v ->  showPasswordUpdateDialog());
        binding.tvEditEmail.setOnClickListener(v -> showEmailUpdateDialog());
        binding.ivDeleteUser.setOnClickListener(v -> showDeleteConfirmationDialog());
        binding.ivLogout.setOnClickListener(v ->{
            FirebaseAuth.getInstance().signOut();
            Utils.intend(requireActivity(), LoginActivity.class);
        });
            binding.tvEditPhone.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setIcon(R.drawable.icon_phone);
                builder.setTitle("Update Phone Number");

            final EditText input = new EditText(requireContext());
            input.setInputType(InputType.TYPE_CLASS_PHONE);
            input.setHint("Enter new phone number");
            builder.setView(input);

            builder.setPositiveButton("Update", (dialog, which) -> {
                String newPhone = input.getText().toString().trim();

                if (!newPhone.isEmpty() && RegisterActivity.isValidPhoneNumber(newPhone) && !newPhone.equals(Utils.getDocumentId())) {
                    replaceDocumentReferences(newPhone);
                } else {
                    Utils.showToast(requireContext(), "Please enter a valid phone number.");
                }
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            builder.show();
        });

    }
    private void replaceDocumentReferences(String newPhoneNumber) {
        String oldPhoneNumber = Utils.getDocumentId();
        if (oldPhoneNumber == null) {
            Utils.showToast(requireActivity(), "Something went wrong. Please try again later.");
            return;
        }
        showProgressBar(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();
        DocumentReference oldUserRef = db.collection("Users").document(oldPhoneNumber);
        DocumentReference newUserRef = db.collection("Users").document(newPhoneNumber);

        DocumentReference oldEarningsRef = db.collection("Earnings").document(oldPhoneNumber);
        DocumentReference newEarningsRef = db.collection("Earnings").document(newPhoneNumber);

        oldUserRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                batch.set(newUserRef, documentSnapshot.getData());
                batch.delete(oldUserRef);
                oldEarningsRef.get().addOnSuccessListener(earningsSnapshot -> {
                    if (earningsSnapshot.exists()) {
                        batch.set(newEarningsRef, earningsSnapshot.getData());
                        batch.delete(oldEarningsRef);
                        batch.commit()
                                .addOnSuccessListener(aVoid -> {
                                    Utils.setDocumentId(newPhoneNumber);
                                    showProgressBar(false);
                                    Utils.showToast(requireActivity(), "Phone Number Updated Successfully");
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {

                                        new AlertDialog.Builder(requireActivity())
                                                .setTitle("Log In Required")
                                                .setMessage("Your phone number has been updated. Please log in again to continue.")
                                                .setCancelable(false)
                                                .setPositiveButton("OK", (dialog, which) -> {
                                                    Intent intent = new Intent(requireActivity(), LoginActivity.class);
                                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                    startActivity(intent);
                                                    requireActivity().finish();
                                                })
                                                .show();
                                    }, 500);
                                })
                                .addOnFailureListener(e -> showProgressBar(false));
                    } else {
                        showProgressBar(false);
                    }
                }).addOnFailureListener(e -> showProgressBar(false));
            } else {
                showProgressBar(false);
            }
        }).addOnFailureListener(e -> showProgressBar(false));
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete User")
                .setIcon(android.R.drawable.ic_menu_delete)
                .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> deleteUser())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())  // No action on "No"
                .show();
    }
    private void deleteUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.delete()
                    .addOnSuccessListener(aVoid -> {
                        deleteUserFromFirestore();
                    })
                    .addOnFailureListener(e -> {
                        Utils.showToast(requireContext(), "Failed to delete user. Please try again later.");
                    });
        } else {
            Utils.showToast(requireContext(), "Something went wrong. Please try again later.");
        }
    }
    private void deleteUserFromFirestore() {
        String documentId = Utils.getDocumentId();
        if (documentId != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("Users").document(documentId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        db.collection("Earnings").document(documentId)
                                .delete()
                                .addOnSuccessListener(aVoid1 -> {
                                    Utils.showToast(requireActivity(), "User deleted successfully.");
                                    Utils.intend(requireActivity(), LoginActivity.class);
                                })
                                .addOnFailureListener(e -> {
                                    Utils.showToast(requireActivity(), "Something went wrong. Please try again later.");
                                });
                    })
             .addOnFailureListener(e -> {
                Utils.showToast(requireActivity(), "Something went wrong. Please try again later.");
            });
        } else {
            Utils.showToast(requireActivity(), "Something went wrong. Please try again later.");
        }
    }

    private void showEmailUpdateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Update Email");
        builder.setIcon(android.R.drawable.ic_dialog_email);
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText emailInput = new EditText(requireContext());
        emailInput.setHint("Enter new email");
        emailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(emailInput);

        final EditText passwordInput = new EditText(requireContext());
        passwordInput.setHint("Enter your password");
        passwordInput.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT);
        layout.addView(passwordInput);

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newEmail = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (TextUtils.isEmpty(newEmail) || TextUtils.isEmpty(password)) {
                Utils.showToast(requireContext(), "Fields cannot be empty");
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                Utils.showToast(requireContext(), "Invalid email format");
                return;
            }
            showProgressBar(true);
            reauthenticateAndVerifyEmail(newEmail, password);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void reauthenticateAndVerifyEmail(String newEmail, String password) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            showProgressBar(false);
                            Utils.showSnackBar(requireView(), "Verification email sent to " + newEmail + "Verify by link to Continue login with new Email", "long");
                            updateFieldInFirestore("Email", newEmail, binding.tvUserEmail);
                            signOutAndPromptLogin(newEmail);
                        } else {
                            showProgressBar(false);
                            Utils.showToast(requireContext(), "Failed to update email. Please try again.");
                        }
                    });
                } else {
                    showProgressBar(false);
                    Utils.showToast(requireContext(), "Authentication failed. Please try again with the correct password.");
                }
            });
        }else{
            showProgressBar(false);
        }
    }
    private void signOutAndPromptLogin(String newmail) {
        FirebaseAuth.getInstance().signOut();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Email Updated")
                .setMessage(newmail+" has been successfully updated. Please login again to continue.")
                .setPositiveButton("Login", (dialog, which) -> {
                    Utils.intend(requireContext(), LoginActivity.class);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setClassName("com.google.android.gm", "com.google.android.gm.ConversationListActivityGmail");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();

                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Utils.intend(requireContext(), LoginActivity.class);
                    Utils.showToast(requireContext(), "Verify email by click link sent "+ newmail);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setClassName("com.google.android.gm", "com.google.android.gm.ConversationListActivityGmail");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .show();
    }


    private void showPasswordUpdateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Update Password");
        builder.setIcon(android.R.drawable.ic_lock_idle_lock);
        // LinearLayout for multiple input fields
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // Input for current password
        final EditText currentPasswordInput = new EditText(requireContext());
        currentPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        currentPasswordInput.setHint("Enter current password");
        layout.addView(currentPasswordInput);

        // Input for new password
        final EditText newPasswordInput = new EditText(requireContext());
        newPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        newPasswordInput.setHint("Enter new password");
        layout.addView(newPasswordInput);

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String currentPassword = currentPasswordInput.getText().toString().trim();
            String newPassword = newPasswordInput.getText().toString().trim();

            if (TextUtils.isEmpty(currentPassword) || TextUtils.isEmpty(newPassword)) {
                Utils.showToast(requireContext(), "Passwords cannot be empty.");
                return;
            }

            if (newPassword.length() < 6) {
                Utils.showToast(requireContext(), "New password must be at least 6 characters.");
                return;
            }

            reauthenticateAndUpdatePassword(currentPassword, newPassword);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
    private void reauthenticateAndUpdatePassword(String currentPassword, String newPassword) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Utils.showToast(requireContext(), "User not authenticated. Please sign in again.");
            return;
        }
        showProgressBar(true);
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

        user.reauthenticate(credential)
                .addOnCompleteListener(reAuthTask -> {
                    if (reAuthTask.isSuccessful()) {
                        updatePassword(user, newPassword);
                    } else {
                        showProgressBar(false);
                        Utils.showToast(requireContext(), "Incorrect Password. Please try again with the correct password.");
                    }
                });
    }
    private void updatePassword(FirebaseUser user, String newPassword) {
        user.updatePassword(newPassword)
                .addOnCompleteListener(passwordUpdateTask -> {
                    if (passwordUpdateTask.isSuccessful()) {
                         updateFieldInFirestore("Password", newPassword, null);
                    } else {
                        showProgressBar(false);
                        Utils.showToast(requireContext(), "Failed to update password. Please try again.");
                    }
                });
    }

    private void changeUserDetails(TextView tv) {
        String title = "";
        String hint = "";
        String currentValue = "";
       if (tv.getId() == R.id.tvEditName) {
            title = "Update Name";
            hint = "Enter new name";
            currentValue = binding.tvUserName.getText().toString();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(title);

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(hint);
        builder.setView(input);
        builder.setIcon(R.drawable.icon_person);
        builder.setPositiveButton("Update", (dialog, which) -> {
            String newValue = input.getText().toString().trim();

            if (TextUtils.isEmpty(newValue)) {
                Utils.showToast(requireContext(), "Value Cannot be empty");
                return;
            }
                updateFieldInFirestore("Name", newValue, binding.tvUserName);

        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void updateFieldInFirestore(String field, String newValue, TextView textViewToUpdate) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore db = Utils.getFirestoreInstance();

            db.collection("Users").document(Utils.getDocumentId())
                    .update(field, newValue)
                    .addOnSuccessListener(aVoid -> {
                        if(textViewToUpdate != null) {
                            textViewToUpdate.setText(newValue);
                        }
                        if(isAdded()) {
                            Utils.showToast(requireContext(), field.substring(0, 1).toUpperCase() + field.substring(1) + " updated successfully");
                            showProgressBar(false);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()){
                            Utils.showToast(requireContext(), "Update failed: " + e.getMessage());
                            showProgressBar(false);
                        }
                    });
        } else {
            if (isAdded()) {
                Utils.showToast(requireContext(), "User not authenticated. Please try again later.");
                showProgressBar(false);
            }
        }
    }

    private void showProgressBar(boolean isVisible) {
        binding.progressBar.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}