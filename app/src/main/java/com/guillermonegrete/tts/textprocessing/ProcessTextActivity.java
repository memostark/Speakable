/*
    Ejemplo tomado: https://github.com/Azure-Samples/Cognitive-Speech-TTS/tree/master/Android
    referencias: https://medium.com/google-developers/custom-text-selection-actions-with-action-process-text-191f792d2999
    Actividad flotante: https://stackoverflow.com/questions/33853311/how-to-create-a-floating-touchable-activity-that-still-allows-to-touch-native-co
                        http://www.androidmethlab.com/2015/09/transparent-floating-window-in-front-of.html
*/

package com.guillermonegrete.tts.textprocessing;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import android.widget.*;
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


import com.google.android.material.tabs.TabLayout;
import com.guillermonegrete.tts.BuildConfig;
import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.customviews.ButtonsPreference;
import com.guillermonegrete.tts.savedwords.SaveWordDialogFragment;
import com.guillermonegrete.tts.services.ScreenTextService;
import com.guillermonegrete.tts.data.source.WordDataSource;
import com.guillermonegrete.tts.data.source.remote.MSTranslatorSource;
import com.guillermonegrete.tts.main.SettingsFragment;
import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem;
import com.guillermonegrete.tts.data.source.remote.GooglePublicSource;
import com.guillermonegrete.tts.db.ExternalLink;
import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.main.TranslatorType;
import dagger.android.AndroidInjection;


import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.guillermonegrete.tts.savedwords.SaveWordDialogFragment.TAG_DIALOG_UPDATE_WORD;
import static com.guillermonegrete.tts.services.ScreenTextService.NO_FLOATING_ICON_SERVICE;


public class ProcessTextActivity extends FragmentActivity implements ProcessTextContract.View, SaveWordDialogFragment.Callback{

    private WiktionaryAdapter mAdapter;

    private String TAG = this.getClass().getSimpleName();

    private String mTranslation;
    private String mSelectedText;

    private Boolean mAutoTTS;

    private Words mFoundWords;

    @Inject ProcessTextContract.Presenter presenter;

    private ViewPager pager;
    private MyPageAdapter adapter;

    private ImageButton playButton;
    private ProgressBar playProgressBar;

    private View playIconsContainer;

    private SharedPreferences preferences;

    private String[] languagesISO;
    private int languageFromIndex;
    private int languagePreferenceIndex;
    private String languageFrom;
    private String languagePreferenceISO;

    private static final String LANGUAGE_PREFERENCE = "ProcessTextLangPreference";
    public static final String NO_SERVICE = "no_service";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);

        mSelectedText = getSelectedText();

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mAutoTTS = getAutoTTSPreference();
        languagePreferenceISO = getPreferenceISO();
        languageFrom = getLanguageFromPreference();

        presenter.setView(this);
        Words extraWord = getIntent().getParcelableExtra("Word");
        if(extraWord != null) presenter.start(extraWord);
        else {
            if(NO_SERVICE.equals(getIntent().getAction())) presenter.getLayout(getSelectedText(), languageFrom, languagePreferenceISO);
            else presenter.startWithService(getSelectedText(), languageFrom, languagePreferenceISO);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        presenter.stop();
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
        return preferences.getBoolean(SettingsFragment.PREF_AUTO_TEST_SWITCH, true);
    }

    private String getLanguageFromPreference(){
        String preference = preferences.getString(SettingsFragment.PREF_LANGUAGE_FROM, "auto");
        languageFromIndex = Arrays.asList(languagesISO).indexOf(preference);
        languageFromIndex++; // Increment because the list we searched is missing one element "auto"
        return  preference;
    }

    private String getPreferenceISO(){
        int englishIndex = 15;
        languagePreferenceIndex = preferences.getInt(LANGUAGE_PREFERENCE, englishIndex);
        languagesISO = getResources().getStringArray(R.array.googleTranslateLanguagesValue);
        return languagesISO[languagePreferenceIndex];
    }

    private WordDataSource getTranslatorSource(){
        int value = TranslatorType.GOOGLE_PUBLIC.getValue();
        int translatorPreference = Integer.parseInt(preferences.getString(TranslatorType.PREFERENCE_KEY, String.valueOf(value)));
        TranslatorType translatorType = TranslatorType.Companion.valueOf(translatorPreference);

        switch (translatorType){
            case GOOGLE_PUBLIC:
                return GooglePublicSource.Companion.getInstance();
            case MICROSOFT:
                return MSTranslatorSource.getInstance(BuildConfig.TranslatorApiKey);
            default:
                return GooglePublicSource.Companion.getInstance();
        }
    }

    private void setWordLayout(Words word){
        setContentView(R.layout.activity_processtext);
        final String textString = word.word;

        TextView mTextTTS = findViewById(R.id.text_tts);
        mTextTTS.setText(textString);

        TextView textViewLanguage = findViewById(R.id.text_language_code);
        if(languageFrom.equals("auto")) textViewLanguage.setVisibility(View.VISIBLE); else textViewLanguage.setVisibility(View.GONE);
        textViewLanguage.setText(word.lang);

        setPlayButton(textString);
        setLanguageFromSpinner();

        findViewById(R.id.save_icon).setOnClickListener(view -> presenter.onClickBookmark());

        if(mAutoTTS) presenter.onClickReproduce(textString);
    }



    @Override
    public void setWiktionaryLayout(Words word, List<WikiItem> items) {
        boolean isLargeWindow = preferences.getBoolean(SettingsFragment.PREF_WINDOW_SIZE, ButtonsPreference.DEFAULT_VALUE);
        if(isLargeWindow) setCenterDialog(); else  setBottomDialog();
        setWordLayout(word);
        mAdapter = new WiktionaryAdapter(this, items);

        mFoundWords = word;

        if(isLargeWindow) createViewPager(); else createSmallViewPager();
    }



    @Override
    public void setSavedWordLayout(final Words word) {
        setBottomDialog();
        mFoundWords = word;

        setWordLayout(word);
        createSmallViewPager();

        setSavedWordToolbar(word);
        languagePreferenceIndex = -1; // Indicates spinner not visible

    }

    @Override
    public void setDictWithSaveWordLayout(Words word, List<WikiItem> items) {
        setWiktionaryLayout(word, items);
        setSavedWordToolbar(word);
        languagePreferenceIndex = -1; // Indicates spinner not visible
    }

    @Override
    public void setTranslationLayout(Words word) {
        setBottomDialog();
        mFoundWords = word;
        setWordLayout(word);
        createSmallViewPager();
    }

    @Override
    public void setSentenceLayout(Words word) {
        setBottomDialog();
        String text = word.word;
        setContentView(R.layout.activity_process_sentence);
        TextView mTextTTS = findViewById(R.id.text_tts);
        mTextTTS.setText(text);
        TextView mTextTranslation = findViewById(R.id.text_translation);
        mTextTranslation.setText(word.definition);
        TextView textLanguage = findViewById(R.id.text_language_code);
        if(languageFrom.equals("auto")) textLanguage.setVisibility(View.VISIBLE); else textLanguage.setVisibility(View.GONE);
        textLanguage.setText(word.lang);
        setPlayButton(text);

        setLanguageFromSpinner();
        setSpinner();

        if(mAutoTTS) presenter.onClickReproduce(text);
    }

    @Override
    public void setExternalDictionary(List<ExternalLink> links) {
        adapter = new MyPageAdapter(getSupportFragmentManager());
        if(mAdapter != null) adapter.addFragment(DefinitionFragment.newInstance(mAdapter));
        TranslationFragment translationFragment = TranslationFragment.newInstance(mFoundWords, languagePreferenceIndex);
        translationFragment.setListener(translationFragListener);
        adapter.addFragment(translationFragment);
        adapter.addFragment(ExternalLinksFragment.newInstance(mSelectedText, (ArrayList<ExternalLink>) links));
        pager.setAdapter(adapter);
    }

    @Override
    public void showSaveDialog(Words word) {
        DialogFragment dialogFragment;
        dialogFragment = SaveWordDialogFragment.newInstance(word);
        dialogFragment.show(getSupportFragmentManager(), "New word process");
    }

    @Override
    public void showDeleteDialog(final String word) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to delete this word?")
                .setPositiveButton("Yes", (dialog, id) -> {
                    presenter.onClickDeleteWord(word);
                    dialog.dismiss();

                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());

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
    public void showLanguageNotAvailable() {
        if(playIconsContainer != null) { // Why do you need to check this?
            playIconsContainer.setVisibility(View.GONE);
            Toast.makeText(this, "Language not available for TTS", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void showLoadingTTS() {
        playProgressBar.setVisibility(View.VISIBLE);
        playButton.setVisibility(View.GONE);
    }

    @Override
    public void showPlayIcon() {
        playButton.setImageResource(R.drawable.ic_volume_up_black_24dp);
    }

    @Override
    public void showStopIcon() {
        playButton.setImageResource(R.drawable.ic_stop_black_24dp);
        playProgressBar.setVisibility(View.GONE);
        playButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void updateTranslation(String translation) {
        TextView textLanguage = findViewById(R.id.text_language_code);
        if(languageFrom.equals("auto")) textLanguage.setVisibility(View.VISIBLE); else textLanguage.setVisibility(View.GONE);

        if(pager != null){
            int fragIndex = pager.getCurrentItem();
            Fragment fragment = adapter.getItem(fragIndex);
            if(fragment instanceof TranslationFragment) ((TranslationFragment)fragment).updateTranslation(translation);
        }else {
            TextView translationTextView = findViewById(R.id.text_translation);
            translationTextView.setText(translation);
        }
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
        Window window = getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setAttributes(wlp);
    }

    private void setBottomDialog(){
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.dimAmount = 0;
        wlp.flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        wlp.gravity = Gravity.BOTTOM;
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setAttributes(wlp);
    }

    private void setSavedWordToolbar(final Words word){
        ImageButton saveIcon = findViewById(R.id.save_icon);
        saveIcon.setImageResource(R.drawable.ic_bookmark_black_24dp);

        ImageButton editIcon = findViewById(R.id.edit_icon);
        editIcon.setVisibility(View.VISIBLE);
        editIcon.setOnClickListener(view -> {
            DialogFragment dialogFragment = SaveWordDialogFragment.newInstance(word);
            dialogFragment.show(getSupportFragmentManager(), TAG_DIALOG_UPDATE_WORD);
        });

    }

    private void setPlayButton(final String text){
        playButton = findViewById(R.id.play_tts_icon);
        playButton.setOnClickListener(view -> presenter.onClickReproduce(text));

        playProgressBar = findViewById(R.id.play_loading_icon);
        playIconsContainer = findViewById(R.id.play_icons_container);
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

    private void setLanguageFromSpinner(){
        Spinner spinner = findViewById(R.id.spinner_language_from_code);
        setSpinnerPopUpHeight(spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.googleTranslateLangsWithAutoValue, R.layout.spinner_layout_end);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setSelection(languageFromIndex, false);
        spinner.setOnItemSelectedListener(new SpinnerListener());
    }

    private void setSpinner(){
        Spinner spinner = findViewById(R.id.translate_to_spinner);
        setSpinnerPopUpHeight(spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.googleTranslateLanguagesArray, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        spinner.setSelection(languagePreferenceIndex, false);
        spinner.setOnItemSelectedListener(new SpinnerListener());
    }

    // Taken from: https://stackoverflow.com/questions/20597584/how-to-limit-the-height-of-spinner-drop-down-view-in-android
    private void setSpinnerPopUpHeight(Spinner spinner){
        try {
            Field popup = Spinner.class.getDeclaredField("mPopup");
            popup.setAccessible(true);

            // Get private mPopup member variable and try cast to ListPopupWindow
            android.widget.ListPopupWindow popupWindow = (android.widget.ListPopupWindow) popup.get(spinner);

            // Set popupWindow height to 500px
            popupWindow.setHeight(300);
        }
        catch (NoClassDefFoundError | ClassCastException | NoSuchFieldException | IllegalAccessException e) {
            // silently fail...
        }
    }

    @Override
    public void onWordSaved(final Words word) {
        presenter.onClickSaveWord(word);
        ImageButton saveIcon = findViewById(R.id.save_icon);
        saveIcon.setImageResource(R.drawable.ic_bookmark_black_24dp);

        ImageButton editIcon = findViewById(R.id.edit_icon);
        editIcon.setVisibility(View.VISIBLE);
        editIcon.setOnClickListener(view -> {
            DialogFragment dialogFragment = SaveWordDialogFragment.newInstance(word);
            dialogFragment.show(getSupportFragmentManager(), TAG_DIALOG_UPDATE_WORD);
        });
    }

    /**
     *  Listener for translation fragment when using ViewPager
     */
    private TranslationFragment.Listener translationFragListener = new TranslationFragment.Listener() {
        @Override
        public void onItemSelected(int position) {
            languagePreferenceISO = languagesISO[position];
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(LANGUAGE_PREFERENCE, position);
            editor.apply();
            presenter.onLanguageSpinnerChange(languageFrom, languagePreferenceISO);
        }
    };

    /**
     *  Listener when layout is for a sentence
     */
    class SpinnerListener implements AdapterView.OnItemSelectedListener{

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            SharedPreferences.Editor editor = preferences.edit();
            switch (parent.getId()){
                case R.id.spinner_language_from_code:
                    if(position == 0) languageFrom = "auto";
                    else languageFrom = languagesISO[position - 1];
                    editor.putString(SettingsFragment.PREF_LANGUAGE_FROM, languageFrom);
                    editor.apply();
                    presenter.onLanguageSpinnerChange(languageFrom, languagePreferenceISO);
                    break;
                case R.id.translate_to_spinner:
                    languagePreferenceISO = languagesISO[position];
                    editor.putInt(LANGUAGE_PREFERENCE, position);
                    editor.apply();
                    presenter.onLanguageSpinnerChange(languageFrom, languagePreferenceISO);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
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

