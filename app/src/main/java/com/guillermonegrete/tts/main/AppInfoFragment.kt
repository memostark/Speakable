package com.guillermonegrete.tts.main

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.guillermonegrete.tts.BuildConfig
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.databinding.FragmentAppInfoBinding
import java.util.*

/**
 * A simple [DialogFragment] for showing information about the application.
 * Extending the AppCompat version because it supports the applied styles.
 */
class AppInfoFragment : AppCompatDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.DialogFragmentTitle)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setTitle(context?.getString(R.string.about))
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentAppInfoBinding.inflate(inflater, container, false)
        binding.version.text = getVersion()
        return binding.root
    }

    private fun getVersion(): String {
        val buildType = if (BuildConfig.BUILD_TYPE == "release") "" else " (${BuildConfig.VERSION_CODE}) ${BuildConfig.BUILD_TYPE.toUpperCase(Locale.getDefault())}"
        val fullText = BuildConfig.VERSION_NAME + buildType
        return getString(R.string.app_version, fullText)
    }
}
