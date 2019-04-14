package com.guillermonegrete.tts.CustomTTS;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;

import android.speech.tts.UtteranceProgressListener;

import java.util.HashMap;
import java.util.Locale;


public class CustomTTS implements TextToSpeech.OnInitListener{
    private static CustomTTS INSTANCE;

    private TextToSpeech localTTS;

    private Boolean isInitialized;

    private String TAG = this.getClass().getSimpleName();

    private String language;

    private Listener listener;

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
        isInitialized = false;
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "CustomTTSID");
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "");
    }

    public void speak(String text){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            localTTS.speak(text, TextToSpeech.QUEUE_FLUSH, params,"CustomTTSID");
        } else {
            localTTS.speak(text, TextToSpeech.QUEUE_FLUSH, map);
        }
    }

    public void stop(){
        localTTS.stop();
    }

     public void initializeTTS(final String langCode, Listener listener) {
        this.listener = listener;
        localTTS.setOnUtteranceProgressListener(new CustomUtteranceListener());
        language = langCode;
        isInitialized = false;
        initializeGoogleLocalService(langCode);
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
