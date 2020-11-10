package com.guillermonegrete.tts.textprocessing;


import android.content.SharedPreferences;

import com.guillermonegrete.tts.AbstractPresenter;
import com.guillermonegrete.tts.customtts.CustomTTS;
import com.guillermonegrete.tts.customtts.interactors.PlayTTS;
import com.guillermonegrete.tts.Executor;
import com.guillermonegrete.tts.MainThread;
import com.guillermonegrete.tts.data.source.ExternalLinksDataSource;
import com.guillermonegrete.tts.main.SettingsFragment;
import com.guillermonegrete.tts.textprocessing.domain.interactors.DeleteWord;
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetDictionaryEntry;
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetDictionaryEntryInteractor;
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetExternalLink;
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetLayout;
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetLayoutInteractor;
import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem;
import com.guillermonegrete.tts.data.source.DictionaryRepository;
import com.guillermonegrete.tts.data.source.WordRepository;
import com.guillermonegrete.tts.db.Words;

import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation;
import com.guillermonegrete.tts.utils.EspressoIdlingResource;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.List;

public class ProcessTextPresenter extends AbstractPresenter implements ProcessTextContract.Presenter{

    private ProcessTextContract.View mView;
    private final WordRepository mRepository;
    private final DictionaryRepository dictionaryRepository;
    private final ExternalLinksDataSource linksRepository;
    private final SharedPreferences sharedPreferences;
    private final CustomTTS customTTS;
    private final GetLangAndTranslation getTranslationInteractor;

    private boolean insideLocalDatabase;
    private Words foundWord;

    private boolean isLoading;
    private boolean isPlaying;
    private boolean isAvailable;
    private boolean hasTranslation;

    private boolean viewIsActive = false;

    @Inject
    ProcessTextPresenter(
            Executor executor,
            MainThread mainThread,
            WordRepository repository,
            DictionaryRepository dictRepository,
            ExternalLinksDataSource linksRepository,
            SharedPreferences sharedPreferences,
            CustomTTS customTTS,
            GetLangAndTranslation getTranslationInteractor){
        super(executor, mainThread);
        mRepository = repository;
        dictionaryRepository = dictRepository;
        this.linksRepository = linksRepository;
        this.sharedPreferences = sharedPreferences;
        this.customTTS = customTTS;
        this.getTranslationInteractor = getTranslationInteractor;

        insideLocalDatabase = false;
        isLoading = false;
        isPlaying = false;
        isAvailable = true;

        hasTranslation = false;
    }




    @Override
    public void addNewWord() {

    }


    @Override
    public void editWord() {

    }

    @Override
    public void start(Words word) {
        insideLocalDatabase = false;
        getDictionaryEntry(word);
    }

    @Override
    public void startWithService(String selectedText, String languageFrom, String languageTo) {
        insideLocalDatabase = false;
        mView.startService();
        getLayout(selectedText, languageFrom, languageTo);
    }

    @Override
    public void getLayout(String text, String languageFrom, String languageTo) {
        hasTranslation = true;
        viewIsActive = true;

        EspressoIdlingResource.INSTANCE.increment();

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

                EspressoIdlingResource.INSTANCE.decrement();
            }

            @Override
            public void onDictionaryLayoutDetermined(Words word, List<WikiItem> items) {
                foundWord = word;
                checkTTSInitialization();
                getExternalLinks(word.lang);
                mView.setWiktionaryLayout(word, items);

                EspressoIdlingResource.INSTANCE.decrement();
            }

            @Override
            public void onTranslationError(String message) {
                hasTranslation = false;
                mView.showTranslationError(message);

                EspressoIdlingResource.INSTANCE.decrement();
            }
        }, mRepository, dictionaryRepository, text, languageFrom, languageTo);

        interactor.execute();

    }

    @Override
    public void getDictionaryEntry(final Words word) {
        foundWord = word;
        hasTranslation = true;
        viewIsActive = true;

        EspressoIdlingResource.INSTANCE.increment();

        checkTTSInitialization();

        GetDictionaryEntry interactor = new GetDictionaryEntry(mExecutor, mMainThread, dictionaryRepository, word.word, new GetDictionaryEntryInteractor.Callback(){

            @Override
            public void onEntryNotAvailable() {
                getExternalLinks(word.lang);
                mView.setSavedWordLayout(word);
                if(!isAvailable) mView.showLanguageNotAvailable();

                EspressoIdlingResource.INSTANCE.decrement();
            }

            @Override
            public void onDictionaryLayoutDetermined(@NotNull List<WikiItem> items) {
                getExternalLinks(word.lang);
                mView.setDictWithSaveWordLayout(word, items);
                if(!isAvailable) mView.showLanguageNotAvailable();

                EspressoIdlingResource.INSTANCE.decrement();
            }


        });
        interactor.execute();
    }

    private void getExternalLinks(String language) {
        GetExternalLink link_interactor = new GetExternalLink(
                mExecutor,
                mMainThread,
                links -> {
                    mView.setExternalDictionary(links);
                    if(!hasTranslation) mView.setTranslationErrorMessage();
                },
                linksRepository,
                language
        );

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
            isLoading = true;
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
        hasTranslation = true;
        getTranslationInteractor.invoke(foundWord.word, new GetLangAndTranslation.Callback() {
            @Override
            public void onTranslationAndLanguage(@NotNull Words word) {
                customTTS.initializeTTS(word.lang, ttsListener);
                mView.updateTranslation(word);
            }

            @Override
            public void onDataNotAvailable() {
                hasTranslation = false;
                mView.setTranslationErrorMessage();
            }
        }, languageFrom, languageTo);

        GetExternalLink linkInteractor = new GetExternalLink(
                mExecutor,
                mMainThread,
                links -> mView.updateExternalLinks(links),
                linksRepository,
                languageFrom
        );

        linkInteractor.execute();
    }


    /**
     * This kind of a hack,
     * This is called when the layout has been finally created to show status of the play button icon
     *
     */
    @Override
    public void onPlayIconSet() {
        if(isLoading){
            mView.showLoadingTTS();
            return;
        }

        if(isAvailable){
            if(isPlaying){
                mView.showStopIcon();
            }else {
                mView.showPlayIcon();
            }
        }else {
            mView.showLanguageNotAvailable();
        }
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
        onViewInactive();
    }

    @Override
    public void stop() {
        onViewInactive();
    }

    @Override
    public void destroy() {
        onViewInactive();
        customTTS.removeListener(ttsListener);
    }

    private void onViewInactive(){
        viewIsActive = false;

        if(isPlaying) {
            customTTS.stop();
            isPlaying = false;
        }
    }

    private void checkTTSInitialization(){
        isAvailable = true;
        String lang = foundWord.lang;
        customTTS.initializeTTS(lang, ttsListener);
    }

    private Boolean getAutoTTSPreference(){
        return sharedPreferences.getBoolean(SettingsFragment.PREF_AUTO_TEST_SWITCH, true);
    }

    private final CustomTTS.Listener ttsListener = new CustomTTS.Listener() {

        @Override
        public void onEngineReady() {
            isLoading = false;
            boolean autoPlay = getAutoTTSPreference();

            if(autoPlay && viewIsActive) {
                onClickReproduce(foundWord.word);
            }
            else {
                mView.showPlayIcon();
            }
        }

        @Override
        public void onLanguageUnavailable() {
            isLoading = false;
            isPlaying = false;
            isAvailable = false;
            mMainThread.post(() -> mView.showLanguageNotAvailable());
        }

        @Override
        public void onSpeakStart() {
            isLoading = false;
            isPlaying = true;

            mMainThread.post(() -> mView.showStopIcon());
        }

        @Override
        public void onSpeakDone() {
            isPlaying = false;
            mMainThread.post(() -> mView.showPlayIcon());
        }

        @Override
        public void onError() {
            isPlaying = false;
            mMainThread.post(() -> mView.showErrorPlayingAudio());
        }
    };
}
