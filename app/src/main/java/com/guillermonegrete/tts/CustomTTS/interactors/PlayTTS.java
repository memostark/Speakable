package com.guillermonegrete.tts.CustomTTS.interactors;

import com.guillermonegrete.tts.AbstractInteractor;
import com.guillermonegrete.tts.CustomTTS.CustomTTS;
import com.guillermonegrete.tts.Executor;
import com.guillermonegrete.tts.MainThread;

public class PlayTTS extends AbstractInteractor implements PlayTTSInteractor{

    private CustomTTS customTTS;

    private String text_to_play;

    public PlayTTS(Executor threadExecutor, MainThread mainThread, CustomTTS customTTS, String text_to_play){
        super(threadExecutor, mainThread);
        this.customTTS = customTTS;
        this.text_to_play = text_to_play;
    }

    @Override
    public void run() {
        customTTS.speak(text_to_play);
    }
}
