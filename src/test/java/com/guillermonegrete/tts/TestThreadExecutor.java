package com.guillermonegrete.tts;

public class TestThreadExecutor implements Executor {
    @Override
    public void execute(final AbstractInteractor interactor) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                interactor.run();
            }
        };
        runnable.run();
    }
}
