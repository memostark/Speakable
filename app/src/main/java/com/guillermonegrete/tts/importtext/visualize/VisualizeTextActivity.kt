package com.guillermonegrete.tts.importtext.visualize

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.Selection
import android.text.Spannable
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.edit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.importtext.epub.NavPoint
import com.guillermonegrete.tts.textprocessing.TextInfoDialog
import com.guillermonegrete.tts.ui.BrightnessTheme
import com.guillermonegrete.tts.utils.dpToPixel
import dagger.android.AndroidInjection
import javax.inject.Inject

class VisualizeTextActivity: AppCompatActivity() {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(VisualizeTextViewModel::class.java)
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var rootConstraintLayout: ConstraintLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var pagesSeekBar: SeekBar
    private lateinit var currentPageLabel: TextView
    private lateinit var currentChapterLabel: TextView

    // Bottom sheet layout
    private lateinit var bottomSheet: ViewGroup
    private lateinit var bottomText: TextView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ViewGroup>

    private var pageItemView: View? = null

    private lateinit var pagesAdapter: VisualizerAdapter

    private val expandedConstraintSet = ConstraintSet()
    private val contractedConstraintSet = ConstraintSet()

    @Inject lateinit var preferences: SharedPreferences
    @Inject lateinit var brightnessTheme: BrightnessTheme

    private var splitterCreated = true
    private var isFullScreen = false

    private lateinit var scaleDetector: ScaleGestureDetector

    private lateinit var languagesISO: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        setPreferenceTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visualize_text)

        progressBar = findViewById(R.id.visualizer_progress_bar)
        pagesSeekBar = findViewById(R.id.pages_seekBar)

        // Bottom sheet
        bottomSheet = findViewById(R.id.visualizer_bottom_sheet)
        bottomText = findViewById(R.id.page_bottom_text_view)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        rootConstraintLayout = findViewById(R.id.visualizer_root_layout)

        contractedConstraintSet.clone(rootConstraintLayout)
        expandedConstraintSet.clone(this, R.layout.activity_visualize_text_expanded)

        createViewModel()

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
            {}, viewModel) // Empty callback, not necessary at the moment
        viewPager.setPageTransformer { view, position ->
            pageItemView = view

            setUpPageParsing(view)

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

        languagesISO = resources.getStringArray(R.array.googleTranslateLanguagesValue)
    }

    /**
     * Handle scaling in text view with selectable text and clickable spans. Intercept touch event if is scaling.
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
                    val item = pageItemView
                    if(item is TextView){
                        val span = item.text as? Spannable
                        Selection.removeSelection(span)
                    }
                    scaleInProgress = false
                }
            }
        }

        return super.dispatchTouchEvent(ev)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(hasFocus && isFullScreen) hideSystemUi()
    }


    override fun onDestroy() {
        viewModel.onFinish()
        super.onDestroy()
    }

    private fun setUIChangesListener() {
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0 && isFullScreen) {
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
            dataLoading.observe(this@VisualizeTextActivity, Observer {
                progressBar.visibility = if(it) View.VISIBLE else View.INVISIBLE

                // Update set in case of layout changes
                // The last layout observer to be called is data loading with value false
                if(!it) contractedConstraintSet.clone(rootConstraintLayout)
            })

            pages.observe(this@VisualizeTextActivity, Observer {
                updateCurrentChapterLabel()
                setUpPagerAndIndexLabel(it)
            })

            book.observe(this@VisualizeTextActivity, Observer {
                if(it.spine.isEmpty()) currentChapterLabel.visibility = View.GONE
                else{
                    updateCurrentChapterLabel()
                    currentChapterLabel.visibility = View.VISIBLE
                }

                val showTOCBtn: ImageButton = findViewById(R.id.show_toc_btn)
                val navPoints = it.tableOfContents.navPoints
                if(navPoints.isEmpty()){
                    showTOCBtn.visibility = View.GONE
                }else{
                    showTOCBtn.visibility = View.VISIBLE
                    showTOCBtn.setOnClickListener { showTableOfContents(navPoints) }
                }
            })

            translatedPageIndex.observe(this@VisualizeTextActivity, Observer {
                val text = viewModel.translatedPages[it]
                bottomText.text = text
                pagesAdapter.notifyItemChanged(it)
            })
        }
    }

    private fun initParse(){
        if(SHOW_EPUB == intent.action) {
            val uri: Uri = intent.getParcelableExtra(EPUB_URI)
            val rootStream = contentResolver.openInputStream(uri)
            viewModel.fileReader = DefaultZipFileReader(rootStream)
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
        val layout = LayoutInflater.from(this).inflate(R.layout.pop_up_settings, view.rootView as ViewGroup, false)

        // Brightness settings
        layout.findViewById<Button>(R.id.white_bg_btn).setOnClickListener {setBackgroundColor(BrightnessTheme.WHITE)}
        layout.findViewById<Button>(R.id.beige_bg_btn).setOnClickListener {setBackgroundColor(BrightnessTheme.BEIGE)}
        layout.findViewById<Button>(R.id.black_bg_btn).setOnClickListener {setBackgroundColor(BrightnessTheme.BLACK)}

        // Split view
        val splitPref: ImageButton = layout.findViewById(R.id.split_page_btn)
        val singlePagePref: ImageButton = layout.findViewById(R.id.single_page_btn)

        splitPref.setOnClickListener {
            setSplitPageMode(true)

            splitPref.isSelected = true
            singlePagePref.isSelected = false
        }
        singlePagePref.setOnClickListener {
            setSplitPageMode(false)

            splitPref.isSelected = false
            singlePagePref.isSelected = true
        }

        splitPref.isSelected = pagesAdapter.hasBottomSheet
        singlePagePref.isSelected = !pagesAdapter.hasBottomSheet

        // Languages preferences
        val fromAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.googleTranslateLangsWithAutoArray,
            android.R.layout.simple_spinner_dropdown_item
        )
        val toAdapter = ArrayAdapter.createFromResource(
            this,
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

        PopupWindow(
            layout,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            isFocusable = true
            elevation = 8f
            showAtLocation(view, Gravity.START or Gravity.BOTTOM, 24, 24)
        }
    }

    private fun setSplitPageMode(splitPage: Boolean){
        val position = viewModel.currentPage

        pagesAdapter.hasBottomSheet = splitPage
        viewPager.adapter = pagesAdapter
        viewPager.setCurrentItem(position, false)

        bottomSheet.visibility = if(splitPage) View.VISIBLE else View.GONE
    }

    private fun setBackgroundColor(theme: BrightnessTheme){
        if(theme != brightnessTheme) {
            saveBrightnessPreference(theme.value)
            viewModel.onFinish()
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
            val zipReader = DefaultZipFileReader(contentResolver.openInputStream(uri))
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
            .setTitle("Table of contents")
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

        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
            viewPager.isUserInputEnabled = false
            pinchDetected = false
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector?) {
            viewPager.isUserInputEnabled = true
        }

        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            detector ?: return false

            if(!pinchDetected){
                if(detector.scaleFactor > PINCH_UPPER_LIMIT && !isFullScreen){
                    toggleImmersiveMode()
                    pinchDetected = true
                    return true
                }
                if(detector.scaleFactor < PINCH_LOWER_LIMIT && isFullScreen){
                    toggleImmersiveMode()
                    pinchDetected = true
                    return true
                }
            }

            return false
        }
    }

    private fun toggleImmersiveMode() {
        val position = viewModel.currentPage

        isFullScreen = !isFullScreen
        if(isFullScreen){
            hideSystemUi()

            expandedConstraintSet.applyTo(rootConstraintLayout)

            pagesAdapter.isExpanded = true


        }else{
            window.decorView.systemUiVisibility = 0
            actionBar?.show()

            contractedConstraintSet.applyTo(rootConstraintLayout)

            pagesAdapter.isExpanded = false
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
            val translatedText = viewModel.translatedPages[position]
            if(translatedText == null) {
                viewModel.translatePage(position)
            }
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
                pagesAdapter.isPageSplit = isSplit
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

    companion object{
        const val IMPORTED_TEXT = "imported_text"
        const val EPUB_URI = "epub_uri"

        const val SHOW_EPUB = "epub"
        const val FILE_ID = "fileId"

        const val PINCH_UPPER_LIMIT = 1.3f
        const val PINCH_LOWER_LIMIT = 0.8f
    }
}