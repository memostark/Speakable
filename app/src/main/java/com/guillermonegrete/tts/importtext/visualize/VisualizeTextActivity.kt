package com.guillermonegrete.tts.importtext.visualize

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Selection
import android.text.Spannable
import android.view.*
import android.webkit.URLUtil
import android.widget.*
import androidx.activity.viewModels
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.databinding.ActivityVisualizeTextBinding
import com.guillermonegrete.tts.ui.BrightnessTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class VisualizeTextActivity: AppCompatActivity() {

    private val viewModel: VisualizeTextViewModel by viewModels()

    private lateinit var binding: ActivityVisualizeTextBinding

    private lateinit var viewPager: ViewPager2

    // Bottom sheet layout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ViewGroup>

    private var pageItemView: View? = null

    private lateinit var pagesAdapter: VisualizerAdapter

    @Inject lateinit var preferences: SharedPreferences
    @Inject lateinit var brightnessTheme: BrightnessTheme
    @StyleRes private var themeRes = R.style.AppMaterialTheme_Black

    private val noteSheetVisible = mutableStateOf(false)

    private var splitterCreated = true

    private var scaleDetector: ScaleGestureDetector? = null

    private var cardWidth = 0
    /**
     * The vertical pixel distance between the center of the card and the center of the screen.
     *
     * A positive distance means the screen's center is below the card's, negative means the card's center is below.
     */
    private var cardYOffset = 0f

    /**
     * The ratio between the size of the screen and card view, ratio = cardWith / screenWidth
     * Used to get the desired dimensions of the card.
     */
    private var ratio = 0.8f

    private var sheetBarHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = getSharedText()
        binding = ActivityVisualizeTextBinding.inflate(layoutInflater)
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

        val exampleFragment = VisualizeTextFragment()
        supportFragmentManager.beginTransaction()
            .add(R.id.main_fragment_container, exampleFragment).commit()

        // Bottom sheet
        sheetBarHeight = resources.getDimensionPixelSize(R.dimen.visualize_sheet_bar_height)

//        scaleDetector = ScaleGestureDetector(this, PinchListener(binding.textReaderCardView))

    }

    override fun onPause() {
        super.onPause()
        viewModel.saveBookData()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (noteSheetVisible.value) {
            noteSheetVisible.value = false
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Handle scaling in text view with selectable text and clickable spans. Intercept touch event if it's scaling.
     *
     * Inspired by: https://stackoverflow.com/a/5369880/10244759
     */
    private var eventInProgress = false
    private var scaleInProgress = false

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {

        val scaleDetector = scaleDetector ?: return super.dispatchTouchEvent(ev)

        if(eventInProgress){
            if(pageItemView?.isShown == true) scaleDetector.onTouchEvent(ev)
            if(scaleDetector.isInProgress) {
                // Cancel long press to avoid showing contextual action menu
                pageItemView?.cancelLongPress()
                scaleInProgress = true
                // Don't pass event when scaling
                return true
            }
        }

        when(ev.actionMasked){
            MotionEvent.ACTION_DOWN -> eventInProgress = true
            MotionEvent.ACTION_UP -> {
                eventInProgress = false

                if(scaleInProgress){
                    // Removes lingering highlight from text
                    removeSelection()
                    scaleInProgress = false
                    return true
                }
            }
        }

        // When scaling don't handle other events, this avoids unexpected clicks and changes of page
        return if(scaleInProgress) true else super.dispatchTouchEvent(ev)
    }

    private fun removeSelection(){
        val item = pageItemView
        if(item is TextView && item.hasSelection()){
            val span = item.text as? Spannable
            Selection.removeSelection(span)
        }
    }

    /*override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(hasFocus && viewModel.fullScreen) {

            // Only hide the UI when page splitter has been created to avoid incorrect size measuring
            if(splitterCreated) hideSystemUi()
        }
    }*/

    inner class PinchListener(private val textCardView: View): ScaleGestureDetector.OnScaleGestureListener{

        private var pinchDetected = false

        private val screenWidth = this@VisualizeTextActivity.resources.displayMetrics.widthPixels

        /**
         * The inverse of the ratio between the widths of the card and the screen.
         * This is the ratio/scale the card should have when fully expanded (max scale).
         */
        private var invRatio = 1f
        private val minScale = 1f
        private var scale = 1f

        private var constantTerm = 0f

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            viewPager.isUserInputEnabled = false
            pinchDetected = false
            invRatio = 1f / ratio
            constantTerm = (cardYOffset / (invRatio - minScale))
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            viewPager.isUserInputEnabled = true

            val factor = detector.scaleFactor

            // Check if it should toggle off full screen mode
            if(viewModel.fullScreen && factor < 1.0f){
                val lastWidth = screenWidth * factor
                val middleWidth = cardWidth + (screenWidth - cardWidth) / 2f
                if(lastWidth < middleWidth){
                    toggleImmersiveMode()
                    scale = 1f
                }
            }

            textCardView.scaleX = scale
            textCardView.scaleY = scale
            if (!viewModel.fullScreen) textCardView.translationY = 0f
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {

            if(!pinchDetected){

                val factor = detector.scaleFactor
                val newScale = scale * factor

                // Avoid making the card smaller
                if (newScale >= minScale) {
                    textCardView.scaleX = newScale
                    textCardView.scaleY = newScale
                    // To calculate the new Y offset, using cross-multiplication: newScale / invRatio = newYOffset / cardYOffset
                    // To normalize the scale/ratio to start from 0 , the min ratio is subtracted, therefore solving for newYOffset yields:
                    // newYOffset = (newScale - minScale) * (cardYOffset / (invRatio - minScale))
                    val newYOffset = (newScale - minScale) * constantTerm
                    if (newScale <= invRatio) textCardView.translationY = newYOffset
                } else {
                    textCardView.scaleX = minScale
                    textCardView.scaleY = minScale
                    textCardView.translationY = 0f
                }

                val fullScreen = viewModel.fullScreen

                if(detector.scaleFactor > PINCH_UPPER_LIMIT && !fullScreen){
                    toggleImmersiveMode()
                    pinchDetected = true
                    scale = invRatio
                    textCardView.scaleX = invRatio
                    textCardView.scaleY = invRatio
                    textCardView.translationY = cardYOffset
                    return true
                }
            }

            return false
        }
    }

    private fun toggleImmersiveMode() {
        val position = viewModel.currentPage

        viewModel.fullScreen = !viewModel.fullScreen

        if(viewModel.fullScreen){
            hideSystemUi()
        }else{
            val decorView = window.decorView
            val controllerCompat = WindowCompat.getInsetsController(window, decorView)
            controllerCompat.show(WindowInsetsCompat.Type.systemBars())
            actionBar?.show()
        }

        viewPager.post { setBottomSheetPeekHeight() }

        viewPager.adapter = pagesAdapter
        viewPager.setCurrentItem(position, false)

    }

    private fun hideSystemUi(){
        val decorView = window.decorView
        val controllerCompat = WindowCompat.getInsetsController(window, decorView)
        controllerCompat.hide(WindowInsetsCompat.Type.systemBars())
        controllerCompat.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        actionBar?.hide()
    }

    private fun setBottomSheetPeekHeight(){
        val peekHeight = sheetBarHeight + viewPager.height / 2
        bottomSheetBehavior.peekHeight = peekHeight
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

    companion object{
        const val IMPORTED_TEXT = "imported_text"
        const val EPUB_URI = "epub_uri"

        const val SHOW_EPUB = "epub"
        const val FILE_ID = "fileId"

        const val PINCH_UPPER_LIMIT = 1.15f
    }
}
