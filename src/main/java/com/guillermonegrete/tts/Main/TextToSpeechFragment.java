package com.guillermonegrete.tts.Main;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.guillermonegrete.tts.CustomTTS.CustomTTS;
import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.Services.ScreenTextService;
import com.guillermonegrete.tts.ThreadExecutor;
import com.guillermonegrete.tts.data.source.remote.MSTranslatorSource;
import com.guillermonegrete.tts.threading.MainThreadImpl;

import java.util.Objects;

import static com.guillermonegrete.tts.Services.ScreenTextService.NORMAL_SERVICE;
import static com.guillermonegrete.tts.Services.ScreenTextService.NO_FLOATING_ICON_SERVICE;


public class TextToSpeechFragment extends Fragment implements MainTTSContract.View {

    protected static final int REQUEST_CODE_SCREEN_CAPTURE = 100;

    private MainTTSPresenter presenter;

    private EditText editText;
    private WebView webview;
    private BottomSheetBehavior bottomSheetBehavior;

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

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View fragment_layout = inflater.inflate(R.layout.fragment_main_tts, container, false);

        LinearLayout bottomSheet = fragment_layout.findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        ImageButton playBtn = fragment_layout.findViewById(R.id.play_btn);
        ImageButton browseBtn = fragment_layout.findViewById(R.id.browse_btn);
        ImageButton pasteBtn = fragment_layout.findViewById(R.id.paste_btn);

        editText = fragment_layout.findViewById(R.id.tts_ev);
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        webview = fragment_layout.findViewById(R.id.webview_wiktionary);
        webview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                view.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });
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
                hideKeyboard();
                String text = editText.getText().toString();
                presenter.onClickShowBrowser(text);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
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
                presenter.onStartOverlayMode();
            }
        });

        fragment_layout.findViewById(R.id.clipboard_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.onStartClipboardMode();
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

    @Override
    public void startClipboardService() {
        final Intent intent = new Intent(getActivity(), ScreenTextService.class);
        intent.setAction(NO_FLOATING_ICON_SERVICE);
        getActivity().startService(intent);
    }

    @Override
    public void startOverlayService() {
        final MediaProjectionManager manager
                = (MediaProjectionManager) Objects.requireNonNull(getActivity()).getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if(manager != null) {
            final Intent permissionIntent = manager.createScreenCaptureIntent();
            startActivityForResult(permissionIntent, REQUEST_CODE_SCREEN_CAPTURE);
        }
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

    private void hideKeyboard(){
        Activity context = getActivity();
        if(context != null){
            InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                View focusedView = context.getCurrentFocus();
                if (focusedView != null) {
                    inputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                }
            }
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
