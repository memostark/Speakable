/*
    Ejemplo tomado: https://github.com/Azure-Samples/Cognitive-Speech-TTS/tree/master/Android
    referencias: https://medium.com/google-developers/custom-text-selection-actions-with-action-process-text-191f792d2999
    Actividad flotante: https://stackoverflow.com/questions/33853311/how-to-create-a-floating-touchable-activity-that-still-allows-to-touch-native-co
                        http://www.androidmethlab.com/2015/09/transparent-floating-window-in-front-of.html
*/

package com.guillermonegrete.tts.TextProcessing;


import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;


import com.google.android.material.tabs.TabLayout;
import com.guillermonegrete.tts.CustomTTS.CustomTTS;
import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.SavedWords.SaveWordDialogFragment;
import com.guillermonegrete.tts.Services.ScreenTextService;
import com.guillermonegrete.tts.Main.SettingsFragment;
import com.guillermonegrete.tts.TextProcessing.domain.model.WikiItem;
import com.guillermonegrete.tts.ThreadExecutor;
import com.guillermonegrete.tts.data.source.DictionaryRepository;
import com.guillermonegrete.tts.data.source.WordRepository;
import com.guillermonegrete.tts.data.source.local.ExternalLinksDataSource;
import com.guillermonegrete.tts.data.source.local.WordLocalDataSource;
import com.guillermonegrete.tts.data.source.remote.MSTranslatorSource;
import com.guillermonegrete.tts.data.source.remote.WiktionarySource;
import com.guillermonegrete.tts.db.ExternalLink;
import com.guillermonegrete.tts.db.ExternalLinksDatabase;
import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.db.WordsDatabase;
import com.guillermonegrete.tts.threading.MainThreadImpl;


import java.util.ArrayList;
import java.util.List;

import static com.guillermonegrete.tts.SavedWords.SaveWordDialogFragment.TAG_DIALOG_UPDATE_WORD;
import static com.guillermonegrete.tts.Services.ScreenTextService.NO_FLOATING_ICON_SERVICE;


public class ProcessTextActivity extends FragmentActivity implements ProcessTextContract.View, SaveWordDialogFragment.Callback{

    private WiktionaryAdapter mAdapter;

    private String TAG = this.getClass().getSimpleName();

    private String mTranslation;
    private String mSelectedText;

    private Boolean mAutoTTS;

    private Words mFoundWords;

    private ProcessTextContract.Presenter presenter;

    private ViewPager pager;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);

        mSelectedText = getSelectedText();

        mAutoTTS = getAutoTTSPreference();

        presenter = new ProcessTextPresenter(ThreadExecutor.getInstance(),
                MainThreadImpl.getInstance(),
                this,
                WordRepository.getInstance(MSTranslatorSource.getInstance(), WordLocalDataSource.getInstance(WordsDatabase.getDatabase(getApplicationContext()).wordsDAO())),
                DictionaryRepository.getInstance(WiktionarySource.getInstance()),
                ExternalLinksDataSource.getInstance(ExternalLinksDatabase.getDatabase(getApplicationContext()).externalLinksDAO()),
                CustomTTS.getInstance(getApplicationContext()));

        Words extraWord = getIntent().getParcelableExtra("Word");
        if(extraWord != null) presenter.start(extraWord);
        else {
            presenter.start(getSelectedText());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.destroy();
    }

    private String getSelectedText(){
        Intent intent = getIntent();
        final CharSequence selected_text = intent
                .getCharSequenceExtra("android.intent.extra.PROCESS_TEXT");
        return selected_text.toString();
    }

    private boolean getAutoTTSPreference(){
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getBoolean(SettingsFragment.PREF_AUTO_TEST_SWITCH, true);
    }

    private void setWordLayout(final String textString){
        setContentView(R.layout.activity_processtext);

        TextView mTextTTS = findViewById(R.id.text_tts);
        mTextTTS.setText(textString);


        findViewById(R.id.play_tts_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // tts.speak(textString);
                presenter.onClickReproduce(textString);
            }
        });

        findViewById(R.id.save_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.onClickBookmark();

            }
        });

        if(mAutoTTS) presenter.onClickReproduce(textString);
    }



    @Override
    public void setWiktionaryLayout(Words word, List<WikiItem> items) {

        setCenterDialog();
        setWordLayout(getSelectedText());
        mAdapter = new WiktionaryAdapter(this, items);

        mFoundWords = word;

        createViewPager();
    }



    @Override
    public void setSavedWordLayout(final Words word) {
        setBottomDialog();
        mFoundWords = word;

        final String word_text = word.word;
        setWordLayout(word_text);
        createSmallViewPager();

        setSavedWordToolbar(word);

    }

    @Override
    public void setDictWithSaveWordLayout(Words word, List<WikiItem> items) {
        setWiktionaryLayout(word, items);
        setSavedWordToolbar(word);
    }

    @Override
    public void setTranslationLayout(Words word) {
        setBottomDialog();
        mFoundWords = word;
        setWordLayout(word.word);
        createSmallViewPager();
    }

    @Override
    public void setSentenceLayout(Words word) {
        setBottomDialog();
        setContentView(R.layout.activity_process_sentence);
        TextView mTextTTS = findViewById(R.id.text_tts);
        mTextTTS.setText(word.word);
        TextView mTextTranslation = findViewById(R.id.text_translation);
        mTextTranslation.setText(word.definition);
        presenter.onClickReproduce(word.word);
    }

    @Override
    public void setExternalDictionary(List<ExternalLink> links) {
        MyPageAdapter adapter = new MyPageAdapter(getSupportFragmentManager());
        if(mAdapter != null) adapter.addFragment(DefinitionFragment.newInstance(mAdapter));
        adapter.addFragment(TranslationFragment.newInstance(mFoundWords));
        adapter.addFragment(ExternalLinksFragment.newInstance(mSelectedText, (ArrayList<ExternalLink>) links));
        pager.setAdapter(adapter);
    }

    @Override
    public void showSaveDialog(Words word) {
        DialogFragment dialogFragment;
        dialogFragment = SaveWordDialogFragment.newInstance(word.word, word.lang, word.definition);
        dialogFragment.show(getSupportFragmentManager(), "New word process");
    }

    @Override
    public void showDeleteDialog(final String word) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to delete this word?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        presenter.onClickDeleteWord(word);
                        dialog.dismiss();

                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        builder.create().show();
    }

    @Override
    public void showWordDeleted() {
        ImageButton saveIcon = findViewById(R.id.save_icon);
        saveIcon.setImageResource(R.drawable.ic_bookmark_border_black_24dp);

        ImageButton editIcon = findViewById(R.id.edit_icon);
        editIcon.setVisibility(View.GONE);
    }

    @Override
    public void startService() {
        final Intent intentService = new Intent(this, ScreenTextService.class);
        intentService.setAction(NO_FLOATING_ICON_SERVICE);
        startService(intentService);
    }


    @Override
    public void setPresenter(ProcessTextContract.Presenter presenter) {
        this.presenter = presenter;
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void setCenterDialog(){
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        WindowManager.LayoutParams wlp = getWindow().getAttributes();
        wlp.flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        getWindow().setAttributes(wlp);
    }

    private void setBottomDialog(){
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        WindowManager.LayoutParams wlp = getWindow().getAttributes();
        wlp.dimAmount = 0;
        wlp.flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        wlp.gravity = Gravity.BOTTOM;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setAttributes(wlp);
    }

    private void setSavedWordToolbar(final Words word){
        ImageButton saveIcon = findViewById(R.id.save_icon);
        saveIcon.setImageResource(R.drawable.ic_bookmark_black_24dp);

        ImageButton editIcon = findViewById(R.id.edit_icon);
        editIcon.setVisibility(View.VISIBLE);
        editIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment dialogFragment = SaveWordDialogFragment.newInstance(
                        word.word,
                        word.lang,
                        word.definition);
                dialogFragment.show(getSupportFragmentManager(), TAG_DIALOG_UPDATE_WORD);
            }
        });

    }

    private void createViewPager(){
        pager =  findViewById(R.id.process_view_pager);
        TabLayout tabLayout = findViewById(R.id.pager_menu_dots);
        tabLayout.setupWithViewPager(pager, true);
    }

    private void createSmallViewPager(){
        pager = findViewById(R.id.process_view_pager);
        ViewGroup.LayoutParams params = pager.getLayoutParams();
        params.height = 250;
        pager.setLayoutParams(params);
        TabLayout tabLayout = findViewById(R.id.pager_menu_dots);
        tabLayout.setupWithViewPager(pager, true);
    }

    @Override
    public void onWordSaved(final Words word) {
        presenter.onClickSaveWord(word);
        ImageButton saveIcon = findViewById(R.id.save_icon);
        saveIcon.setImageResource(R.drawable.ic_bookmark_black_24dp);

        ImageButton editIcon = findViewById(R.id.edit_icon);
        editIcon.setVisibility(View.VISIBLE);
        editIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment dialogFragment = SaveWordDialogFragment.newInstance(
                        word.word,
                        word.lang,
                        word.definition);
                dialogFragment.show(getSupportFragmentManager(), TAG_DIALOG_UPDATE_WORD);
            }
        });
    }


    private class MyPageAdapter extends FragmentPagerAdapter{

        private List<Fragment> fragments = new ArrayList<>();

        void addFragment(Fragment fragment) {
            fragments.add(fragment);
        }

        MyPageAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }
    }
}

