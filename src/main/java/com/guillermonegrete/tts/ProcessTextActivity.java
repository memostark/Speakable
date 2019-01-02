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
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.db.WordsDAO;
import com.guillermonegrete.tts.db.WordsDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class ProcessTextActivity extends FragmentActivity implements CustomTTSListener{
    private CustomTTS tts;
    private WiktionaryListAdapter mAdapter;

    private String TAG = this.getClass().getSimpleName();
    public static final String LONGPRESS_SERVICE_NOSHOW = "startServiceLong";
    public static final String LONGPRESS_SERVICE = "showServiceg";

    private boolean mIsSentence;

    private String mTranslation;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setWindowParams();

        Intent intent = getIntent();
        final CharSequence selected_text = intent
                .getCharSequenceExtra("android.intent.extra.PROCESS_TEXT");
        final String textString = selected_text.toString();

        if(tts == null) {
            tts = new CustomTTS(ProcessTextActivity.this);
            tts.setListener(this);
        }

        final Intent intentService = new Intent(this, ScreenTextService.class);
        intentService.setAction(LONGPRESS_SERVICE_NOSHOW);
        startService(intentService);

        mAdapter = new WiktionaryListAdapter(this);

        String[] splittedText = textString.split(" ");
        mIsSentence = splittedText.length > 1;
        if(mIsSentence){
            setSentenceLayout();
        }else{
            setWordLayout(textString);
            sendWiktionaryRequest(textString);
        }
        tts.determineLanguage(textString);
        TextView mTextTTS = (TextView) findViewById(R.id.text_tts);
        mTextTTS.setText(selected_text);


    }

    private void setWordLayout(final String textString){
        setContentView(R.layout.activity_processtext);

        WordsDAO wordsDAO = WordsDatabase.getDatabase(getApplicationContext()).wordsDAO();
        Words foundWords = wordsDAO.findWord(textString);
        if(foundWords != null) {
            ImageButton saveIcon = (ImageButton) findViewById(R.id.save_icon);
            saveIcon.setImageResource(R.drawable.ic_bookmark_black_24dp);
        }

        findViewById(R.id.play_tts_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tts.speak(textString);
            }
        });

        findViewById(R.id.save_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // saveWord(textString);
                showSaveDialog(textString);

            }
        });


    }

    private void setSentenceLayout(){
        setContentView(R.layout.activity_process_sentence);

    }

    private void showSaveDialog(String word) {
        DialogFragment dialogFragment;
        dialogFragment = SaveWordDialogFragment.newInstance(word, tts.language, mTranslation);
        dialogFragment.show(getSupportFragmentManager(), "New word process");
    }

    private void sendWiktionaryRequest(String textString){
        //------------------------------ Taken from https://stackoverflow.com/questions/20337389/how-to-parse-wiktionary-api-------------------------------------------------
        String url = "https://en.wiktionary.org/w/api.php?action=query&prop=extracts&format=json&explaintext=&redirects=1&titles=" + textString;
        final TextView mWikiContent = (TextView) findViewById(R.id.text_error_message);
        mWikiContent.setMovementMethod(new ScrollingMovementMethod());

        RequestQueue queue = Volley.newRequestQueue(this);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        String extract = extractResponseContent(response);
                        WiktionaryParser wikiParser = new WiktionaryParser(extract);

                        List<WiktionaryParser.WiktionaryItem> wikiItems = wikiParser.parse();

                        for (WiktionaryParser.WiktionaryItem item: wikiItems) {
                            switch (item.itemType){
                                case WiktionaryParser.TYPE_HEADER:
                                    mAdapter.addSectionHeaderItem(item.itemText);
                                    break;
                                case WiktionaryParser.TYPE_SUBHEADER:
                                    mAdapter.addSectionSubHeaderItem(item.itemText);
                                    break;
                                case WiktionaryParser.TYPE_TEXT:
                                    mAdapter.addItem(item.itemText);
                                    break;
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

                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mWikiContent.setText(R.string.no_entry);
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);

    }

    public static class WiktionaryParser{
        String text;
        final static int TYPE_HEADER = 100;
        final static int TYPE_SUBHEADER = 101;
        final static int TYPE_TEXT = 102;

        public WiktionaryParser(String text){
            this.text = text;
        }

        public List<WiktionaryItem> parse(){
            List<String> LanguageSections = getLanguages(text);

            String[] separated;
            List<WiktionaryItem> items = new ArrayList<>();

            for (int i=1; i<LanguageSections.size(); i++){
                separated = LanguageSections.get(i).split("\n=== ");
                String lang = separated[0].split(" ")[0];

                items.add(new WiktionaryItem(lang, TYPE_HEADER));

                int j;
                //Log.i(TAG,"--------------"+lang+"---------------");
                for (j=1; j<separated.length;j++){
                    String[] subheaders = separated[j].split(" ===\n");
                    //Log.i(TAG,"----Subheader " + j +": "+subheaders[0]);
                    //Log.i(TAG,subheaders[1]);
                    items.add(new WiktionaryItem(subheaders[0], TYPE_SUBHEADER));
                    String[] subsubheader = subheaders[1].split("\n==== ");
                    for(int k=0; k<subsubheader.length; k++){
                        items.add(new WiktionaryItem(subsubheader[k].replace("====\n",""), TYPE_TEXT));
                    }
                }
            }

            return items;
        }

        private List<String> getLanguages(String extract){
            String[] separated = extract.split("\n== ");
            List<String> langs = new ArrayList<>();
            Collections.addAll(langs, separated);
            return langs;
        }

        public static class WiktionaryItem{
            String itemText;
            int itemType;
            public WiktionaryItem(String itemText, int itemType){
                this.itemText = itemText;
                this.itemType = itemType;
            }

            /*
            *  Used for Collections.frequency to count how many types are inside List
            *  Should find a better way to do this
            * */
            @Override
            public boolean equals(Object o) {
                WiktionaryItem instance;
                if(!(o instanceof WiktionaryItem)) return false;
                else {
                    instance = (WiktionaryItem) o;
                    return this.itemType == instance.itemType;
                }
            }
        }
    }


    public static List<String> getLanguages(String extract){
        String[] separated = extract.split("\n== ");
        List<String> langs = new ArrayList<>();
        for (int i=0; i<separated.length; i++){
            langs.add(separated[i]);
            //Log.i(TAG,(i+1)+".- "+separated[i]);
        }
        return langs;
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
        mTranslation = translation;
        if(mIsSentence){
            TextView mTextTranslation = (TextView) findViewById(R.id.text_translation);
            mTextTranslation.setText(translation);
        }
    }
}

