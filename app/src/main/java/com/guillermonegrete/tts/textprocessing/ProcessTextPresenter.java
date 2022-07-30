package com.guillermonegrete.tts.textprocessing;


import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.guillermonegrete.tts.AbstractPresenter;
import com.guillermonegrete.tts.customtts.CustomTTS;
import com.guillermonegrete.tts.customtts.interactors.PlayTTS;
import com.guillermonegrete.tts.MainThread;
import com.guillermonegrete.tts.data.source.ExternalLinksDataSource;
import com.guillermonegrete.tts.data.source.WordRepositorySource;
import com.guillermonegrete.tts.main.SettingsFragment;
import com.guillermonegrete.tts.textprocessing.domain.interactors.DeleteWord;
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetDictionaryEntry;
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetDictionaryEntryInteractor;
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetExternalLink;
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetLayout;
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetLayoutInteractor;
import com.guillermonegrete.tts.textprocessing.domain.model.GetLayoutResult;
import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem;
import com.guillermonegrete.tts.data.source.DictionaryRepository;
import com.guillermonegrete.tts.db.Words;

import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation;
import com.guillermonegrete.tts.utils.EspressoIdlingResource;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ProcessTextPresenter extends AbstractPresenter implements ProcessTextContract.Presenter{

    private ProcessTextContract.View mView;
    private final WordRepositorySource mRepository;
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

    private final MutableLiveData<GetLayoutResult> layoutResult = new MutableLiveData<>();

    @Inject
    ProcessTextPresenter(
            ExecutorService executor,
            MainThread mainThread,
            WordRepositorySource repository,
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
        insideLocalDatabase = true;
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

        EspressoIdlingResource.increment();

        GetLayout interactor = new GetLayout(executorService, mMainThread, new GetLayoutInteractor.Callback() {
            @Override
            public void onLayoutDetermined(Words word, ProcessTextLayoutType layoutType) {
                foundWord = word;
                checkTTSInitialization();

                switch (layoutType){
                    case WORD_TRANSLATION:
                        getExternalLinks(word.lang);
                        break;
                    case SAVED_WORD:
                        insideLocalDatabase = true;
                        getExternalLinks(word.lang);
                        break;
                }

                layoutResult.setValue(new GetLayoutResult.WordSuccess(layoutType, word));

                EspressoIdlingResource.decrement();
            }

            @Override
            public void onDictionaryLayoutDetermined(Words word, List<WikiItem> items) {
                foundWord = word;
                checkTTSInitialization();
                getExternalLinks(word.lang);
                layoutResult.setValue(new GetLayoutResult.DictionarySuccess(word, items));

                EspressoIdlingResource.decrement();
            }

            @Override
            public void onTranslationError(String message) {
                hasTranslation = false;
                layoutResult.setValue(new GetLayoutResult.Error(new Exception(message)));

                EspressoIdlingResource.decrement();
            }
        }, mRepository, dictionaryRepository, text, languageFrom, languageTo);

        interactor.execute();

    }

    @Override
    public void getDictionaryEntry(final Words word) {
        foundWord = word;
        hasTranslation = true;
        viewIsActive = true;

        EspressoIdlingResource.increment();

        checkTTSInitialization();

        GetDictionaryEntry interactor = new GetDictionaryEntry(executorService, mMainThread, dictionaryRepository, word.word, new GetDictionaryEntryInteractor.Callback(){

            @Override
            public void onEntryNotAvailable() {
                getExternalLinks(word.lang);
                mView.setSavedWordLayout(word);
                if(!isAvailable) mView.showLanguageNotAvailable();

                EspressoIdlingResource.decrement();
            }

            @Override
            public void onDictionaryLayoutDetermined(@NotNull List<WikiItem> items) {
                getExternalLinks(word.lang);
                mView.setDictWithSaveWordLayout(word, items);
                if(!isAvailable) mView.showLanguageNotAvailable();

                EspressoIdlingResource.decrement();
            }


        });
        interactor.execute();
    }

    private void getExternalLinks(String language) {
        GetExternalLink link_interactor = new GetExternalLink(executorService, mMainThread, linksRepository);
        link_interactor.invoke(language, links -> {
                    mView.setExternalDictionary(links);
                    if(!hasTranslation) mView.setTranslationErrorMessage();
        });
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
        DeleteWord interactor = new DeleteWord(executorService, mMainThread, mRepository, word);
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
            PlayTTS interactor = new PlayTTS(executorService, mMainThread, customTTS, ttsListener, text);
            interactor.execute();
        }
    }

    @Override
    public void onClickEdit() {

    }

    @Override
    public void onLanguageSpinnerChange(String languageFrom, final String languageTo) {
        EspressoIdlingResource.increment();
        hasTranslation = true;
        getTranslationInteractor.invoke(foundWord.word, new GetLangAndTranslation.Callback() {
            @Override
            public void onTranslationAndLanguage(@NotNull Words word) {
                customTTS.initializeTTS(word.lang, ttsListener);
                mView.updateTranslation(word);
                EspressoIdlingResource.decrement();
            }

            @Override
            public void onDataNotAvailable() {
                hasTranslation = false;
                mView.setTranslationErrorMessage();
                EspressoIdlingResource.decrement();
            }
        }, languageFrom, languageTo);

        GetExternalLink linkInteractor = new GetExternalLink(executorService, mMainThread, linksRepository);

        linkInteractor.invoke(languageFrom, links -> mView.updateExternalLinks(links));
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
        System.out.println("Shutting down service");
        executorService.shutdown();
        executorService.shutdownNow();
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
        EspressoIdlingResource.increment();
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
            mMainThread.post(() -> {
                mView.showLanguageNotAvailable();
                EspressoIdlingResource.decrement();
            });
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
            mMainThread.post(() -> {
                mView.showPlayIcon();
                EspressoIdlingResource.decrement();
            });
        }

        @Override
        public void onError() {
            isPlaying = false;
            mMainThread.post(() -> {
                mView.showErrorPlayingAudio();
                EspressoIdlingResource.decrement();
            });
        }
    };

    public LiveData<GetLayoutResult> getLayoutResult() {
        return layoutResult;
    }
}
