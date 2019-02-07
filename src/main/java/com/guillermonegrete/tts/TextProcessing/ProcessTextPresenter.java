package com.guillermonegrete.tts.TextProcessing;


import com.guillermonegrete.tts.AbstractPresenter;
import com.guillermonegrete.tts.CustomTTS.CustomTTS;
import com.guillermonegrete.tts.CustomTTS.interactors.PlayTTS;
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
    private CustomTTS customTTS;

    private boolean insideLocalDatabase;
    private Words foundWord;

    public ProcessTextPresenter(Executor executor, MainThread mainThread, ProcessTextContract.View view,
                                WordRepository repository, DictionaryRepository dictRepository, CustomTTS customTTS){
        super(executor, mainThread);
        mView = view;
        mRepository = repository;
        dictionaryRepository = dictRepository;
        this.customTTS = customTTS;

        insideLocalDatabase = false;
        mView.setPresenter(this);
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
                System.out.println(String.format("Got layout: %s", layoutType.name()));
                System.out.println(word.word);
                System.out.println(word.definition);
                System.out.println(word.lang);
                Boolean isInitialized = customTTS.getInitialized() && customTTS.getLanguage().equals(word.lang);
                System.out.println(String.format("Is initialized %s", String.valueOf(isInitialized)));
                foundWord = word;
                if(!isInitialized) customTTS.initializeTTS(word.lang);
                switch (layoutType){
                    case WORD_TRANSLATION:
                        mView.setTranslationLayout(word);
                        break;
                    case SAVED_WORD:
                        insideLocalDatabase = true;
                        mView.setSavedWordLayout(word);
                        break;
                    case SENTENCE_TRANSLATION:
                        mView.setSentenceLayout(word);
                        break;
                }
            }

            @Override
            public void onDictionaryLayoutDetermined(Words word, List<WikiItem> items) {
                System.out.println("Got dictionary layout:");
                System.out.println(word.word);
                System.out.println(word.definition);
                System.out.println(word.lang);
                Boolean isInitialized = customTTS.getInitialized() && customTTS.getLanguage().equals(word.lang);
                System.out.println(String.format("Is initialized %s", String.valueOf(isInitialized)));
                foundWord = word;
                if(!isInitialized) customTTS.initializeTTS(word.lang);
                mView.setWiktionaryLayout(items);
            }
        }, mRepository, dictionaryRepository, text);

        interactor.execute();

    }

    @Override
    public void getExternalLinks() {

    }

    @Override
    public void onClickBookmark() {
        if(insideLocalDatabase) mView.showDeleteDialog(foundWord.word);
        else mView.showSaveDialog(foundWord);

    }

    @Override
    public void onClickDeleteWord() {

    }

    @Override
    public void onClickReproduce(String text) {
        PlayTTS interactor = new PlayTTS(mExecutor, mMainThread, customTTS, text);
        interactor.execute();
    }

    @Override
    public void onClickEdit() {

    }

    @Override
    public void start() {

    }

    @Override
    public void destroy() {
    }
}
