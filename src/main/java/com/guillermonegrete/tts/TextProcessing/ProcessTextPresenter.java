package com.guillermonegrete.tts.TextProcessing;


import com.guillermonegrete.tts.AbstractPresenter;
import com.guillermonegrete.tts.Executor;
import com.guillermonegrete.tts.MainThread;
import com.guillermonegrete.tts.TextProcessing.domain.interactors.GetLayout;
import com.guillermonegrete.tts.TextProcessing.domain.interactors.GetLayoutInteractor;
import com.guillermonegrete.tts.TextProcessing.domain.model.WikiItem;
import com.guillermonegrete.tts.data.source.DictionaryRepository;
import com.guillermonegrete.tts.data.source.WordRepository;
import com.guillermonegrete.tts.db.Words;

import java.util.List;

public class ProcessTextPresenter extends AbstractPresenter implements ProcessTextContract.Presenter{

    private ProcessTextContract.View mView;
    private WordRepository mRepository;
    private DictionaryRepository dictionaryRepository;

    public ProcessTextPresenter(Executor executor, MainThread mainThread, ProcessTextContract.View view, WordRepository repository, DictionaryRepository dictRepository){
        super(executor, mainThread);
        mView = view;
        mRepository = repository;
        dictionaryRepository = dictRepository;

        mView.setPresenter(this);
    }

    @Override
    public void reproduceTTS() {

    }

    @Override
    public void addNewWord() {

    }

    @Override
    public void deleteWord() {

    }

    @Override
    public void editWord() {

    }

    @Override
    public void getLayout(String text) {
        GetLayout interactor = new GetLayout(mExecutor, mMainThread, new GetLayoutInteractor.Callback() {
            @Override
            public void onLayoutDetermined(Words word, ProcessTextLayoutType layoutType) {
                System.out.print("Got layout");
                switch (layoutType){
                    case WORD_TRANSLATION:
                        mView.setTranslationLayout(word);
                        break;
                    case SAVED_WORD:
                        mView.setSavedWordLayout(word);
                        break;
                    case SENTENCE_TRANSLATION:
                        mView.setSentenceLayout(word);
                        break;
                    default:
                        mView.onClickEdit();
                        break;
                }
            }

            @Override
            public void onDictionaryLayoutDetermined(List<WikiItem> items) {
                mView.setWiktionaryLayout(items);
            }
        }, mRepository, dictionaryRepository, text);

        interactor.execute();

    }

    @Override
    public void getExternalLinks() {

    }

    @Override
    public void start() {

    }
}
