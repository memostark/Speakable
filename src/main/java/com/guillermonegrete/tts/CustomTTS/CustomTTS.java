package com.guillermonegrete.tts.CustomTTS;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.guillermonegrete.speech.tts.Synthesizer;
import com.guillermonegrete.speech.tts.Voice;
import com.guillermonegrete.tts.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CustomTTS implements TextToSpeech.OnInitListener{
    private static CustomTTS INSTANCE;

    private TextToSpeech tts;
    private Synthesizer mSynth;

    private Boolean isGoogleTTSready;
    private Boolean localTTS;

    private String TAG = this.getClass().getSimpleName();

    public String language;

    public static CustomTTS getInstance(Context context){
        if(INSTANCE == null){
            INSTANCE = new CustomTTS(context);
        }

        return INSTANCE;
    }

    private CustomTTS(Context context){
        tts = new TextToSpeech(context, this);
        mSynth = new Synthesizer(context.getResources().getString(R.string.api_key));
    }

    public void speak(String text){
        if(localTTS){
            tts.speak(text, TextToSpeech.QUEUE_ADD, null);
        }else{
            mSynth.SpeakToAudio(text);
        }
    }

     public void initializeTTS(final String langCode) {
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
    }

    private void initializeGoogleLocalService(final String langCode){
        int result = tts.setLanguage(new Locale(langCode));
        if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("Initialize TTS Error", "This Language is not supported");
        } else {
            localTTS = true;
        }
    }

    @Override
    public void onInit(int status) {
        isGoogleTTSready = (status == TextToSpeech.SUCCESS);
    }

    public void finishTTS(){
        if(tts!=null){
            tts.stop();
            tts.shutdown();
        }
    }
}
