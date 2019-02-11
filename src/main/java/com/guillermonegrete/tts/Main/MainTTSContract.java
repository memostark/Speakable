package com.guillermonegrete.tts.Main;

public interface MainTTSContract {
    interface View {
        void setDictionaryWebPage(String word);
    }

    interface Presenter{
        void onClickReproduce(String text);
    }
}
