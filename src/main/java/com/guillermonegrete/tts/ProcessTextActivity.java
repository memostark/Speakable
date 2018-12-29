/*
    Ejemplo tomado: https://github.com/Azure-Samples/Cognitive-Speech-TTS/tree/master/Android
    referencias: https://medium.com/google-developers/custom-text-selection-actions-with-action-process-text-191f792d2999
    Actividad flotante: https://stackoverflow.com/questions/33853311/how-to-create-a-floating-touchable-activity-that-still-allows-to-touch-native-co
                        http://www.androidmethlab.com/2015/09/transparent-floating-window-in-front-of.html
*/

package com.guillermonegrete.tts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class ProcessTextActivity extends Activity implements CustomTTSListener{
    private CustomTTS tts;
    private CustomAdapter mAdapter;

    private String TAG=this.getClass().getSimpleName();
    public static final String LONGPRESS_SERVICE_NOSHOW = "startServiceLong";
    public static final String LONGPRESS_SERVICE = "showServiceg";

    private boolean mIsSentence;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setWindowParams();
        setContentView(R.layout.activity_processtext);
        Intent intent = getIntent();
        final CharSequence selected_text = intent
                .getCharSequenceExtra("android.intent.extra.PROCESS_TEXT");
        final String textString = selected_text.toString();

        TextView mTextTTS = (TextView) findViewById(R.id.text_tts);
        mTextTTS.setText(selected_text);


        if(tts == null) {
            tts = new CustomTTS(ProcessTextActivity.this);
            tts.setListener(this);
        }

        final Intent intentService = new Intent(this, ScreenTextService.class);
        intentService.setAction(LONGPRESS_SERVICE_NOSHOW);
        startService(intentService);

        mAdapter = new CustomAdapter(this);

        String[] splittedText = textString.split(" ");
        mIsSentence = splittedText.length > 1;
        if(!mIsSentence) sendWiktionaryRequest(textString);
        tts.determineLanguage(textString);


        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tts.speak(textString);
            }
        });
    }

    private void sendWiktionaryRequest(String textString){
        //------------------------------ Taken from https://stackoverflow.com/questions/20337389/how-to-parse-wiktionary-api-------------------------------------------------
        String url = "https://en.wiktionary.org/w/api.php?action=query&prop=extracts&format=json&explaintext=&redirects=1&titles=" + textString;
        final TextView mWikiContent = (TextView) findViewById(R.id.text_translation);
        mWikiContent.setMovementMethod(new ScrollingMovementMethod());

        RequestQueue queue = Volley.newRequestQueue(this);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        String extract = extractResponseContent(response);
                        List<String> langs = getLanguages(extract);

                        String[] separated;

                        for (int i=1; i<langs.size(); i++){
                            separated = langs.get(i).split("\n=== ");
                            String lang = separated[0].split(" ")[0];
                            mAdapter.addSectionHeaderItem(lang);
                            int j;
                            //Log.i(TAG,"--------------"+lang+"---------------");
                            for (j=1; j<separated.length;j++){
                                String[] subheaders = separated[j].split(" ===\n");
                                //Log.i(TAG,"----Subheader " + j +": "+subheaders[0]);
                                //Log.i(TAG,subheaders[1]);
                                mAdapter.addSectionSubHeaderItem(subheaders[0]);
                                String[] subsubheader = subheaders[1].split("\n==== ");
                                int k;
                                for(k=0;k<subsubheader.length;k++){
                                    mAdapter.addItem(subsubheader[k].replace("====\n",""));
                                }
                            }

                        }

                        setList();

                    }

                    private String extractResponseContent(String response){
                        String extract;
                        try{
                            JSONObject reader = new JSONObject(response);
                            JSONObject pagesJSON = reader.getJSONObject("query").getJSONObject("pages");
                            Iterator<String> iterator = pagesJSON.keys();
                            String key="";
                            while (iterator.hasNext()) {
                                key = iterator.next();
                                Log.i("TAG","key:"+key);
                            }
                            JSONObject extractJSON = pagesJSON.getJSONObject(key);
                            extract = extractJSON.getString("extract");
                        }catch (JSONException e){
                            Log.e("BingTTS","unexpected JSON exception", e);
                            extract = "Parse Failed: " + e.getMessage();
                            mWikiContent.setText(extract);
                        }
                        return extract;
                    }

                    private List<String> getLanguages(String extract){
                        String[] separated = extract.split("\n== ");
                        List<String> langs = new ArrayList<>();
                        for (int i=0; i<separated.length; i++){
                            langs.add(separated[i]);
                            //Log.i(TAG,(i+1)+".- "+separated[i]);
                        }
                        return langs;
                    }

                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mWikiContent.setText(R.string.no_entry);
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);

    }

    @Override
    protected void onDestroy() {
        if(tts != null) tts.finishTTS();
        super.onDestroy();
    }

    public void setWindowParams() {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        WindowManager.LayoutParams wlp = getWindow().getAttributes();
        wlp.dimAmount = 0;
        wlp.flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        getWindow().setAttributes(wlp);
    }

    private void setList(){
        ListView listView = (ListView) findViewById(R.id.listView_wiki);
        listView.setAdapter(mAdapter);
    }


    @Override
    public void onLanguageDetected(String translation) {
        if(mIsSentence){
            TextView mTextTranslation = (TextView) findViewById(R.id.text_translation);
            mTextTranslation.setText(translation);
        }
    }
}

