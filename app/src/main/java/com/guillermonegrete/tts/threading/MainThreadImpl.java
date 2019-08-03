package com.guillermonegrete.tts.threading;

import android.os.Handler;
import android.os.Looper;

import com.guillermonegrete.tts.MainThread;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MainThreadImpl implements MainThread {

    private static MainThread mainThread;

    private Handler handler;

    @Inject
    public MainThreadImpl(){handler = new Handler(Looper.getMainLooper());}

    @Override
    public void post(Runnable runnable) {
        handler.post(runnable);
    }

    public static MainThread getInstance(){
        if (mainThread == null){
            mainThread = new MainThreadImpl();
        }

        return mainThread;
    }
}
