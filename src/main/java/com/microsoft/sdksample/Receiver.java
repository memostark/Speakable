package com.microsoft.sdksample;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Receiver extends BroadcastReceiver {
    NotificationManager mNotificationManager;
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, ScreenTextService.class);
        context.stopService(service);
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(MainActivity.notificationId);
    }
}
