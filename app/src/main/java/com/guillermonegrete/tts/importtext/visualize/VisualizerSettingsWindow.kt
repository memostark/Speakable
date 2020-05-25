package com.guillermonegrete.tts.importtext.visualize

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.ui.BrightnessTheme

class VisualizerSettingsWindow (
    parent: View,
    viewModel: VisualizeTextViewModel,
    width: Int,
    height: Int
): PopupWindow(width, height) {

    var callback: Callback? = null

    init{
        val context = parent.context
        val layout = LayoutInflater.from(context).inflate(R.layout.pop_up_settings, parent.rootView as ViewGroup, false)

        // Brightness settings
        layout.findViewById<Button>(R.id.white_bg_btn).setOnClickListener{ callback?.onBackgroundColorSet(BrightnessTheme.WHITE) }
        layout.findViewById<Button>(R.id.beige_bg_btn).setOnClickListener{ callback?.onBackgroundColorSet(BrightnessTheme.BEIGE) }
        layout.findViewById<Button>(R.id.black_bg_btn).setOnClickListener{ callback?.onBackgroundColorSet(BrightnessTheme.BLACK) }

        // Split view
        val splitPref: ImageButton = layout.findViewById(R.id.split_page_btn)
        val singlePagePref: ImageButton = layout.findViewById(R.id.single_page_btn)

        splitPref.setOnClickListener {
            callback?.onPageMode(true)

            splitPref.isSelected = true
            singlePagePref.isSelected = false
        }
        singlePagePref.setOnClickListener {
            callback?.onPageMode(false)

            splitPref.isSelected = false
            singlePagePref.isSelected = true
        }

        splitPref.isSelected = viewModel.hasBottomSheet
        singlePagePref.isSelected = !viewModel.hasBottomSheet

        // Languages preferences
        val languagesISO = viewModel.languagesISO

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
        val spinnerListener = SpinnerListener(viewModel, languagesISO)

        val fromMenu: Spinner = layout.findViewById(R.id.spinner_language_from)
        fromMenu.adapter = fromAdapter
        var index = languagesISO.indexOf(viewModel.languageFrom) + 1 // Increment because the list we searched is missing one element "auto"
        fromMenu.setSelection(index, false)
        fromMenu.onItemSelectedListener = spinnerListener

        val toMenu: Spinner = layout.findViewById(R.id.spinner_language_to)
        toMenu.adapter = toAdapter
        index = languagesISO.indexOf(viewModel.languageTo)
        if(index == -1) index = 15 // 15 is English, the default.
        toMenu.setSelection(index, false)
        toMenu.onItemSelectedListener = spinnerListener

        contentView = layout
    }

    class SpinnerListener(
        val viewModel: VisualizeTextViewModel,
        private val languagesISO: Array<String>
    ) : AdapterView.OnItemSelectedListener {

        override fun onNothingSelected(parent: AdapterView<*>?) {}

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            parent ?: return

            when (parent.id) {
                R.id.spinner_language_from -> {
                    viewModel.languageFrom = if (position == 0) "auto" else languagesISO[position - 1]
                }
                R.id.spinner_language_to -> viewModel.languageTo = languagesISO[position]
                else -> {}
            }
        }
    }

    interface Callback{
        fun onBackgroundColorSet(theme: BrightnessTheme)

        fun onPageMode(isSplit: Boolean)
    }

}