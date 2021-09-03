package com.guillermonegrete.tts.importtext.visualize

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.Selection
import android.text.Spannable
import android.view.*
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.edit
import androidx.core.view.*
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.guillermonegrete.tts.EventObserver
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.importtext.epub.NavPoint
import com.guillermonegrete.tts.textprocessing.TextInfoDialog
import com.guillermonegrete.tts.ui.BrightnessTheme
import com.guillermonegrete.tts.utils.dpToPixel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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
    private var statusBarHeight = 0

    /**
     * The ratio between the size of the screen and card view, ratio = cardWith / screenWidth
     * Used to get the desired dimensions of the card.
     */
    private val ratio = 0.8f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPreferenceTheme()
        setContentView(R.layout.activity_visualize_text)

        progressBar = findViewById(R.id.visualizer_progress_bar)
        pagesSeekBar = findViewById(R.id.pages_seekBar)

        textCardView = findViewById(R.id.text_reader_card_view)
        setUpCardViewDimensions()

        // Bottom sheet
        bottomSheet = findViewById(R.id.visualizer_bottom_sheet)
        bottomText = findViewById(R.id.page_bottom_text_view)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        translationProgress = findViewById(R.id.page_translation_progress)

        currentPageLabel = findViewById(R.id.reader_current_page)
        currentChapterLabel = findViewById(R.id.reader_current_chapter)

        val brightnessButton: ImageButton = findViewById(R.id.brightness_settings_btn)
        brightnessButton.setOnClickListener { showSettingsPopUp(it) }

        if(SHOW_EPUB != intent.action) {
            currentChapterLabel.visibility = View.GONE
        }

        viewPager = findViewById(R.id.text_reader_viewpager)
        // Creates one item so setPageTransformer is called
        // Used to get the page text view properties to create page splitter.
        viewPager.adapter = VisualizerAdapter(listOf(""),
            {}, viewModel, true) // Empty callback, not necessary at the moment
        viewPager.setPageTransformer { view, position ->
            pageItemView = view

            setUpPageParsing(view)

            removeSelection()

            // A new page is shown when position is 0.0f,
            // so we request focus in order to highlight text correctly.
            if(position == 0.0f) setPageTextFocus()
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
    private fun setUpCardViewDimensions() {

        val cardParams = textCardView.layoutParams
        val metrics = resources.displayMetrics

        cardWidth = (metrics.widthPixels * ratio).toInt()
        cardParams.width = cardWidth
        cardParams.height = (metrics.heightPixels * ratio).toInt()
        textCardView.layoutParams = cardParams

        // In case a status bar exists, offset the card view in order to keep it centered with the screen
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        statusBarHeight = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0

        textCardView.translationY = - statusBarHeight / 2f
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

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val actionId = ev?.actionMasked ?: -1

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

        when(actionId){
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
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0 && viewModel.fullScreen) {
                // The system bars are visible
                val handler = Handler()
                handler.postDelayed({ hideSystemUi() }, 3000)
            }
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
            dataLoading.observe(this@VisualizeTextActivity, {
                progressBar.visibility = if(it) View.VISIBLE else View.INVISIBLE
            })

            pages.observe(this@VisualizeTextActivity, EventObserver {
                updateCurrentChapterLabel()
                setUpPagerAndIndexLabel(it)
            })

            book.observe(this@VisualizeTextActivity, {
                if(it.spine.isEmpty()) currentChapterLabel.visibility = View.GONE
                else{
                    updateCurrentChapterLabel()
                    currentChapterLabel.visibility = View.VISIBLE
                }

                val showTOCBtn: ImageButton = findViewById(R.id.show_toc_btn)
                val navPoints = it.tableOfContents.navPoints

                val tocVisibility = if(navPoints.isEmpty()) {
                    View.GONE
                }else{
                    showTOCBtn.setOnClickListener { showTableOfContents(navPoints) }
                    View.VISIBLE
                }
                showTOCBtn.visibility = tocVisibility
            })

            translatedPageIndex.observe(this@VisualizeTextActivity, EventObserver {
                val text = viewModel.translatedPages[it]
                bottomText.text = text
                pagesAdapter.notifyItemChanged(it)
            })

            translationLoading.observe(this@VisualizeTextActivity, {
                translationProgress.visibility = if(it) View.VISIBLE else View.INVISIBLE
                if(it) bottomText.text = ""
            })

            translationError.observe(this@VisualizeTextActivity, EventObserver {
                Toast.makeText(this@VisualizeTextActivity, getString(R.string.error_translation), Toast.LENGTH_SHORT).show()
                bottomText.text = getString(R.string.click_to_translate_msg)
            })

            languagesISO = resources.getStringArray(R.array.googleTranslateLanguagesValue)
        }

        // Restore UI state in case of config change
        bottomSheet.visibility = if(viewModel.hasBottomSheet) View.VISIBLE else View.GONE
        setFullBottomSheet(viewModel.isSheetExpanded)
    }

    private fun initParse(){
        if(SHOW_EPUB == intent.action) {
            val uri: Uri = intent.getParcelableExtra(EPUB_URI)
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
        pagesAdapter = VisualizerAdapter(pages, { showTextDialog(it) }, viewModel)
        viewPager.adapter = pagesAdapter

        val position = viewModel.getPage()
        currentPageLabel.text = resources.getString(R.string.reader_current_page_label, position + 1, pages.size) // Example: 1 / 33
        viewPager.setCurrentItem(position, false)

        // Subtract 1 because seek bar is zero based numbering
        pagesSeekBar.max = pages.size - 1
        pagesSeekBar.progress = position
    }

    private fun showSettingsPopUp(view: View) {

        val popUpCallback = object: VisualizerSettingsWindow.Callback{

            override fun onBackgroundColorSet(theme: BrightnessTheme) {
                setBackgroundColor(theme)
            }

            override fun onPageMode(isSplit: Boolean) {
                setSplitPageMode(isSplit)
            }
        }

        VisualizerSettingsWindow(
            view,
            viewModel,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            isFocusable = true
            elevation = 8f
            callback = popUpCallback
            showAtLocation(view, Gravity.START or Gravity.BOTTOM, 24, 24)
        }


    }

    private fun setSplitPageMode(splitPage: Boolean){
        val position = viewModel.currentPage

        viewModel.hasBottomSheet = splitPage
        viewPager.adapter = pagesAdapter
        viewPager.setCurrentItem(position, false)

        bottomSheet.visibility = if(splitPage) View.VISIBLE else View.GONE
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
                    bottomText.text = viewModel.translatedPages[position] ?: getString(R.string.click_to_translate_msg)

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

        private val invRatio
            get() = 1f / ratio
        private var scale = 1f

        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
            viewPager.isUserInputEnabled = false
            pinchDetected = false
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector?) {
            viewPager.isUserInputEnabled = true

            detector ?: return
            val factor = detector.scaleFactor

            // Check if it should toggle off full screen mode
            if(viewModel.fullScreen && factor < 1.0f){
                val lastWidth = screenWidth * factor
                val middleWidth = cardWidth + (screenWidth - cardWidth) / 2f
                if(lastWidth < middleWidth){
                    toggleImmersiveMode()
                    scale = 1f
                    textCardView.translationY = - statusBarHeight.toFloat() / 2f
                }
            }

            textCardView.scaleX = scale
            textCardView.scaleY = scale
        }

        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            detector ?: return false

            if(!pinchDetected){

                val factor = detector.scaleFactor
                val newScale = scale * factor

                // Avoid making the card smaller
                if (newScale >= 1f) {
                    textCardView.scaleX = newScale
                    textCardView.scaleY = newScale
                    textCardView.invalidate()
                }

                val fullScreen = viewModel.fullScreen

                if(detector.scaleFactor > PINCH_UPPER_LIMIT && !fullScreen){
                    toggleImmersiveMode()
                    pinchDetected = true
                    scale = invRatio
                    textCardView.scaleX = invRatio
                    textCardView.scaleY = invRatio
                    // Remove the offset for the full screen, only necessary when the status bar is visible
                    textCardView.translationY = 0f
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
            window.decorView.systemUiVisibility = 0
            actionBar?.show()
        }

        viewPager.post { setBottomSheetPeekHeight() }

        viewPager.adapter = pagesAdapter
        viewPager.setCurrentItem(position, false)

    }

    private fun hideSystemUi(){
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)

        actionBar?.hide()
    }

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
            }
        })
    }

    private fun setFullBottomSheet(full: Boolean){
        bottomSheetBehavior.state = if(full) BottomSheetBehavior.STATE_EXPANDED else BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun setBottomSheetPeekHeight(){
        val peekHeight = this.dpToPixel(30) + viewPager.height / 2

        bottomSheetBehavior.peekHeight = peekHeight
    }

    companion object{
        const val IMPORTED_TEXT = "imported_text"
        const val EPUB_URI = "epub_uri"

        const val SHOW_EPUB = "epub"
        const val FILE_ID = "fileId"

        const val PINCH_UPPER_LIMIT = 1.15f
    }
}