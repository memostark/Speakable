package com.guillermonegrete.tts.importtext.visualize

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.webkit.URLUtil
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavArgument
import androidx.navigation.fragment.NavHostFragment
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.databinding.ActivityVisualizeTextBinding
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class VisualizeTextActivity: AppCompatActivity() {

    private var visualizerFragment: VisualizeTextFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        val binding = ActivityVisualizeTextBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.visualizer_fragment_container) as NavHostFragment
        val navController = navHostFragment.navController

        val graph = navController.navInflater.inflate(R.navigation.visualize_text)
        val text = getSharedText()
        if (URLUtil.isValidUrl(text)) {
            graph.setStartDestination(R.id.webReaderFragmentDest)
            graph.addArgument("link", NavArgument.Builder().setDefaultValue(text).build())
        } else {
            graph.setStartDestination(R.id.visualizeFragmentDest)
        }

        navController.graph = graph

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            var insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            if (navController.currentDestination?.id == R.id.webReaderFragmentDest) v.updatePadding(left = insets.left, right = insets.right)
            windowInsets
        }
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
