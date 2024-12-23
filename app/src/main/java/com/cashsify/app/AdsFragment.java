package com.cashsify.app;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.cashsify.app.databinding.FragmentAdsBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Random;

public class AdsFragment extends Fragment {

    private static final String TAG = "AdLogs";
    private @NonNull FragmentAdsBinding binding;


    private final FirebaseFirestore db = Utils.getFirestoreInstance();
    private final String documentId = Utils.getDocumentId();
    private LinearLayout ll_ads;
    private LinearLayout ll_completed;
    private ListenerRegistration listenerRegistration;
    private int completedTask = 0;
    private String generatedCaptcha;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAdsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

         ll_ads = root.findViewById(R.id.ll_adsContainer);
         ll_completed = root.findViewById(R.id.ll_taskCompleted);
        AdHelper adHelper = ((MainActivity) requireActivity()).getAdHelper();

        listenerRegistration = db.collection("Earnings").document(documentId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening to Firestore updates", e);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                           completedTask = documentSnapshot.getLong("completedTasks") != null ? documentSnapshot.getLong("completedTasks").intValue() : 0;

                        boolean hasCompletedTasks = completedTask >= 20;
                        ll_ads.setVisibility(hasCompletedTasks ? View.GONE : View.VISIBLE);
                        ll_completed.setVisibility(hasCompletedTasks ? View.VISIBLE : View.GONE);

                    } else {
                        Log.e(TAG, "Document does not exist in Firestore.");
                    }
                });


        binding.ivAdIcon.setOnClickListener(v -> {
                    adHelper.showAd(requireActivity(), rewardItem -> {
                                Log.d(TAG, "User earned reward: " + rewardItem.getAmount());
                                showCaptchaDialog();
                            },
                            null
                    );
                });
        binding.btnWatchAd.setOnClickListener(v -> {
            adHelper.showAd(requireActivity(), rewardItem -> {
                        Log.d(TAG, "User earned reward: " + rewardItem.getAmount());
                        showCaptchaDialog();
                    },
                    null
            );
        });
        return root;
    }

    private void showCaptchaDialog() {
        generatedCaptcha = generateRandomCaptcha();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_captcha, null);
        builder.setCancelable(false);
        builder.setView(dialogView);

        EditText captchaInput = dialogView.findViewById(R.id.et_captcha_input);
        TextView captchaDisplay = dialogView.findViewById(R.id.tv_captcha);
        Button submitButton = dialogView.findViewById(R.id.btn_submit);
        ImageView refreshButton = dialogView.findViewById(R.id.iv_refresh_captcha);

        captchaDisplay.setText(generatedCaptcha);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        refreshButton.setOnClickListener(v -> {
            generatedCaptcha = generateRandomCaptcha();
            captchaDisplay.setText(generatedCaptcha);
        });

        submitButton.setOnClickListener(v -> {
            String userInput = captchaInput.getText().toString().trim();
            if (generatedCaptcha.equals(userInput)) {
                dialog.dismiss();
                updateDatabaseWithReward();
            } else {
                dialog.dismiss();
                Utils.showToast(requireContext(), "INVALID CAPTCHA");
            }
        });

        dialog.show();
    }

    private String generateRandomCaptcha() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder captcha = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            captcha.append(chars.charAt(random.nextInt(chars.length())));
        }
        return captcha.toString();
    }

    private void updateDatabaseWithReward() {
        CurrencyUtils databaseHelper = new CurrencyUtils();
        boolean isUpdated = databaseHelper.incrementRewardForUser(Utils.getDocumentId(), 1);

        if (isUpdated) {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.nav_home);
        } else {
            Utils.showToast(requireActivity(), "Failed to update reward.");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
        binding = null;
    }
}