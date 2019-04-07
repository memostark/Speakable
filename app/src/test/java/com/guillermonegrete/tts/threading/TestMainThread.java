package com.guillermonegrete.tts.threading;

import com.guillermonegrete.tts.MainThread;

public class TestMainThread implements MainThread {

    @Override
    public void post(Runnable runnable) {
        runnable.run();
    }
}
