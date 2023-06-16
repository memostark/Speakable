package com.guillermonegrete.tts.main;

import com.guillermonegrete.tts.BasePresenter;

public interface MainTTSContract {
    interface View {
        void setDictionaryWebPage(String word);

        void setEditText(String text);

        void startClipboardService();

        void startOverlayService();

        void showDetectedLanguage(String language);

        void showLanguageNotAvailable();

        void showLoadingTTS();

        void showPlayIcon();

        void showStopIcon();

    }

    interface Presenter extends BasePresenter<View> {
        void onClickReproduce(String text, String lang);
    }
}
