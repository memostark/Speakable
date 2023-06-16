package com.guillermonegrete.tts.importtext.visualize

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.button.MaterialButtonToggleGroup
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.ui.BrightnessTheme

class VisualizerSettingsWindow(
    parent: View,
    width: Int,
    height: Int,
    hasBottomSheet: Boolean,
    languagesISO: Array<String>,
    languageFrom: String,
    languageTo: String,
    callback: Callback
): PopupWindow(width, height) {

    init{
        val context = parent.context
        val layout = LayoutInflater.from(context).inflate(R.layout.pop_up_settings, parent.rootView as ViewGroup, false)

        // Brightness settings
        layout.findViewById<Button>(R.id.white_bg_btn).setOnClickListener{ callback.onBackgroundColorSet(BrightnessTheme.WHITE) }
        layout.findViewById<Button>(R.id.beige_bg_btn).setOnClickListener{ callback.onBackgroundColorSet(BrightnessTheme.BEIGE) }
        layout.findViewById<Button>(R.id.black_bg_btn).setOnClickListener{ callback.onBackgroundColorSet(BrightnessTheme.BLACK) }

        // Split view
        val pageModeToggle: MaterialButtonToggleGroup = layout.findViewById(R.id.page_toggle_container)

        val checkedItemId = if(hasBottomSheet) R.id.split_page_btn else R.id.single_page_btn
        pageModeToggle.check(checkedItemId)

        pageModeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            when(checkedId){
                R.id.single_page_btn -> if(isChecked) callback.onPageMode(false)
                R.id.split_page_btn -> if(isChecked) callback.onPageMode(true)
            }
        }

        // Languages preferences
        val fromAdapter = ArrayAdapter.createFromResource(
            context,
            R.array.googleTranslateLangsWithAutoArray,
            android.R.layout.simple_spinner_dropdown_item
        )
        val toAdapter = ArrayAdapter.createFromResource(
            context,
            R.array.googleTranslateLanguagesArray,
            android.R.layout.simple_spinner_dropdown_item
        )
        val spinnerListener = SpinnerListener(callback)

        val fromMenu: Spinner = layout.findViewById(R.id.spinner_language_from)
        fromMenu.adapter = fromAdapter
        var index = languagesISO.indexOf(languageFrom) + 1 // Increment because the list we searched is missing one element "auto"
        fromMenu.setSelection(index, false)
        fromMenu.onItemSelectedListener = spinnerListener

        val toMenu: Spinner = layout.findViewById(R.id.spinner_language_to)
        toMenu.adapter = toAdapter
        index = languagesISO.indexOf(languageTo)
        if(index == -1) index = 15 // 15 is English, the default.
        toMenu.setSelection(index, false)
        toMenu.onItemSelectedListener = spinnerListener

        contentView = layout
    }

    class SpinnerListener(private val callback: Callback): AdapterView.OnItemSelectedListener {

        override fun onNothingSelected(parent: AdapterView<*>?) {}

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            parent ?: return

            when (parent.id) {
                R.id.spinner_language_from -> callback.onLanguageFromChanged(position)
                R.id.spinner_language_to -> callback.onLanguageToChanged(position)
                else -> {}
            }
        }
    }

    interface Callback{
        fun onBackgroundColorSet(theme: BrightnessTheme)

        fun onPageMode(isSplit: Boolean)

        fun onLanguageToChanged(position: Int)

        fun onLanguageFromChanged(position: Int)
    }

}