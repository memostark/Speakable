package com.guillermonegrete.tts.CustomTTS;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;


public class CustomTTS implements TextToSpeech.OnInitListener{
    private static CustomTTS INSTANCE;

    private TextToSpeech tts;

    private Boolean isInitialized;

    private String TAG = this.getClass().getSimpleName();

    private String language;

    private Listener listener;

    public static CustomTTS getInstance(Context context){
        if(INSTANCE == null){
            INSTANCE = new CustomTTS(context);
        }

        return INSTANCE;
    }

    private CustomTTS(Context context){
        tts = new TextToSpeech(context, this);
        isInitialized = false;
    }

    public void speak(String text){
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

     public void initializeTTS(final String langCode, Listener listener) {
        this.listener = listener;
        language = langCode;
        isInitialized = false;
        initializeGoogleLocalService(langCode);
    }

    private void initializeGoogleLocalService(String langCode){
        System.out.println(String.format("Language to set: %s", langCode ));
        int result = tts.setLanguage(new Locale(langCode.toUpperCase()));
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
        System.out.print("Local TTS Status: ");
        System.out.println(status);
    }

    public Boolean getInitialized() {
        return isInitialized;
    }

    public String getLanguage() {
        return language;
    }

    public void finishTTS(){
        System.out.println("Destroying tts");
        isInitialized = false;
        if(tts!=null){
            tts.stop();
            tts.shutdown();
        }
        INSTANCE = null;
    }

    public interface Listener{
        void onLanguageUnavailable();
    }
}
