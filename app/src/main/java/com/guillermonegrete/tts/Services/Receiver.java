package com.guillermonegrete.tts.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.guillermonegrete.tts.Services.ScreenTextService;

public class Receiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, ScreenTextService.class);
        context.stopService(service);
    }
}
