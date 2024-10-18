package com.guillermonegrete.tts.importtext.visualize

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.view.ContextThemeWrapper
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.databinding.PopUpSettingsBinding
import com.guillermonegrete.tts.ui.BrightnessTheme

class VisualizerSettingsWindow(
    parent: View,
    theme: Int,
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
        val themeContext = ContextThemeWrapper(context, theme)
        val binding = PopUpSettingsBinding.inflate(LayoutInflater.from(themeContext), parent.rootView as ViewGroup, false)

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
        val fromMenu = binding.pickLanguageFrom
        var index = languagesISO.indexOf(languageFrom) + 1 // Increment because the list we searched is missing one element "auto"
        fromMenu.setText(fromMenu.adapter.getItem(index).toString(), false)
        fromMenu.setOnItemClickListener { _, _, position, _ ->
            callback.onLanguageFromChanged(position)
        }

        val toMenu = binding.pickLanguageTo
        index = languagesISO.indexOf(languageTo)
        if(index == -1) index = 15 // 15 is English, the default.
        toMenu.setText(toMenu.adapter.getItem(index).toString(), false)
        toMenu.setOnItemClickListener { _, _, position, _ ->
            callback.onLanguageToChanged(position)
        }

        contentView = binding.root
    }

    interface Callback{
        fun onBackgroundColorSet(theme: BrightnessTheme)

        fun onPageMode(isSplit: Boolean)

        fun onLanguageToChanged(position: Int)

        fun onLanguageFromChanged(position: Int)
    }

}
