package com.guillermonegrete.tts;

import com.guillermonegrete.tts.AbstractInteractor;

/**
 * This executor is responsible for running interactors on background threads.
 */
public interface Executor {

    /**
     * This method should call the interactor's run method and thus start the interactor. This should be called
     * on a background thread as interactors might do lengthy operations.
     *
     * @param interactor The interactor to run.
     */
    void execute(final AbstractInteractor interactor);
}
