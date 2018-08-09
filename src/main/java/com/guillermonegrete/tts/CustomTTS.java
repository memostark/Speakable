package com.guillermonegrete.tts;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CustomTTS implements TextToSpeech.OnInitListener{
    private Context mContext;
    private TextToSpeech tts;
    private Synthesizer mSynth;
    private CustomTTSListener mListener;

    private Boolean isGoogleTTSready;
    private Boolean localTTS;

    private String TAG = this.getClass().getSimpleName();

    public CustomTTS(Context context){
        mContext = context;
        tts = new TextToSpeech(context, this);
        mSynth = new Synthesizer(mContext.getResources().getString(R.string.api_key));
    }

    void setListener(CustomTTSListener listener){
        mListener=listener;
    }

    public void speak(String text){
        if(localTTS){
            tts.speak(text, TextToSpeech.QUEUE_ADD, null);
        }else{
            mSynth.SpeakToAudio(text);
        }
    }

    public void determineLanguage(final String text){
        final JSONArray body = new JSONArray();
        try {
            JSONObject obj = new JSONObject();
            obj.put("Text", text);
            body.put(obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, body.toString());

        String urlDetectLang = "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&to=en";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.POST, urlDetectLang,null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {

                String langCode, translation;
                try {
                    JSONObject jsonObject = response.getJSONObject(0);
                    JSONObject jsonLang = jsonObject.getJSONObject("detectedLanguage");
                    JSONArray jsonTranslation = jsonObject.getJSONArray("translations");
                    langCode = jsonLang.getString("language");
                    translation = jsonTranslation.getJSONObject(0).getString("text");
                } catch (JSONException e) {
                    langCode = "NOLANG";
                    translation = "No translation";
                    Log.e(TAG, e.getMessage());
                    e.printStackTrace();
                }
                if(mListener!=null) mListener.onLanguageDetected(translation);
                playVoice(text, langCode);
                Log.i(TAG,langCode);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Error: " + error.getMessage());
            }
        }){
            @Override
            public byte[] getBody() {
                return body.toString().getBytes();
            }

            @Override
            public String getBodyContentType() {
                return "application/json";
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type","application/json");
                headers.put("Ocp-Apim-Subscription-Key", mContext.getResources().getString(R.string.translator_api_key));
                return headers;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(mContext);
        queue.add(request);
    }

    private void playVoice(final String selectedText, final String langCode) {
        if(langCode.equals("he")){
            mSynth.SetServiceStrategy(Synthesizer.ServiceStrategy.AlwaysService);
            Voice v = new Voice("he-IL", "Microsoft Server Speech Text to Speech Voice (he-IL, Asaf)", Voice.Gender.Male, true);
            mSynth.SetVoice(v, null);
            mSynth.SpeakToAudio(selectedText);
            localTTS=false;
        }else{
            int result = tts.setLanguage(new Locale(langCode));
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("error", "This Language is not supported");
            } else {
                localTTS=true;
                tts.speak(selectedText, TextToSpeech.QUEUE_ADD, null);
            }
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
