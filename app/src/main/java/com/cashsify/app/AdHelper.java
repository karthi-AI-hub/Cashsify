package com.cashsify.app;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.FullScreenContentCallback;

public class AdHelper {

    private static final String TAG = "AdLogs";
    private RewardedAd rewardedAd;
    private final Context context;

    public AdHelper(Context context) {
        this.context = context;
        loadAd();
    }

    public void loadAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(context, "ca-app-pub-3940256099942544/5224354917", adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                rewardedAd = ad;
                Log.d(TAG, "Ad successfully loaded.");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                rewardedAd = null;
                Log.e(TAG, "Failed to load ad: " + loadAdError.getMessage());
            }
        });
    }

    public void showAd(Activity activity, OnUserEarnedRewardListener rewardCallback, Runnable onAdDismissedCallback) {
        if (rewardedAd != null) {
            setFullScreenContentCallback(onAdDismissedCallback);
            rewardedAd.show(activity, rewardCallback);
        } else {
            Log.e(TAG, "Ad not loaded yet.");
        }
    }

    public void setFullScreenContentCallback(Runnable onAdDismissedCallback) {
        if (rewardedAd != null) {
            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad dismissed. Loading the next ad.");

                    loadAd();

                    if (onAdDismissedCallback != null) {
                        onAdDismissedCallback.run();
                    }
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                    Log.e(TAG, "Ad failed to show: " + adError.getMessage());
                    Utils.showToast(context, "Somethings went wrong. Try again later.");
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad is showing.");
                    rewardedAd = null;
                }
            });
        }
    }


}
