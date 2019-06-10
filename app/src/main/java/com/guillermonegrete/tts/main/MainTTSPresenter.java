
package com.guillermonegrete.tts.main;

import com.guillermonegrete.tts.AbstractPresenter;
import com.guillermonegrete.tts.customtts.CustomTTS;
import com.guillermonegrete.tts.customtts.interactors.PlayTTS;
import com.guillermonegrete.tts.Executor;
import com.guillermonegrete.tts.MainThread;
import com.guillermonegrete.tts.data.source.WordRepository;
import com.guillermonegrete.tts.data.source.WordRepositorySource;
import com.guillermonegrete.tts.db.Words;

public class MainTTSPresenter extends AbstractPresenter implements MainTTSContract.Presenter {

    private CustomTTS tts;
    private WordRepository wordRepository;
    private MainTTSContract.View view;

    private boolean isPlaying;
    private boolean isAvailable;

    public MainTTSPresenter(Executor executor, MainThread mainThread, MainTTSContract.View view, WordRepository wordRepository, CustomTTS tts) {
        super(executor, mainThread);
        this.view = view;
        this.tts = tts;
        this.tts.setListener(ttsListener);
        this.wordRepository = wordRepository;
        isPlaying = false;
    }

    @Override
    public void onClickReproduce(final String text) {

        if(isPlaying) {
            tts.stop();
            isPlaying = false;
            view.showPlayIcon();
        }else{
            isAvailable = true;
            view.showLoadingTTS();
            // TODO this request should be done in a background thread
            wordRepository.getLanguageAndTranslation(text, new WordRepositorySource.GetTranslationCallback() {
                @Override
                public void onTranslationAndLanguage(Words word) {
                    String language = word.lang;
                    boolean isInitialized = tts.getInitialized() && tts.getLanguage().equals(language);
                    if (!isInitialized) tts.initializeTTS(language);
                    view.showDetectedLanguage(language);
                    if(isAvailable) {
                        PlayTTS interactor = new PlayTTS(mExecutor, mMainThread, tts, text);
                        interactor.execute();
                    }
                }

                @Override
                public void onDataNotAvailable() {}
            });
        }
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
            isPlaying = false;
            isAvailable = false;
            view.showLanguageNotAvailable();
        }

        @Override
        public void onSpeakStart() {
            isPlaying = true;
            // TODO move main thread code to interactor
            mMainThread.post(new Runnable() {
                @Override
                public void run() {
                    view.showStopIcon();
                }
            });
        }

        @Override
        public void onSpeakDone() {
            isPlaying = false;
            mMainThread.post(new Runnable() {
                @Override
                public void run() {
                    view.showPlayIcon();
                }
            });
        }
    };

    @Override
    public void start() {
        view.showPlayIcon();
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
            tts.stop();
            isPlaying = false;
        }
    }
}

