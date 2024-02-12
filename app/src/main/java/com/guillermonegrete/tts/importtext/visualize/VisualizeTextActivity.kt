package com.guillermonegrete.tts.importtext.visualize

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.*
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.guillermonegrete.tts.EventObserver
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.importtext.epub.NavPoint
import com.guillermonegrete.tts.importtext.visualize.model.SplitPageSpan
import com.guillermonegrete.tts.textprocessing.TextInfoDialog
import com.guillermonegrete.tts.ui.BrightnessTheme
import com.guillermonegrete.tts.utils.dpToPixel
import com.guillermonegrete.tts.utils.getScreenSizes
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class VisualizeTextActivity: AppCompatActivity() {

    private val viewModel: VisualizeTextViewModel by viewModels()

    private lateinit var viewPager: ViewPager2
    private lateinit var progressBar: ProgressBar
    private lateinit var pagesSeekBar: SeekBar
    private lateinit var currentPageLabel: TextView
    private lateinit var currentChapterLabel: TextView
    private lateinit var textCardView: CardView

    // Bottom sheet layout
    private lateinit var bottomSheet: ViewGroup
    private lateinit var bottomText: TextView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ViewGroup>
    private lateinit var translationProgress: ProgressBar

    private var pageItemView: View? = null

    private lateinit var pagesAdapter: VisualizerAdapter

    @Inject lateinit var preferences: SharedPreferences
    @Inject lateinit var brightnessTheme: BrightnessTheme

    private var splitterCreated = true

    private lateinit var scaleDetector: ScaleGestureDetector

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPreferenceTheme()
        setContentView(R.layout.activity_visualize_text)

        progressBar = findViewById(R.id.visualizer_progress_bar)
        pagesSeekBar = findViewById(R.id.pages_seekBar)

        textCardView = findViewById(R.id.text_reader_card_view)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Never draw on the cutout because it may obstruct text in full screen
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
        }
        // Let the layout cover the full screen (except cutout) this way the card is always centered
        WindowCompat.setDecorFitsSystemWindows(window, false)
        currentPageLabel = findViewById(R.id.reader_current_page)
        currentChapterLabel = findViewById(R.id.reader_current_chapter)

        // Bottom sheet
        bottomSheet = findViewById(R.id.visualizer_bottom_sheet)
        bottomText = findViewById(R.id.page_bottom_text_view)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        translationProgress = findViewById(R.id.page_translation_progress)

        val brightnessButton: ImageButton = findViewById(R.id.brightness_settings_btn)
        brightnessButton.setOnClickListener { showSettingsPopUp(it) }

        if(SHOW_EPUB != intent.action) {
            currentChapterLabel.visibility = View.GONE
        }

        viewPager = findViewById(R.id.text_reader_viewpager)
        // Creates one item so setPageTransformer is called
        // Used to get the page text view properties to create page splitter.
        viewPager.adapter = VisualizerAdapter(listOf(""),
            {}, true) // Empty callback, not necessary at the moment
        viewPager.setPageTransformer { view, position ->
            pageItemView = view

            setUpPageParsing(view)

            removeSelection()

            // A new page is shown when position is 0.0f,
            // so we request focus in order to highlight text correctly.
            if(position == 0.0f) setPageTextFocus()

            // Remove highlights when page is mostly hidden.
            if(position > 0.9f || position < -0.9f){
                val topText: TextView = view.findViewById(R.id.page_text_view) ?: return@setPageTransformer
                val text = SpannableString(topText.text)
                val spans = text.getSpans(0, text.length, BackgroundColorSpan::class.java).map { span -> text.removeSpan(span) }
                if(spans.isNotEmpty()) topText.setText(text, TextView.BufferType.SPANNABLE)
            }
        }
        viewPager.post{
            addPagerCallback()

            setBottomSheetPeekHeight()
            setBottomSheetCallbacks()
        }

        scaleDetector = ScaleGestureDetector(this, PinchListener())

        setUIChangesListener()
        setUpSeekBar()
    }

    /**
     *  Changes the dimensions of the card to have the same aspect ratio as the screen
     *  and smaller size given by the defined ratio.
     */
    private fun setUpCardViewDimensions(hasCutOut: Boolean) {
        val screenSizes = getScreenSizes()
        // Remove cutout height because it's not used
        val screenHeight = if(!hasCutOut) screenSizes.height else screenSizes.height - screenSizes.statusHeight

        val cardHeight = textCardView.height
        val cardCenterY = textCardView.y + cardHeight / 2
        cardYOffset = (screenHeight / 2) - cardCenterY
        ratio = cardHeight / screenHeight.toFloat()
        cardWidth = (screenSizes.width * ratio).toInt()

        val cardParams = textCardView.layoutParams
        cardParams.width = cardWidth
        textCardView.layoutParams = cardParams
    }

    override fun onPause() {
        super.onPause()
        viewModel.saveBookData()
    }

    /**
     * Handle scaling in text view with selectable text and clickable spans. Intercept touch event if it's scaling.
     *
     * Inspired by: https://stackoverflow.com/a/5369880/10244759
     */
    private var eventInProgress = false
    private var scaleInProgress = false

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {

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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(hasFocus && viewModel.fullScreen) {

            // Only hide the UI when page splitter has been created to avoid incorrect size measuring
            if(splitterCreated) hideSystemUi()
        }
    }

    private fun setUIChangesListener() {

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val systembarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            val hasCutOut = insets.isVisible(WindowInsetsCompat.Type.displayCutout())
            if (!hasCutOut) { // Phones with a normal status bar require extra margin to avoid overlapping with the bar
                currentChapterLabel.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = currentChapterLabel.marginTop + systembarInsets.top
                }
            }

            // Margin for the bottom icons
            currentPageLabel.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = currentPageLabel.marginBottom + systembarInsets.bottom
            }

            textCardView.post {
                setUpCardViewDimensions(hasCutOut)
            }

            ViewCompat.setOnApplyWindowInsetsListener(window.decorView, null)
            insets
        }
    }

    private fun setUpSeekBar(){

        pagesSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser) viewPager.currentItem = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

    }

    // Change activity value at runtime: https://stackoverflow.com/a/6390025/10244759
    private fun setPreferenceTheme() {
        when(brightnessTheme){
            BrightnessTheme.WHITE -> setTheme(R.style.AppMaterialTheme_White)
            BrightnessTheme.BEIGE -> setTheme(R.style.AppMaterialTheme_Beige)
            BrightnessTheme.BLACK -> setTheme(R.style.AppMaterialTheme_Black)
        }
    }

    private fun createViewModel() {
        viewModel.apply {
            dataLoading.observe(this@VisualizeTextActivity) {
                progressBar.visibility = if (it) View.VISIBLE else View.INVISIBLE
            }

            pages.observe(this@VisualizeTextActivity, EventObserver {
                updateCurrentChapterLabel()
                setUpPagerAndIndexLabel(it)
            })

            book.observe(this@VisualizeTextActivity) {
                if (it.spine.isEmpty()) currentChapterLabel.visibility = View.GONE
                else {
                    updateCurrentChapterLabel()
                    currentChapterLabel.visibility = View.VISIBLE
                }

                val showTOCBtn: ImageButton = findViewById(R.id.show_toc_btn)
                val navPoints = it.tableOfContents.navPoints

                val tocVisibility = if (navPoints.isEmpty()) {
                    View.GONE
                } else {
                    showTOCBtn.setOnClickListener { showTableOfContents(navPoints) }
                    View.VISIBLE
                }
                showTOCBtn.visibility = tocVisibility
            }

            translatedPageIndex.observe(this@VisualizeTextActivity, EventObserver {
                val translation = viewModel.translatedPages[it]
                bottomText.text = translation?.translatedText
                pagesAdapter.notifyItemChanged(it)
            })

            translationLoading.observe(this@VisualizeTextActivity) {
                translationProgress.visibility = if (it) View.VISIBLE else View.INVISIBLE
                if (it) bottomText.text = ""
            }

            translationError.observe(this@VisualizeTextActivity, EventObserver {
                Toast.makeText(this@VisualizeTextActivity, getString(R.string.error_translation), Toast.LENGTH_SHORT).show()
                bottomText.text = getString(R.string.click_to_translate_msg)
            })

            languagesISO = resources.getStringArray(R.array.googleTranslateLanguagesValue)
        }
    }

    private fun initParse(){
        if(SHOW_EPUB == intent.action) {
            val uri: Uri = intent.getParcelableExtra(EPUB_URI) ?: return
            val rootStream = contentResolver.openInputStream(uri)
            viewModel.fileReader = DefaultZipFileReader(rootStream, this)
            viewModel.fileUri = uri.toString()
            viewModel.fileId = intent.getIntExtra(FILE_ID, -1)
            viewModel.parseEpub()
        } else {
            viewModel.parseSimpleText(intent?.extras?.getString(IMPORTED_TEXT) ?: "No text")
        }
    }

    private fun setUpPagerAndIndexLabel(pages: List<CharSequence>){
        pagesAdapter = VisualizerAdapter(pages, { showTextDialog(it) })
        pagesAdapter.hasBottomSheet = viewModel.hasBottomSheet
        pagesAdapter.isPageSplit = viewModel.isSheetExpanded
        viewPager.adapter = pagesAdapter

        val position = viewModel.getPage()
        currentPageLabel.text = resources.getString(R.string.reader_current_page_label, position + 1, pages.size) // Example: 1 / 33
        viewPager.setCurrentItem(position, false)

        // Subtract 1 because seek bar is zero based numbering
        pagesSeekBar.max = pages.size - 1
        pagesSeekBar.progress = position

        // Restore UI state in case of config change
        bottomSheet.visibility = if(viewModel.hasBottomSheet) View.VISIBLE else View.GONE
        setFullBottomSheet(viewModel.isSheetExpanded)
    }

    private fun showSettingsPopUp(view: View) {

        val languagesISO = viewModel.languagesISO

        val popUpCallback = object: VisualizerSettingsWindow.Callback{

            override fun onBackgroundColorSet(theme: BrightnessTheme) {
                setBackgroundColor(theme)
            }

            override fun onPageMode(isSplit: Boolean) {
                setSplitPageMode(isSplit)
            }

            override fun onLanguageToChanged(position: Int) {
                viewModel.languageTo = languagesISO[position]
            }

            override fun onLanguageFromChanged(position: Int) {
                viewModel.languageFrom = if (position == 0) "auto" else languagesISO[position - 1]
            }
        }

        VisualizerSettingsWindow(
            view,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            viewModel.hasBottomSheet,
            languagesISO,
            viewModel.languageFrom,
            viewModel.languageTo,
            popUpCallback,
        ).apply {
            isFocusable = true
            elevation = 8f
            showAtLocation(view, Gravity.START or Gravity.BOTTOM, 24, 24)
        }
    }

    private fun setSplitPageMode(isEnabled: Boolean){
        val position = viewModel.currentPage

        viewModel.hasBottomSheet = isEnabled
        pagesAdapter.hasBottomSheet = isEnabled
        viewPager.adapter = pagesAdapter
        viewPager.setCurrentItem(position, false)

        bottomSheet.visibility = if(isEnabled) View.VISIBLE else View.GONE
    }

    private fun setBackgroundColor(theme: BrightnessTheme){
        if(theme != brightnessTheme) {
            saveBrightnessPreference(theme.value)
            finish()
            startActivity(intent)
        }
    }

    private fun saveBrightnessPreference(preference: String){
        preferences.edit{ putString(BrightnessTheme.PREFERENCE_KEY, preference) }
    }

    private fun addPagerCallback(){
        var swipeFirst = false
        viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){

            override fun onPageSelected(position: Int) {
                viewModel.currentPage = position

                val pageNumber = position + 1
                currentPageLabel.text = resources.getString(R.string.reader_current_page_label, pageNumber, viewModel.pagesSize)

                pagesSeekBar.progress = position

                if(pagesAdapter.hasBottomSheet)
                    bottomText.text = viewModel.translatedPages[position]?.translatedText ?: getString(R.string.click_to_translate_msg)

            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                if(state == ViewPager2.SCROLL_STATE_DRAGGING) swipeFirst = true
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

                if(positionOffset > 0){
                    swipeFirst = false
                }else{
                    if(swipeFirst) {
                        swipeFirst = false
                        if(position == 0) {
                            viewModel.swipeChapterLeft()
                        } else if(position == viewModel.pagesSize - 1) {
                            viewModel.swipeChapterRight()
                        }
                    }
                }
            }
        })
    }

    private fun setPageTextFocus() {
        val focusedView = viewPager.focusedChild

        val pageTextView: View? = focusedView?.findViewById(R.id.page_text_view)
        pageTextView?.requestFocus()
    }

    private fun setUpPageParsing(focusedView: View){
        // Execute only one time
        if(splitterCreated) {

            createViewModel()

            val pageTextView: TextView = focusedView as TextView
            viewModel.pageSplitter = createPageSplitter(pageTextView)
            initParse()
            splitterCreated = false
        }
    }

    private fun createPageSplitter(textView: TextView): PageSplitter {
        val lineSpacingExtra = resources.getDimension(R.dimen.visualize_page_text_line_spacing_extra)
        val lineSpacingMultiplier = 1f
        val pageItemPadding = this.dpToPixel(80 )

        val uri: Uri? = intent.getParcelableExtra(EPUB_URI)
        val imageGetter = if(uri != null) {
            val zipReader = DefaultZipFileReader(contentResolver.openInputStream(uri), this)
            InputStreamImageGetter( this, zipReader)
        } else null

        return PageSplitter(
            viewPager.width - pageItemPadding,
            viewPager.height - pageItemPadding,
            lineSpacingMultiplier,
            lineSpacingExtra,
            textView.paint,
            textView.includeFontPadding,
            imageGetter
        )
    }

    private fun showTableOfContents(navPoints: List<NavPoint>){

        val filePaths = navPoints.map { it.getContentWithoutTag() }
        val titles = navPoints.map { it.navLabel }

        val adapter = ArrayAdapter(this, R.layout.dialog_item, titles)

        val dialog = AlertDialog.Builder(this)
            .setTitle(resources.getString(R.string.table_of_contents))
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setAdapter(adapter) { _, i ->
                val path = filePaths[i]
                viewModel.jumpToChapter(path)
            }
            .create()
        dialog.show()
    }

    private fun updateCurrentChapterLabel(){
        currentChapterLabel.text = resources.getString(R.string.reader_current_chapter_label, viewModel.currentChapter + 1, viewModel.spineSize)
    }

    private fun showTextDialog(text: CharSequence){
        val dialog = TextInfoDialog.newInstance(
            text.toString(),
            TextInfoDialog.NO_SERVICE,
            null,
            brightnessTheme.value
        )
        dialog.show(supportFragmentManager, "Text_info")
    }

    inner class PinchListener: ScaleGestureDetector.OnScaleGestureListener{

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

    @SuppressLint("ClickableViewAccessibility")
    private fun setBottomSheetCallbacks(){
        val translateBtn: Button = findViewById(R.id.page_translate_btn)
        translateBtn.setOnClickListener {
            val position = viewPager.currentItem
            viewModel.translatePage(position)

            setFullBottomSheet(true)
        }

        val arrowBtn: Button = findViewById(R.id.arrow_btn)
        arrowBtn.setOnClickListener {
            val isFull = pagesAdapter.isPageSplit
            setFullBottomSheet(!isFull)
        }

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) { arrowBtn.rotation = slideOffset * 180 }

            override fun onStateChanged(bottomSheet: View, newState: Int) {

                when(newState){
                    BottomSheetBehavior.STATE_EXPANDED -> setSplitMode(true)
                    BottomSheetBehavior.STATE_COLLAPSED -> setSplitMode(false)
                    else -> {}
                }
            }

            private fun setSplitMode(isSplit: Boolean){
                pagesAdapter.notifyItemRangeChanged(0, pagesAdapter.itemCount, isSplit)
                viewModel.isSheetExpanded = isSplit
                pagesAdapter.isPageSplit = isSplit
            }
        })

        val metrics = resources.displayMetrics
        val cardHalfHeight = (metrics.heightPixels * ratio / 2f).toInt()

        var startX = 0f
        var startY = 0f
        val radius = 40f
        // To detect the click, onTouchListener is used to get the touch coordinates and because onClickListener consumes the touch event
        bottomText.setOnTouchListener { _, event ->
            val duration = event.eventTime - event.downTime

            when(event.action){
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                }
                MotionEvent.ACTION_UP -> {
                    if(duration < 300
                        && abs(event.x - startX) < radius && abs(event.y - startY) < radius) {
                        val index = bottomText.getOffsetForPosition(event.x, event.y)
                        val splitSpans = viewModel.findSelectedSentence(viewPager.currentItem, index) ?: return@setOnTouchListener true
                        highlightSpans(splitSpans)
                    }
                }
            }

            // Need to dispatch the touch event to the ViewPager otherwise scrolling won't work on the bottom text
            // The y position of the touch event is in terms of the bottom text origin, add offset to make it in terms of the card view origin
            val newEvent = MotionEvent.obtain(event.downTime, event.eventTime, event.action, event.x, event.y + cardHalfHeight, event.metaState)
            try{
                viewPager.dispatchTouchEvent(newEvent)
            }catch (exception: IllegalArgumentException){
                // Sometimes an exception will be thrown "pointerIndex out of range".
                // Ignoring it seems to not affect the scaling gesture
                Timber.e("Dispatching touch event to ViewPager error: ${exception.message}")
            }
            true
        }
    }

    private fun setFullBottomSheet(full: Boolean){
        bottomSheetBehavior.state = if(full) BottomSheetBehavior.STATE_EXPANDED else BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun setBottomSheetPeekHeight(){
        val peekHeight = this.dpToPixel(30) + viewPager.height / 2

        bottomSheetBehavior.peekHeight = peekHeight
    }

    private fun highlightSpans(pageSpans: SplitPageSpan) {
        val text = SpannableString(bottomText.text)

        //Remove previous
        text.getSpans(0, text.length, BackgroundColorSpan::class.java).map { span -> text.removeSpan(span) }

        text.setSpan(BackgroundColorSpan(0x6633B5E5), pageSpans.bottomSpan.start, pageSpans.bottomSpan.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        bottomText.setText(text, TextView.BufferType.SPANNABLE)

        // Notify the adapter to set the highlight, send span payload
        pagesAdapter.notifyItemChanged(viewPager.currentItem, pageSpans.topSpan)
    }

    companion object{
        const val IMPORTED_TEXT = "imported_text"
        const val EPUB_URI = "epub_uri"

        const val SHOW_EPUB = "epub"
        const val FILE_ID = "fileId"

        const val PINCH_UPPER_LIMIT = 1.15f
    }
}
