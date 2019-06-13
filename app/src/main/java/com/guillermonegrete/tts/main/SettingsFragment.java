package com.guillermonegrete.tts.main;


import android.os.Bundle;

import com.guillermonegrete.tts.R;

import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {
    public static final String PREF_AUTO_TEST_SWITCH = "auto_tts_switch";
    public static final String PREF_CLIPBOARD_SWITCH = "clipboard_show_dialog";
    public static final String PREF_LANGUAGE_TO = "ProcessTextLangPreference";
    public static final String PREF_LANGUAGE_FROM = "translate_from_pref_key";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_main, rootKey);

    }
}
