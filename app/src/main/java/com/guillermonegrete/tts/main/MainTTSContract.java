package com.guillermonegrete.tts.main;

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

    interface Presenter{
        void onClickReproduce(String text);

        void onClickShowBrowser(String text);

        void onClickPaste(String text);

        void onStartOverlayMode();

        void onStartClipboardMode();
    }
}
