package com.guillermonegrete.tts;

import java.util.concurrent.ExecutorService;

public abstract class AbstractPresenter {

    protected Executor mExecutor;
    protected ExecutorService executorService;
    protected MainThread mMainThread;

    public AbstractPresenter(Executor executor, MainThread mainThread){
        mExecutor = executor;
        mMainThread = mainThread;
    }

    public AbstractPresenter(ExecutorService executor, MainThread mainThread){
        executorService = executor;
        mMainThread = mainThread;
    }
}
