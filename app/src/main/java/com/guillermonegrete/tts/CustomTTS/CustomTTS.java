package com.guillermonegrete.tts.CustomTTS;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;

import android.speech.tts.UtteranceProgressListener;
import com.guillermonegrete.speech.tts.Synthesizer;
import com.guillermonegrete.speech.tts.Voice;
import com.guillermonegrete.tts.BuildConfig;

import java.util.HashMap;
import java.util.Locale;


public class CustomTTS implements TextToSpeech.OnInitListener{
    private static CustomTTS INSTANCE;

    private TextToSpeech localTTS;
    private Synthesizer mSynth;

    private Boolean isInitialized;
    private Boolean usinglocalTTS;

    private String TAG = this.getClass().getSimpleName();

    private String language;

    private Listener listener = null;

    private HashMap<String, String> map = new HashMap<>();
    private Bundle params = new Bundle();

    public static CustomTTS getInstance(Context context){
        if(INSTANCE == null){
            INSTANCE = new CustomTTS(context);
        }

        return INSTANCE;
    }

    private CustomTTS(Context context){
        localTTS = new TextToSpeech(context, this);
        mSynth = new Synthesizer(BuildConfig.TTSApiKey, synthCallback);
        isInitialized = false;
        usinglocalTTS = false;
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "CustomTTSID");
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "");
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void speak(String text){
        if(usinglocalTTS){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                localTTS.speak(text, TextToSpeech.QUEUE_FLUSH, params,"CustomTTSID");
            } else {
                localTTS.speak(text, TextToSpeech.QUEUE_FLUSH, map);
            }
        }else{
            if(mSynth.getLocalAudioBytes(text) == null){
                mSynth.getAudio(text);
            }
            mSynth.speakLocalAudio();
        }
    }

    public void stop(){
        if(usinglocalTTS) localTTS.stop();
        else mSynth.stopSound();
    }

     public void initializeTTS(final String langCode) {
        localTTS.setOnUtteranceProgressListener(new CustomUtteranceListener());
        language = langCode;
        isInitialized = false;
        if(langCode.equals("he")){
            initializeMSService();
        }else{
            initializeGoogleLocalService(langCode);
        }
    }

    private void initializeMSService(){
        mSynth.SetServiceStrategy(Synthesizer.ServiceStrategy.AlwaysService);
        Voice voice = new Voice("he-IL", "Microsoft Server Speech Text to Speech Voice (he-IL, Asaf)", Voice.Gender.Male, true);
        mSynth.SetVoice(voice, null);
        usinglocalTTS = false;
        isInitialized = true;
    }

    private void initializeGoogleLocalService(String langCode){
        System.out.println(String.format("Language to set: %s", langCode ));
        int result = localTTS.setLanguage(new Locale(langCode.toUpperCase()));
        if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
            System.out.print("Error code: ");
            System.out.println(result);
            System.out.println("Initialize TTS Error, This Language is not supported");
            if(listener != null) listener.onLanguageUnavailable();
        } else {
            usinglocalTTS = true;
            isInitialized = true;
        }
    }

    @Override
    public void onInit(int status) {
        System.out.print("Local TTS Status:");
        System.out.println(status);
    }

    private class CustomUtteranceListener extends UtteranceProgressListener {

        @Override
        public void onStart(String utteranceId) {
            listener.onSpeakStart();
        }

        @Override
        public void onDone(String utteranceId) {
            listener.onSpeakDone();
        }

        @Override
        public void onError(String utteranceId) {listener.onLanguageUnavailable();}
    }

    private Synthesizer.Callback synthCallback = new Synthesizer.Callback() {
        @Override
        public void onStart() {listener.onSpeakStart();}

        @Override
        public void onStop() { listener.onSpeakDone();}

        @Override
        public void onError() { }
    };

    public Boolean getInitialized() {
        return isInitialized;
    }

    public String getLanguage() {
        return language;
    }

    public void finishTTS(){
        System.out.println("Destroying localTTS");
        isInitialized = false;
        if(localTTS !=null){
            localTTS.stop();
            localTTS.shutdown();
        }
        INSTANCE = null;
    }

    public interface Listener{
        void onLanguageUnavailable();

        void onSpeakStart();

        void onSpeakDone();
    }
}
