
package com.guillermonegrete.tts.main;

import com.guillermonegrete.tts.AbstractPresenter;
import com.guillermonegrete.tts.customtts.CustomTTS;
import com.guillermonegrete.tts.customtts.interactors.PlayTTS;
import com.guillermonegrete.tts.Executor;
import com.guillermonegrete.tts.MainThread;
import com.guillermonegrete.tts.data.source.WordRepository;
import com.guillermonegrete.tts.data.source.WordRepositorySource;
import com.guillermonegrete.tts.db.Words;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MainTTSPresenter extends AbstractPresenter implements MainTTSContract.Presenter {

    private CustomTTS tts;
    private WordRepository wordRepository;
    private MainTTSContract.View view;

    private boolean isPlaying;

    private String text;

    @Inject
    MainTTSPresenter(Executor executor, MainThread mainThread, WordRepository wordRepository, CustomTTS tts) {
        super(executor, mainThread);
        this.tts = tts;
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
            view.showLoadingTTS();
            this.text = text;

            // TODO this request should be done in a background thread
            wordRepository.getLanguageAndTranslation(text, new WordRepositorySource.GetTranslationCallback() {
                @Override
                public void onTranslationAndLanguage(Words word) {
                    String language = word.lang;
                    tts.initializeTTS(language, ttsListener);
                    view.showDetectedLanguage(language);
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
        public void onEngineReady() {
            System.out.println("On engine ready");

            System.out.println("Running tts interactor");
            PlayTTS interactor = new PlayTTS(mExecutor, mMainThread, tts, ttsListener, text);
            interactor.execute();
        }

        @Override
        public void onLanguageUnavailable() {
            isPlaying = false;
            view.showLanguageNotAvailable();
        }

        @Override
        public void onSpeakStart() {
            isPlaying = true;
            // TODO move main thread code to interactor
            mMainThread.post(() -> view.showStopIcon());
        }

        @Override
        public void onSpeakDone() {
            isPlaying = false;
            mMainThread.post(() -> view.showPlayIcon());
        }
    };

    @Override
    public void start() {}

    @Override
    public void setView(MainTTSContract.View view) {
        this.view = view;
        this.view.showPlayIcon();
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
        tts.removeListener(ttsListener);
        view = null;
    }

    private void stopPlaying(){
        if(isPlaying) {
            tts.stop();
            isPlaying = false;
        }
    }
}

