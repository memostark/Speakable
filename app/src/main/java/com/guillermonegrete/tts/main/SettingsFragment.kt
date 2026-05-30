package com.guillermonegrete.tts.main


import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.preference.ListPreference
import androidx.preference.Preference

import com.guillermonegrete.tts.R

import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.guillermonegrete.tts.utils.applyTheme


class SettingsFragment : PreferenceFragmentCompat(), MenuProvider {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val menuHost: MenuHost = requireActivity()
        (menuHost as AppCompatActivity).supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val layout = super.onCreateView(inflater, container, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(layout) { _, rootInsets ->
            val insets = rootInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            listView.updatePadding(bottom = insets.bottom)
            rootInsets
        }

        return layout
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.findItem(R.id.settings_menu_item).isVisible = false
    }

    override fun onMenuItemSelected(menuItem: MenuItem) = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_main, rootKey)

        val themePreference: ListPreference? = findPreference(PREF_THEME)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            val clipboardPreference: Preference? = findPreference(PREF_CLIPBOARD_SWITCH)
            clipboardPreference?.isVisible = true
        }

        themePreference?.setOnPreferenceChangeListener { _, newValue ->
            val themeOption = newValue as String
            applyTheme(themeOption)
            true
        }

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val wordPreference: ListPreference? = findPreference(PREF_WORD_SEL)
        val sentencePreference: ListPreference? = findPreference(PREF_SENTENCE_SEL)

        wordPreference?.setOnPreferenceChangeListener { preference, _ ->
            val oldValue = sharedPrefs.getString(preference.key, WORD_SEL_DEFAULT)
            sentencePreference?.value = oldValue
            true
        }

        sentencePreference?.setOnPreferenceChangeListener { preference, _ ->
            val oldValue = sharedPrefs.getString(preference.key, SENTENCE_SEL_DEFAULT)
            wordPreference?.value = oldValue
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
        const val PREF_WORD_SEL = "word_selection_gesture"
        const val PREF_SENTENCE_SEL = "sentence_selection_gesture"

        const val WORD_SEL_DEFAULT = "tap"
        const val SENTENCE_SEL_DEFAULT = "double_tap"
    }
}
