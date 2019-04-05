package com.guillermonegrete.tts.Main;

import com.guillermonegrete.tts.AbstractPresenter;
import com.guillermonegrete.tts.CustomTTS.CustomTTS;
import com.guillermonegrete.tts.CustomTTS.interactors.PlayTTS;
import com.guillermonegrete.tts.Executor;
import com.guillermonegrete.tts.MainThread;
import com.guillermonegrete.tts.data.source.WordRepository;
import com.guillermonegrete.tts.data.source.WordRepositorySource;
import com.guillermonegrete.tts.db.Words;

public class MainTTSPresenter extends AbstractPresenter implements MainTTSContract.Presenter {

    private CustomTTS tts;
    private MainTTSContract.View view;
    private WordRepository wordRepository;

    public MainTTSPresenter(Executor executor, MainThread mainThread, MainTTSContract.View view, WordRepository wordRepository, CustomTTS tts) {
        super(executor, mainThread);
        this.view = view;
        this.tts = tts;
        this.wordRepository = wordRepository;
    }

    @Override
    public void onClickReproduce(final String text) {
        // TODO this request should be done in a background thread

        wordRepository.getLanguageAndTranslation(text, new WordRepositorySource.GetTranslationCallback() {
            @Override
            public void onTranslationAndLanguage(Words word) {
                String language = word.lang;
                boolean isInitialized = tts.getInitialized() && tts.getLanguage().equals(language);
                if(!isInitialized) tts.initializeTTS(language, ttsListener);
                view.showDetectedLanguage(language);
                PlayTTS interactor = new PlayTTS(mExecutor, mMainThread, tts, text);
                interactor.execute();
            }

            @Override
            public void onDataNotAvailable() {

            }
        });
    }

    @Override
    public void onClickShowBrowser(String text) {
        view.setDictionaryWebPage(text);
    }

    @Override
    public void onClickPaste(String text) {
        view.setEditText(text);
    }

    @Override
    public void onStartOverlayMode() {
        view.startOverlayService();
    }

    @Override
    public void onStartClipboardMode() {
        view.startClipboardService();
    }

    private CustomTTS.Listener ttsListener = new CustomTTS.Listener() {
        @Override
        public void onLanguageUnavailable() {
            view.showLanguageNotAvailable();
        }
    };
}
