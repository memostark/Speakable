package com.guillermonegrete.tts.threading;

import android.os.Handler;
import android.os.Looper;

import com.guillermonegrete.tts.MainThread;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MainThreadImpl implements MainThread {

    private Handler handler;

    @Inject
    MainThreadImpl(){handler = new Handler(Looper.getMainLooper());}

    @Override
    public void post(Runnable runnable) {
        handler.post(runnable);
    }
}
