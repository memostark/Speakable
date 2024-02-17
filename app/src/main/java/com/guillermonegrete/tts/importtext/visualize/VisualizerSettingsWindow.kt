package com.guillermonegrete.tts.importtext.visualize

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.databinding.PopUpSettingsBinding
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
        val binding = PopUpSettingsBinding.inflate(LayoutInflater.from(context), parent.rootView as ViewGroup, false)

        // Brightness settings
        binding.whiteBgBtn.setOnClickListener { callback.onBackgroundColorSet(BrightnessTheme.WHITE) }
        binding.beigeBgBtn.setOnClickListener { callback.onBackgroundColorSet(BrightnessTheme.BEIGE) }
        binding.blackBgBtn.setOnClickListener { callback.onBackgroundColorSet(BrightnessTheme.BLACK) }

        // Split view
        val checkedItemId = if(hasBottomSheet) R.id.split_page_btn else R.id.single_page_btn
        binding.pageToggleContainer.check(checkedItemId)

        binding.pageToggleContainer.addOnButtonCheckedListener { _, checkedId, isChecked ->
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

        val fromMenu = binding.spinnerLanguageFrom
        fromMenu.adapter = fromAdapter
        var index = languagesISO.indexOf(languageFrom) + 1 // Increment because the list we searched is missing one element "auto"
        fromMenu.setSelection(index, false)
        fromMenu.onItemSelectedListener = spinnerListener

        val toMenu = binding.spinnerLanguageTo
        toMenu.adapter = toAdapter
        index = languagesISO.indexOf(languageTo)
        if(index == -1) index = 15 // 15 is English, the default.
        toMenu.setSelection(index, false)
        toMenu.onItemSelectedListener = spinnerListener

        contentView = binding.root
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
