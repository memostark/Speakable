package com.guillermonegrete.tts.main


import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.preference.ListPreference
import androidx.preference.Preference

import com.guillermonegrete.tts.R

import androidx.preference.PreferenceFragmentCompat
import com.guillermonegrete.tts.utils.applyTheme


class SettingsFragment : PreferenceFragmentCompat(), MenuProvider {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        return super.onCreateView(inflater, container, savedInstanceState)
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
