package com.guillermonegrete.tts.customtts.interactors;

import com.guillermonegrete.tts.AbstractInteractor;
import com.guillermonegrete.tts.customtts.CustomTTS;
import com.guillermonegrete.tts.Executor;
import com.guillermonegrete.tts.MainThread;

public class PlayTTS extends AbstractInteractor implements PlayTTSInteractor{

    private CustomTTS customTTS;
    private CustomTTS.Listener listener;

    private String text_to_play;

    public PlayTTS(Executor threadExecutor, MainThread mainThread, CustomTTS customTTS, CustomTTS.Listener listener, String text_to_play){
        super(threadExecutor, mainThread);
        this.customTTS = customTTS;
        this.listener = listener;
        this.text_to_play = text_to_play;
    }

    @Override
    public void run() {
        customTTS.speak(text_to_play, listener);
    }
}
