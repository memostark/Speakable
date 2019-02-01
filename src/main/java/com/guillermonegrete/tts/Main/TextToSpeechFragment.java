package com.guillermonegrete.tts.Main;


import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;

import com.guillermonegrete.tts.CustomTTS.CustomTTS;
import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.Services.ScreenTextService;

public class TextToSpeechFragment extends Fragment {

    protected static final int REQUEST_CODE_SCREEN_CAPTURE = 100;
    public static final String NORMAL_SERVICE = "startService";

    private CustomTTS tts;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(tts == null) {
            tts = new CustomTTS(getActivity());
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View fragment_layout = inflater.inflate(R.layout.fragment_main_tts, container, false);

        fragment_layout.findViewById(R.id.stop_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // TO DO
                }
        });

        // Use a string for speech.
        //m_syn.SpeakToAudio(getString(R.string.tts_text));

        // Use SSML for speech.
        //String text = "<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xmlns:mstts=\"http://www.w3.org/2001/mstts\" xml:lang=\"en-US\"><voice xml:lang=\"en-US\" name=\"Microsoft Server Speech Text to Speech Voice (en-US, ZiraRUS)\">You can also use SSML markup for text to speech.</voice></speak>";
        //m_syn.SpeakSSMLToAudio(text);

        Button playBtn = (Button) fragment_layout.findViewById(R.id.play_btn);
        final WebView webview = (WebView) fragment_layout.findViewById(R.id.webview_wiktionary);
        webview.setWebViewClient(new HelloWebViewClient());

        // TODO fix audio not playing, implemente tts listener
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText mEdit   = (EditText)fragment_layout.findViewById(R.id.tts_ev);
                final String text = mEdit.getText().toString();
                tts.determineLanguage(text);
                webview.loadUrl("https://en.m.wiktionary.org/wiki/"+text);
            }
        });

        fragment_layout.findViewById(R.id.startBubble_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final MediaProjectionManager manager
                        = (MediaProjectionManager)getActivity().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                final Intent permissionIntent = manager.createScreenCaptureIntent();
                startActivityForResult(permissionIntent, REQUEST_CODE_SCREEN_CAPTURE);
            }
        });

        return fragment_layout;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                final Intent intent = new Intent(getActivity(), ScreenTextService.class);
                intent.setAction(NORMAL_SERVICE);
                intent.putExtra(ScreenTextService.EXTRA_RESULT_CODE, resultCode);
                intent.putExtras(data);
                getActivity().startService(intent);
                getActivity().finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        if(tts != null) tts.finishTTS();
        super.onDestroy();
    }

    private class HelloWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }
}
