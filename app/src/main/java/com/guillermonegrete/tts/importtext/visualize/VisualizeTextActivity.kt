package com.guillermonegrete.tts.importtext.visualize

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.webkit.URLUtil
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.databinding.ActivityVisualizeTextBinding
import com.guillermonegrete.tts.webreader.WebReaderFragment
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class VisualizeTextActivity: AppCompatActivity() {

    private var visualizerFragment: VisualizeTextFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        val text = getSharedText()
        val binding = ActivityVisualizeTextBinding.inflate(layoutInflater)
        val fragment: Fragment
        if (URLUtil.isValidUrl(text)) {
            fragment = WebReaderFragment()
            fragment.arguments = bundleOf("link" to text)
        } else {
            fragment = VisualizeTextFragment()
            visualizerFragment = fragment
        }

        supportFragmentManager.beginTransaction()
            .add(R.id.main_fragment_container, fragment).commitNow()

        // Set content view after the fragment was committed to make sure the fragment's theme is applied correctly
        setContentView(binding.root)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val fragment = visualizerFragment ?: return super.dispatchTouchEvent(ev)
        return fragment.dispatchTouchEvent(ev) || super.dispatchTouchEvent(ev)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        val fragment = visualizerFragment
        if(hasFocus && fragment != null) {

            fragment.onWindowFocusChanged(true)
        }
    }

    private fun getSharedText() = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""

    /**
     * In case the child fragment has a different theme than the app theme, this allows to update the bar colors.
     */
    fun themeUpdated(isDark: Boolean) {
        val style = if (isDark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(style, style)
    }
}
