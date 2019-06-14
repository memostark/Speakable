package com.guillermonegrete.tts.main


import android.os.Bundle

import com.guillermonegrete.tts.R

import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_main, rootKey)

    }

    companion object {
        const val PREF_AUTO_TEST_SWITCH = "auto_tts_switch"
        const val PREF_CLIPBOARD_SWITCH = "clipboard_show_dialog"
        const val PREF_LANGUAGE_TO = "ProcessTextLangPreference"
        const val PREF_LANGUAGE_FROM = "translate_from_pref_key"
        const val PREF_WINDOW_SIZE = "window_size"
    }
}
