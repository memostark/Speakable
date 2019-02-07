package com.guillermonegrete.tts.CustomTTS;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.guillermonegrete.speech.tts.Synthesizer;
import com.guillermonegrete.speech.tts.Voice;
import com.guillermonegrete.tts.R;

import java.util.Locale;


public class CustomTTS implements TextToSpeech.OnInitListener{
    private static CustomTTS INSTANCE;

    private TextToSpeech tts;
    private Synthesizer mSynth;

    private Boolean isInitialized;
    private Boolean localTTS;

    private String TAG = this.getClass().getSimpleName();

    private String language;

    public static CustomTTS getInstance(Context context){
        if(INSTANCE == null){
            INSTANCE = new CustomTTS(context);
        }

        return INSTANCE;
    }

    public CustomTTS(Context context){
        tts = new TextToSpeech(context, this);
        mSynth = new Synthesizer(context.getResources().getString(R.string.api_key));
        isInitialized = false;
        localTTS = false;
    }

    public void speak(String text){
        if(localTTS){
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }else{
            mSynth.SpeakToAudio(text);
        }
    }

     public void initializeTTS(final String langCode) {
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
        localTTS = false;
        isInitialized = true;
    }

    private void initializeGoogleLocalService(String langCode){
        System.out.println(String.format("Language to set: %s", langCode ));
        int result = tts.setLanguage(new Locale(langCode.toUpperCase()));
        if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
            System.out.print("Error code: ");
            System.out.println(result);
            System.out.println("Initialize TTS Error, This Language is not supported");
        } else {
            localTTS = true;
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
    }
}
