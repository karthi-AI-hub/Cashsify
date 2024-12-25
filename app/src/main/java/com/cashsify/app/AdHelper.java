package com.cashsify.app;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.FullScreenContentCallback;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class AdHelper {

    private static final String TAG = "AdLogs";
    private static final int MAX_PRELOADED_ADS = 2;
    private final Context context;
    private final List<String> adUnitIds = new ArrayList<>();
    private final List<Queue<RewardedAd>> adQueues = new ArrayList<>();


    public AdHelper(Context context) {
        this.context = context;
        adUnitIds.add("ca-app-pub-7086602185948470/1291652554");
        adUnitIds.add("ca-app-pub-7086602185948470/8767182851");
        adUnitIds.add("ca-app-pub-7086602185948470/6802235192");

        for (int i = 0; i < adUnitIds.size(); i++) {
            adQueues.add(new LinkedList<>());
        }

        preloadAds();
    }

    private void preloadAds() {
        for (int i = 0; i < adUnitIds.size(); i++) {
            for (int j = 0; j < MAX_PRELOADED_ADS; j++) {
                loadAd(adUnitIds.get(i), adQueues.get(i));
            }
        }
    }
    private void loadAd(String adUnitId, Queue<RewardedAd> queue) {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(context, adUnitId, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                if (queue.size() < MAX_PRELOADED_ADS) {
                    queue.offer(ad);
                    Log.d(TAG, "Ad loaded for unit: " + adUnitId + ". Queue size: " + queue.size());
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "Failed to load ad for unit: " + adUnitId + ". Error: " + loadAdError.getMessage());
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Log.d(TAG, "Retrying to load ad for unit: " + adUnitId);
                    loadAd(adUnitId, queue);
                }, 10000);
            }
        });
    }

    public void showAd(Activity activity, OnUserEarnedRewardListener rewardCallback, Runnable onAdDismissedCallback) {
        boolean adShown = false;

        for (int i = 0; i < adQueues.size(); i++) {
            Queue<RewardedAd> queue = adQueues.get(i);
            RewardedAd ad = queue.poll();
            if (ad != null) {
                Log.d(TAG, "Showing ad from unit: " + adUnitIds.get(i));
                setFullScreenContentCallback(ad, queue, onAdDismissedCallback);
                ad.show(activity, rewardCallback);
                adShown = true;
                break;
            } else {
                Log.d(TAG, "No ads available in queue for unit: " + adUnitIds.get(i));
            }
        }

        if (!adShown) {
            Log.e(TAG, "No preloaded ads available in any queue.");
            Utils.showToast(activity, "No ads available. Please try again later.");
        }
    }

    private void setFullScreenContentCallback(RewardedAd ad, Queue<RewardedAd> queue, Runnable onAdDismissedCallback) {
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed. Loading a new ad.");
                int queueIndex = adQueues.indexOf(queue);
                if (queueIndex != -1) {
                    loadAd(adUnitIds.get(queueIndex), queue);
                } else {
                    Log.e(TAG, "Queue mismatch. Unable to reload ad.");
                }
                if (onAdDismissedCallback != null) {
                    onAdDismissedCallback.run();
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                Log.e(TAG, "Ad failed to show: " + adError.getMessage());
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad is showing.");
            }
        });
    }
}
