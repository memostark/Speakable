package com.guillermonegrete.tts.importtext.visualize

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MotionEvent
import android.webkit.URLUtil
import androidx.activity.viewModels
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.fragment.NavHostFragment
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.databinding.ActivityVisualizeTextBinding
import com.guillermonegrete.tts.ui.BrightnessTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class VisualizeTextActivity: AppCompatActivity() {

    private val viewModel: VisualizeTextViewModel by viewModels()

    private var visualizerFragment: VisualizeTextFragment? = null

    @Inject lateinit var preferences: SharedPreferences
    @Inject lateinit var brightnessTheme: BrightnessTheme
    @StyleRes private var themeRes = R.style.AppMaterialTheme_Black

    private val noteSheetVisible = mutableStateOf(false)

    private var splitterCreated = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = getSharedText()
        val binding = ActivityVisualizeTextBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.main_fragment_container) as NavHostFragment
        if (URLUtil.isValidUrl(text)) {
            val navController = navHostFragment.navController
            val inflater = navController.navInflater
            val graph = inflater.inflate(R.navigation.importtext)
            graph.setStartDestination(R.id.webReaderFragment)
            navController.setGraph(graph, bundleOf("link" to text))
            return
        }

        val fragment = VisualizeTextFragment()
        supportFragmentManager.beginTransaction()
            .add(R.id.main_fragment_container, fragment).commit()
        visualizerFragment = fragment
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (noteSheetVisible.value) {
            noteSheetVisible.value = false
        } else {
            super.onBackPressed()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val fragment = visualizerFragment ?: return super.dispatchTouchEvent(ev)
        return fragment.dispatchTouchEvent(ev) || super.dispatchTouchEvent(ev)
    }

    /*override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(hasFocus && viewModel.fullScreen) {

            // Only hide the UI when page splitter has been created to avoid incorrect size measuring
            if(splitterCreated) hideSystemUi()
        }
    }*/

    private fun hideSystemUi(){
        val decorView = window.decorView
        val controllerCompat = WindowCompat.getInsetsController(window, decorView)
        controllerCompat.hide(WindowInsetsCompat.Type.systemBars())
        controllerCompat.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        actionBar?.hide()
    }

    private fun getSharedText(): String {
        val clipData = intent.clipData
        if (clipData != null && clipData.itemCount > 0) {
            val size = clipData.itemCount
            val stringBuilder = StringBuilder()
            for (i in 0 until size) {
                val item = clipData.getItemAt(i)
                stringBuilder.append(item.text)
            }
            return stringBuilder.toString()
        }
        return ""
    }
}
