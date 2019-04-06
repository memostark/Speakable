package com.guillermonegrete.tts;

public abstract class AbstractPresenter {

    protected Executor mExecutor;
    protected MainThread mMainThread;

    public AbstractPresenter(Executor executor, MainThread mainThread){
        mExecutor = executor;
        mMainThread = mainThread;
    }
}
