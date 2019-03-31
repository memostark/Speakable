package com.guillermonegrete.tts.Main;


import android.content.ClipData;
import android.content.ClipboardManager;
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
import android.widget.EditText;
import android.widget.ImageButton;

import com.guillermonegrete.tts.CustomTTS.CustomTTS;
import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.Services.ScreenTextService;
import com.guillermonegrete.tts.ThreadExecutor;
import com.guillermonegrete.tts.data.source.remote.MSTranslatorSource;
import com.guillermonegrete.tts.threading.MainThreadImpl;

import java.util.Objects;


public class TextToSpeechFragment extends Fragment implements MainTTSContract.View {

    protected static final int REQUEST_CODE_SCREEN_CAPTURE = 100;
    public static final String NORMAL_SERVICE = "startService";

    private MainTTSPresenter presenter;

    private EditText editText;
    private WebView webview;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        presenter = new MainTTSPresenter(
                ThreadExecutor.getInstance(),
                MainThreadImpl.getInstance(),
                this,
                MSTranslatorSource.getInstance(),
                CustomTTS.getInstance(getActivity().getApplicationContext())
        );
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View fragment_layout = inflater.inflate(R.layout.fragment_main_tts, container, false);

//        fragment_layout.findViewById(R.id.stop_btn).setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    // TO DO
//                }
//        });

        ImageButton playBtn = fragment_layout.findViewById(R.id.play_btn);
        ImageButton browseBtn = fragment_layout.findViewById(R.id.browse_btn);
        ImageButton pasteBtn = fragment_layout.findViewById(R.id.paste_btn);

        editText = fragment_layout.findViewById(R.id.tts_ev);

        webview = fragment_layout.findViewById(R.id.webview_wiktionary);
        webview.setWebViewClient(new HelloWebViewClient());


        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = editText.getText().toString();
                presenter.onClickReproduce(text);
            }
        });

        browseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = editText.getText().toString();
                presenter.onClickShowBrowser(text);
            }
        });

        pasteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.onClickPaste(getClipText());
            }
        });



        fragment_layout.findViewById(R.id.startBubble_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final MediaProjectionManager manager
                        = (MediaProjectionManager) Objects.requireNonNull(getActivity()).getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                if(manager != null) {
                    final Intent permissionIntent = manager.createScreenCaptureIntent();
                    startActivityForResult(permissionIntent, REQUEST_CODE_SCREEN_CAPTURE);
                }
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
    public void setDictionaryWebPage(String word) {
        webview.loadUrl("https://en.m.wiktionary.org/wiki/" + word);
    }

    @Override
    public void setEditText(String text) {
        editText.setText(text);
    }

    private String getClipText(){
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);

        if(clipboard == null) return "";

        ClipData clip = clipboard.getPrimaryClip();

        if(clip == null) return "";
        if(clip.getItemCount() <= 0) return"";

        final CharSequence pasteData = clip.getItemAt(0).getText();
        if(pasteData == null){
            return "";
        }else {
            return pasteData.toString();
        }
    }

    private class HelloWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }
}
