package com.guillermonegrete.tts.TextProcessing.domain.interactors;

import com.guillermonegrete.tts.AbstractInteractor;
import com.guillermonegrete.tts.Executor;
import com.guillermonegrete.tts.MainThread;

import java.io.Console;

public class GetLayout extends AbstractInteractor implements GetLayoutInteractor {

    private GetLayoutInteractor.Callback mCallback;

    public GetLayout(Executor threadExecutor, MainThread mainThread, Callback callback){
        super(threadExecutor, mainThread);
        mCallback = callback;
    }

    @Override
    public void run() {
        System.out.print("Get layout implementation run method");

    }
}
