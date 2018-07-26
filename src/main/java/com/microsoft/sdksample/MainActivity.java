//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license.
//
// Microsoft Cognitive Services (formerly Project Oxford): https://www.microsoft.com/cognitive-services
//
// Microsoft Cognitive Services (formerly Project Oxford) GitHub:
// https://github.com/Microsoft/Cognitive-Speech-TTS
//
// Copyright (c) Microsoft Corporation
// All rights reserved.
//
// MIT License:
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//

package com.microsoft.sdksample;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;
import com.microsoft.speech.tts.Synthesizer;
import com.microsoft.speech.tts.Voice;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    // Note: Sign up at http://www.projectoxford.ai for the client credentials.
    private Synthesizer m_syn;
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 100;
    private String TAG=this.getClass().getSimpleName();

    private boolean localTTS;
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getString(R.string.api_key).startsWith("Please")) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.add_subscription_key_tip_title))
                    .setMessage(getString(R.string.add_subscription_key_tip))
                    .setCancelable(false)
                    .show();
        } else {

            if (m_syn == null) {
                // Create Text To Speech Synthesizer.
                m_syn = new Synthesizer(getString(R.string.api_key));
            }

            if(tts == null) {
                tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            Log.i(TAG,"Google TTS ready");
                        } else
                            Log.e("error", "Initilization Failed!");

                    }
                });
            }
            m_syn.SetServiceStrategy(Synthesizer.ServiceStrategy.AlwaysService);

            //Voice v = new Voice("en-US", "Microsoft Server Speech Text to Speech Voice (en-US, ZiraRUS)", Voice.Gender.Female, true);
            Voice v = new Voice("he-IL", "Microsoft Server Speech Text to Speech Voice (he-IL, Asaf)", Voice.Gender.Male, true);
            //Voice v = new Voice("zh-CN", "Microsoft Server Speech Text to Speech Voice (zh-CN, HuihuiRUS)", Voice.Gender.Female, true);
            m_syn.SetVoice(v, null);

            // Use a string for speech.
            //m_syn.SpeakToAudio(getString(R.string.tts_text));

            // Use SSML for speech.
            //String text = "<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xmlns:mstts=\"http://www.w3.org/2001/mstts\" xml:lang=\"en-US\"><voice xml:lang=\"en-US\" name=\"Microsoft Server Speech Text to Speech Voice (en-US, ZiraRUS)\">You can also use SSML markup for text to speech.</voice></speak>";
            //m_syn.SpeakSSMLToAudio(text);

            //EditText editV=(EditText) findViewByID(R.id.tts_ev);

            findViewById(R.id.stop_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    m_syn.stopSound();
                }
            });

            Button bm = (Button) findViewById(R.id.play_btn);

            //Toast.makeText(MainActivity.this, "Edittext Toast1:" + text, Toast.LENGTH_LONG).show();

            bm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EditText mEdit   = (EditText)findViewById(R.id.tts_ev);
                    final String text = mEdit.getText().toString();
                    Toast.makeText(MainActivity.this, "Edittext Toast2:" + text, Toast.LENGTH_LONG).show();
                    RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
                    queue.add(detectLanguage(text));
                }
            });

            findViewById(R.id.startBubble_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final MediaProjectionManager manager
                            = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                    final Intent permissionIntent = manager.createScreenCaptureIntent();
                    startActivityForResult(permissionIntent, REQUEST_CODE_SCREEN_CAPTURE);
                    //startService(new Intent(MainActivity.this, ScreenTextService.class));

                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        if(tts != null){

            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();

    }

    private JsonRequest detectLanguage(final String selectedText){
        final JSONArray body = new JSONArray();
        try {
            JSONObject obj = new JSONObject();
            obj.put("Text", selectedText);
            body.put(obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, body.toString());

        String urlDetectLang = "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&to=de&to=it";

        return new JsonArrayRequest(Request.Method.POST, urlDetectLang,null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {

                String langCode;
                try {
                    JSONObject jsonLang = response.getJSONObject(0).getJSONObject("detectedLanguage");
                    langCode = jsonLang.getString("language");
                } catch (JSONException e) {
                    langCode = "NOLANG";
                    Log.e(TAG, e.getMessage());
                    e.printStackTrace();
                }
                playVoice(selectedText, langCode);
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
                headers.put("Ocp-Apim-Subscription-Key", getString(R.string.translator_api_key));
                return headers;
            }
        };


    }

    private void playVoice(final String selectedText, final String langCode) {
        if(langCode.equals("he")){
            m_syn.SetServiceStrategy(Synthesizer.ServiceStrategy.AlwaysService);
            Voice v = new Voice("he-IL", "Microsoft Server Speech Text to Speech Voice (he-IL, Asaf)", Voice.Gender.Male, true);
            m_syn.SetVoice(v, null);
            m_syn.SpeakToAudio(selectedText);
            localTTS=false;
        }else{
            int result = tts.setLanguage(new Locale(langCode));
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("error", "This Language is not supported");
            } else {
                tts.speak(selectedText, TextToSpeech.QUEUE_ADD, null);
                localTTS=true;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                final Intent intent = new Intent(this, ScreenTextService.class);
                intent.putExtra(ScreenTextService.EXTRA_RESULT_CODE, resultCode);
                intent.putExtras(data);
                startService(intent);
                this.finish();
            }
        }
    }
}
