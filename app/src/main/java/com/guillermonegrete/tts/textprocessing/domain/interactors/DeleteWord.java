package com.guillermonegrete.tts.textprocessing.domain.interactors;

import com.guillermonegrete.tts.AbstractInteractor;
import com.guillermonegrete.tts.Executor;
import com.guillermonegrete.tts.MainThread;
import com.guillermonegrete.tts.data.source.WordRepositorySource;

public class DeleteWord extends AbstractInteractor implements DeleteWordInteractor {

    private String word;
    private WordRepositorySource repository;

    public DeleteWord(Executor executor, MainThread mainThread, WordRepositorySource repository, String word) {
        super(executor, mainThread);
        this.repository = repository;
        this.word = word;
    }

    @Override
    public void run() {
        repository.deleteWord(word);
    }
}
