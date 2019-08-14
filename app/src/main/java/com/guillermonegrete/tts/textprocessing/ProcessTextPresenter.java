package com.guillermonegrete.tts.textprocessing;


import com.guillermonegrete.tts.AbstractPresenter;
import com.guillermonegrete.tts.customtts.CustomTTS;
import com.guillermonegrete.tts.customtts.interactors.PlayTTS;
import com.guillermonegrete.tts.Executor;
import com.guillermonegrete.tts.MainThread;
import com.guillermonegrete.tts.data.source.ExternalLinksDataSource;
import com.guillermonegrete.tts.textprocessing.domain.interactors.DeleteWord;
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetDictionaryEntry;
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetDictionaryEntryInteractor;
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetExternalLink;
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetLayout;
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetLayoutInteractor;
import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem;
import com.guillermonegrete.tts.data.source.DictionaryRepository;
import com.guillermonegrete.tts.data.source.WordRepository;
import com.guillermonegrete.tts.db.ExternalLink;
import com.guillermonegrete.tts.db.Words;

import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class ProcessTextPresenter extends AbstractPresenter implements ProcessTextContract.Presenter{

    private ProcessTextContract.View mView;
    private WordRepository mRepository;
    private DictionaryRepository dictionaryRepository;
    private ExternalLinksDataSource linksRepository;
    private CustomTTS customTTS;

    private boolean insideLocalDatabase;
    private Words foundWord;

    private boolean isPlaying;
    private boolean isAvailable;

    @Inject
    ProcessTextPresenter(
            Executor executor,
            MainThread mainThread,
            WordRepository repository,
            DictionaryRepository dictRepository,
            ExternalLinksDataSource linksRepository,
            CustomTTS customTTS){
        super(executor, mainThread);
        mRepository = repository;
        dictionaryRepository = dictRepository;
        this.linksRepository = linksRepository;
        this.customTTS = customTTS;

        insideLocalDatabase = false;
        isPlaying = false;
        isAvailable = true;
    }




    @Override
    public void addNewWord() {

    }


    @Override
    public void editWord() {

    }

    @Override
    public void start(Words word) {
        getDictionaryEntry(word);
    }

    @Override
    public void startWithService(String selectedText, String languageFrom, String languageTo) {
        mView.startService();
        getLayout(selectedText, languageFrom, languageTo);
    }

    @Override
    public void getLayout(String text, String languageFrom, String languageTo) {
        GetLayout interactor = new GetLayout(mExecutor, mMainThread, new GetLayoutInteractor.Callback() {
            @Override
            public void onLayoutDetermined(Words word, ProcessTextLayoutType layoutType) {
                foundWord = word;
                checkTTSInitialization();

                switch (layoutType){
                    case WORD_TRANSLATION:
                        getExternalLinks(word.lang);
                        mView.setTranslationLayout(word);
                        break;
                    case SAVED_WORD:
                        insideLocalDatabase = true;
                        getExternalLinks(word.lang);
                        mView.setSavedWordLayout(word);
                        break;
                    case SENTENCE_TRANSLATION:
                        mView.setSentenceLayout(word);
                        break;
                }
            }

            @Override
            public void onDictionaryLayoutDetermined(Words word, List<WikiItem> items) {
                foundWord = word;
                checkTTSInitialization();
                getExternalLinks(word.lang);
                mView.setWiktionaryLayout(word, items);
            }
        }, mRepository, dictionaryRepository, text, languageFrom, languageTo);

        interactor.execute();

    }

    @Override
    public void getDictionaryEntry(final Words word) {
        foundWord = word;
        checkTTSInitialization();

        GetDictionaryEntry interactor = new GetDictionaryEntry(mExecutor, mMainThread, dictionaryRepository, word.word, new GetDictionaryEntryInteractor.Callback(){

            @Override
            public void onEntryNotAvailable() {
                getExternalLinks(word.lang);
                mView.setSavedWordLayout(word);
                if(!isAvailable) mView.showLanguageNotAvailable();
            }

            @Override
            public void onDictionaryLayoutDetermined(@NotNull List<WikiItem> items) {
                getExternalLinks(word.lang);
                mView.setDictWithSaveWordLayout(word, items);
                if(!isAvailable) mView.showLanguageNotAvailable();
            }


        });
        interactor.execute();
    }


    @Override
    public void getExternalLinks(String language) {
        GetExternalLink link_interactor = new GetExternalLink(mExecutor, mMainThread, links -> {
            mView.setExternalDictionary(links);
            for(ExternalLink link : links){
                System.out.print("Site name: ");
                System.out.println(link.siteName);
            }

        }, linksRepository, language);

        link_interactor.execute();
    }

    @Override
    public void onClickBookmark() {
        if(insideLocalDatabase) mView.showDeleteDialog(foundWord.word);
        else mView.showSaveDialog(foundWord);
    }

    @Override
    public void onClickSaveWord(Words word) {
        insideLocalDatabase = true;
    }

    @Override
    public void onClickDeleteWord(String word) {
        DeleteWord interactor = new DeleteWord(mExecutor, mMainThread, mRepository, word);
        interactor.execute();
        mView.showWordDeleted();
    }

    @Override
    public void onClickReproduce(String text) {
        if(isPlaying){
            customTTS.stop();
            isPlaying = false;
            mView.showPlayIcon();
        }else if(isAvailable){
            mView.showLoadingTTS();
            PlayTTS interactor = new PlayTTS(mExecutor, mMainThread, customTTS, ttsListener, text);
            interactor.execute();
        }
    }

    @Override
    public void onClickEdit() {

    }

    @Override
    public void onLanguageSpinnerChange(String languageFrom, final String languageTo) {
        GetLangAndTranslation interactor = new GetLangAndTranslation(mExecutor, mMainThread, mRepository, foundWord.word, languageFrom, languageTo, new GetLangAndTranslation.Callback() {
            @Override
            public void onTranslationAndLanguage(@NotNull Words word) {
                boolean isInitialized = customTTS.getInitialized() && customTTS.getLanguage().equals(word.lang);
                if(!isInitialized) customTTS.initializeTTS(word.lang);
                mView.updateTranslation(word.definition);
            }

            @Override
            public void onDataNotAvailable() {

            }
        });
        interactor.execute();
    }

    @Override
    public void setView(ProcessTextContract.View view) {
        mView = view;
    }

    @Override
    public void start() {

    }

    @Override
    public void pause() {
        stopPlaying();
    }

    @Override
    public void stop() {
        stopPlaying();
    }

    @Override
    public void destroy() {
        stopPlaying();
    }

    private void stopPlaying(){
        if(isPlaying) {
            customTTS.stop();
            isPlaying = false;
        }
    }

    private void checkTTSInitialization(){
        isAvailable = true;
        String lang = foundWord.lang;
        boolean isInitialized = customTTS.getInitialized() && customTTS.getLanguage().equals(lang);
        if(!isInitialized) customTTS.initializeTTS(lang);
    }

    private CustomTTS.Listener ttsListener = new CustomTTS.Listener() {
        @Override
        public void onLanguageUnavailable() {
            isPlaying = false;
            isAvailable = false;
            mMainThread.post(() -> mView.showLanguageNotAvailable());
        }

        @Override
        public void onSpeakStart() {
            isPlaying = true;
            // TODO move main thread code to interactor
            mMainThread.post(() -> mView.showStopIcon());
        }

        @Override
        public void onSpeakDone() {
            isPlaying = false;
            mMainThread.post(() -> mView.showPlayIcon());
        }
    };
}
