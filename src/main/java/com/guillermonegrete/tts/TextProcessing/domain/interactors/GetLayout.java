package com.guillermonegrete.tts.TextProcessing.domain.interactors;

import com.guillermonegrete.tts.AbstractInteractor;
import com.guillermonegrete.tts.Executor;
import com.guillermonegrete.tts.MainThread;
import com.guillermonegrete.tts.data.source.WordDataSource;
import com.guillermonegrete.tts.data.source.WordRepository;
import com.guillermonegrete.tts.db.Words;


public class GetLayout extends AbstractInteractor implements GetLayoutInteractor {

    private GetLayoutInteractor.Callback mCallback;
    private WordRepository mRepository;
    private String mText;

    public GetLayout(Executor threadExecutor, MainThread mainThread, Callback callback, WordRepository repository, String text){
        super(threadExecutor, mainThread);
        mCallback = callback;
        mText = text;
        mRepository = repository;
    }

    @Override
    public void run() {
        System.out.print("Get layout implementation run method");
        String[] splittedText = mText.split(" ");
        if(splittedText.length > 1){
            System.out.print("Get sentence layout");
            // Get translation, wait for callback
        }else{
            // Search in database
            System.out.print("Request layouts");
            mRepository.getWordLanguageInfo(mText, new WordDataSource.GetWordCallback() {
                @Override
                public void onWordLoaded(Words word) {
                    System.out.print("Word retrieved");
                    System.out.print(word.word);
                }

                @Override
                public void onDataNotAvailable() {
                    // Not in database nor microsoft translation
                }
            });
        }

    }
}
