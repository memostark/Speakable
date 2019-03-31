package com.guillermonegrete.tts.Main;

public interface MainTTSContract {
    interface View {
        void setDictionaryWebPage(String word);

        void setEditText(String text);
    }

    interface Presenter{
        void onClickReproduce(String text);

        void onClickShowBrowser(String text);

        void onClickPaste(String text);
    }
}
