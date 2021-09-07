package com.guillermonegrete.tts.textprocessing.domain.interactors;

import com.guillermonegrete.tts.AbstractInteractor;
import com.guillermonegrete.tts.MainThread;
import com.guillermonegrete.tts.data.source.WordRepositorySource;

import java.util.concurrent.ExecutorService;

public class DeleteWord extends AbstractInteractor implements DeleteWordInteractor {

    private final String word;
    private final WordRepositorySource repository;

    public DeleteWord(ExecutorService executor, MainThread mainThread, WordRepositorySource repository, String word) {
        super(executor, mainThread);
        this.repository = repository;
        this.word = word;
    }

    @Override
    public void run() {
        repository.deleteWord(word);
    }
}
