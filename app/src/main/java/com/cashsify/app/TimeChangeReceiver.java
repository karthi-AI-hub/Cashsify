package com.cashsify.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TimeChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_TIME_CHANGED.equals(intent.getAction()) ||
                Intent.ACTION_DATE_CHANGED.equals(intent.getAction())) {
            scheduleResetEarningsWorker(context);
        }
    }

    private void scheduleResetEarningsWorker(Context context) {
        MainActivity activity = new MainActivity();
        activity.scheduleResetEarningsWorker(context);
    }
}

