package com.guillermonegrete.tts.main


import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference

import com.guillermonegrete.tts.R

import androidx.preference.PreferenceFragmentCompat
import com.guillermonegrete.tts.utils.applyTheme


class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.settings_menu_item).isVisible = false
        super.onPrepareOptionsMenu(menu)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_main, rootKey)

        val themePreference: ListPreference? = findPreference(PREF_THEME)

        themePreference?.setOnPreferenceChangeListener { _, newValue ->
            val themeOption = newValue as String
            applyTheme(themeOption)
            true
        }
    }

    companion object {
        const val PREF_AUTO_TEST_SWITCH = "auto_tts_switch"
        const val PREF_CLIPBOARD_SWITCH = "clipboard_show_dialog"
        const val PREF_LANGUAGE_TO = "ProcessTextLangPreference"
        const val PREF_LANGUAGE_FROM = "translate_from_pref_key"
        const val PREF_WINDOW_SIZE = "window_size"
        const val PREF_THEME = "theme_pref_key"
    }
}
