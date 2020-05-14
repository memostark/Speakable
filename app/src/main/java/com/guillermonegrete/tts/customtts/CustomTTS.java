package com.guillermonegrete.tts.customtts;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;

import android.speech.tts.UtteranceProgressListener;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Locale;


@Singleton
public class CustomTTS implements TextToSpeech.OnInitListener{

    private TextToSpeech localTTS;

    private Boolean isInitialized;
    private Boolean isShutdown = false;

    private String language;

    private Listener listener = null;

    private HashMap<String, String> map = new HashMap<>();
    private Bundle params = new Bundle();

    private Context context;

    @Inject
    public CustomTTS(Context context){
        this.context = context.getApplicationContext();

        localTTS = new TextToSpeech(this.context, this);
        isInitialized = false;
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "CustomTTSID");
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "");
    }

    private void speak(String text){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            localTTS.speak(text, TextToSpeech.QUEUE_FLUSH, params,"CustomTTSID");
        } else {
            localTTS.speak(text, TextToSpeech.QUEUE_FLUSH, map);
        }
    }

    public void speak(String text, Listener listener){
        this.listener = listener;
        if(isInitialized) speak(text);
        else listener.onLanguageUnavailable();
    }

    public void stop(){
        localTTS.stop();
    }

    public void removeListener(Listener listener){
        if(listener == this.listener){
            this.listener = null;
        }
    }

     public void initializeTTS(String langCode, Listener listener) {
        boolean ttsReady = isInitialized && language.equals(langCode);

        if(!ttsReady){
            this.listener = listener;
            language = langCode;
            isInitialized = false;

            initializeGoogleLocalService(langCode);
            localTTS.setOnUtteranceProgressListener(new CustomUtteranceListener());

        } else {
            if(listener != null) listener.onEngineReady();
        }
    }

    private void initializeGoogleLocalService(String langCode){
        if(isShutdown){
            // Recreate and set language on init method
            localTTS = new TextToSpeech(context.getApplicationContext(), this);
        } else {
            setLocalLanguage(langCode);
        }
    }

    @Override
    public void onInit(int status) {
        String message;
        switch (status){
            case TextToSpeech.ERROR:
                message = "Error";
                break;
            case TextToSpeech.SUCCESS:
                message = "Success";

                // If true, it was called from initializeGoogleLocalService method, set language
                if(isShutdown) setLocalLanguage(language);
                isShutdown = false;
                break;
            default:
                message = "Unknown";
                break;
        }
        System.out.println("Local TTS Status: " + message);
    }

    private void setLocalLanguage(String langCode){

        int result = localTTS.setLanguage(new Locale(langCode.toUpperCase()));
        if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
            System.out.println("Error code: " + result);
            System.out.println("Initialize TTS Error, This Language is not supported");
            if(listener != null) listener.onLanguageUnavailable();

        } else {
            isInitialized = true;

            if(listener != null) listener.onEngineReady();
        }
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
        public void onError(String utteranceId) {
            listener.onLanguageUnavailable();
        }

        @Override
        public void onError(String utteranceId, int errorCode) {
            super.onError(utteranceId, errorCode);
            String error;
            switch (errorCode){
                case TextToSpeech.ERROR_INVALID_REQUEST:
                    error = "ERROR_INVALID_REQUEST";
                    break;
                case TextToSpeech.ERROR_NETWORK:
                    error = "ERROR_NETWORK";
                    break;
                case TextToSpeech.ERROR_NETWORK_TIMEOUT:
                    error = "ERROR_NETWORK_TIMEOUT";
                    break;
                case TextToSpeech.ERROR_NOT_INSTALLED_YET:
                    error = "ERROR_NOT_INSTALLED_YET";
                    break;
                case TextToSpeech.ERROR_OUTPUT:
                    error = "ERROR_OUTPUT";
                    break;
                case TextToSpeech.ERROR_SERVICE:
                    error = "ERROR_SERVICE";
                    break;
                case TextToSpeech.ERROR_SYNTHESIS:
                    error = "ERROR_SYNTHESIS";
                    break;
                default:
                    error = "Unknown";
                    break;
            }
            System.out.println("Text To speech error: " + error);
        }
    }

    public String getLanguage() {
        return language;
    }

    public void finishTTS(){
        System.out.println("Destroying localTTS");
        isInitialized = false;
        isShutdown = true;
        if(localTTS != null){
            localTTS.stop();
            localTTS.shutdown();
        }
    }

    public interface Listener{
        void onEngineReady();

        void onLanguageUnavailable();

        void onSpeakStart();

        void onSpeakDone();

        void onError();
    }
}
