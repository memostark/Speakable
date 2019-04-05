package com.guillermonegrete.tts.Main;

import com.guillermonegrete.tts.AbstractPresenter;
import com.guillermonegrete.tts.CustomTTS.CustomTTS;
import com.guillermonegrete.tts.CustomTTS.interactors.PlayTTS;
import com.guillermonegrete.tts.Executor;
import com.guillermonegrete.tts.MainThread;
import com.guillermonegrete.tts.data.source.WordDataSource;
import com.guillermonegrete.tts.data.source.remote.MSTranslatorSource;
import com.guillermonegrete.tts.db.Words;

public class MainTTSPresenter extends AbstractPresenter implements MainTTSContract.Presenter {

    private CustomTTS tts;
    private MSTranslatorSource languageSource;
    private MainTTSContract.View view;

    public MainTTSPresenter(Executor executor, MainThread mainThread, MainTTSContract.View view, MSTranslatorSource languageSource, CustomTTS tts) {
        super(executor, mainThread);
        this.view = view;
        this.tts = tts;
        this.languageSource = languageSource;
    }

    @Override
    public void onClickReproduce(final String text) {
        // TODO this request should be done in a background thread
        languageSource.getWordLanguageInfo(text, new WordDataSource.GetWordCallback() {
            @Override
            public void onWordLoaded(Words word) {
                boolean isInitialized = tts.getInitialized() && tts.getLanguage().equals(word.lang);
                if(!isInitialized) tts.initializeTTS(word.lang);
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
}
