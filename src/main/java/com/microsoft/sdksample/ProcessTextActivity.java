/*
    referencias: https://medium.com/google-developers/custom-text-selection-actions-with-action-process-text-191f792d2999
    Actividad flotante: https://stackoverflow.com/questions/33853311/how-to-create-a-floating-touchable-activity-that-still-allows-to-touch-native-co
                        http://www.androidmethlab.com/2015/09/transparent-floating-window-in-front-of.html
*/

package com.microsoft.sdksample;

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

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.microsoft.speech.tts.Synthesizer;
import com.microsoft.speech.tts.Voice;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ProcessTextActivity extends Activity{
    private Synthesizer m_syn;
    private CustomAdapter mAdapter;

    private String TAG="ProcessTextActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setWindowParams();
        setContentView(R.layout.activity_processtext);
        Intent intent = getIntent();
        final CharSequence text = intent
                .getCharSequenceExtra("android.intent.extra.PROCESS_TEXT");
        final String textString=text.toString();

        TextView mTextTTS = (TextView) findViewById(R.id.text_tts);
        mTextTTS.setText(text);

        if (m_syn == null) {
            // Create Text To Speech Synthesizer.
            m_syn = new Synthesizer(getString(R.string.api_key));
        }

        mAdapter = new CustomAdapter(this);

        //------------------------------ Tomado de https://stackoverflow.com/questions/20337389/how-to-parse-wiktionary-api-------------------------------------------------
        String url = "https://en.wiktionary.org/w/api.php?action=query&prop=extracts&format=json&explaintext=&titles="+textString;
        final TextView mWikiContent = (TextView) findViewById(R.id.text_wikiContent);
        mWikiContent.setMovementMethod(new ScrollingMovementMethod());

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(detectLanguage());

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
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
                            extract = "Parse Failed";
                            mWikiContent.setText(extract);
                        }
                        String[] separated = extract.split("\n== ");
                        List<String> langs = new ArrayList<>();
                        int i;
                        for (i=0; i<separated.length;i++){
                            langs.add(separated[i]);
                            //Log.i(TAG,(i+1)+".- "+separated[i]);
                        }

                        for (i=1;i<langs.size();i++){
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
                        /*tokens = new StringTokenizer(extract, "===");
                        i=1;
                        Log.i(TAG,"--------------OPCION 3---------------");
                        while (tokens.hasMoreTokens()){
                            Log.i(TAG,i+".- "+tokens.nextToken().trim());
                            i++;
                        }*/


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mWikiContent.setText("No entry");
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);

        m_syn.SetServiceStrategy(Synthesizer.ServiceStrategy.AlwaysService);
        Voice v = new Voice("he-IL", "Microsoft Server Speech Text to Speech Voice (he-IL, Asaf)", Voice.Gender.Male, true);
        m_syn.SetVoice(v, null);
        m_syn.SpeakToAudio(textString);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                m_syn.SpeakToAudio(textString);
            }
        });
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

        /*int i, height=0;
        int MAX_HEIGHT = 700;
        for (i=0;i<mAdapter.getCount();i++) {
            View item = mAdapter.getView(i, null, listView);
            item.measure(0, 0);
            height+=item.getMeasuredHeight();
            Log.i(TAG, "Height: " + item.getMeasuredHeight());

        }
        // https://stackoverflow.com/questions/40861136/set-listview-height-programmatically
        if(height>500){
            ViewGroup.LayoutParams params = listView.getLayoutParams();
            params.height=MAX_HEIGHT;
            listView.setLayoutParams(params);
            //listView.requestLayout();
        }*/

    }

    private StringRequest detectLanguage(){
        final JSONArray body = new JSONArray();
        try {
            JSONObject obj = new JSONObject();
            obj.put("Text", "Hello world");
            body.put(obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, body.toString());

        String urlDetectLang = "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&to=de&to=it";
        return new StringRequest(Request.Method.POST, urlDetectLang, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i(TAG, "---------->"+response);
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Error: " + error.getMessage());
            }
        }){
            @Override
            public byte[] getBody() throws AuthFailureError {
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
                headers.put("Ocp-Apim-Subscription-Key", getString(R.string.translator_api_key));
                return headers;
            }
        };


    }
}

