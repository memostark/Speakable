package com.guillermonegrete.tts;

import java.util.concurrent.ExecutorService;

public abstract class AbstractPresenter {

    protected ExecutorService executorService;
    protected MainThread mMainThread;

    public AbstractPresenter(ExecutorService executor, MainThread mainThread){
        executorService = executor;
        mMainThread = mainThread;
    }
}
