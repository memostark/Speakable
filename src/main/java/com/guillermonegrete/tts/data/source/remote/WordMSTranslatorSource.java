package com.guillermonegrete.tts.data.source.remote;



import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.guillermonegrete.tts.data.source.WordDataSource;
import com.guillermonegrete.tts.db.Words;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class WordMSTranslatorSource implements WordDataSource {

    private static WordMSTranslatorSource INSTANCE;

    public static WordMSTranslatorSource getInstance(){
        if(INSTANCE == null){
          INSTANCE = new WordMSTranslatorSource();
        }

        return INSTANCE;
    }

    private WordMSTranslatorSource(){

    }

    @Override
    public void getWordLanguageInfo(final String wordText, final GetWordCallback callback) {
        System.out.print("Retrieving remote word data");
        final JSONArray body = createRequestBody(wordText);

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
                    Words retrieved_word = new Words(wordText, langCode, translation);
                    callback.onWordLoaded(retrieved_word);
                } catch (JSONException e) {
                    callback.onDataNotAvailable();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

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
                headers.put("Ocp-Apim-Subscription-Key", "49596075062f40e1b30f709feb7b1018");
                return headers;
            }
        };

    }

    private JSONArray createRequestBody(final String text){
        JSONArray body = new JSONArray();
        try {
            JSONObject obj = new JSONObject();
            obj.put("Text", text);
            body.put(obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return body;
    }
}
