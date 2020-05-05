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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.importtext.epub.NavPoint
import com.guillermonegrete.tts.textprocessing.TextInfoDialog
import com.guillermonegrete.tts.ui.BrightnessTheme
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
    private lateinit var currentPageLabel: TextView
    private lateinit var currentChapterLabel: TextView

    private var pageTextView: TextView? = null

    private lateinit var pagesAdapter: VisualizerAdapter

    private val expandedConstraintSet = ConstraintSet()
    private val contractedConstraintSet = ConstraintSet()

    @Inject lateinit var preferences: SharedPreferences
    @Inject lateinit var brightnessTheme: BrightnessTheme

    private var splitterCreated = true
    private var isFullScreen = false

    private lateinit var scaleDetector: ScaleGestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        setPreferenceTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visualize_text)

        progressBar = findViewById(R.id.visualizer_progress_bar)
        rootConstraintLayout = findViewById(R.id.visualizer_root_layout)

        contractedConstraintSet.clone(rootConstraintLayout)
        expandedConstraintSet.clone(this, R.layout.activity_visualize_text_expanded)

        createViewModel()

        currentPageLabel = findViewById(R.id.reader_current_page)
        currentChapterLabel = findViewById(R.id.reader_current_chapter)

        val brightnessButton: ImageButton = findViewById(R.id.brightness_settings_btn)
        brightnessButton.setOnClickListener { showBrightnessSettings(it) }

        if(SHOW_EPUB != intent.action) {
            currentChapterLabel.visibility = View.GONE
        }

        viewPager = findViewById(R.id.text_reader_viewpager)
        // Creates one item so setPageTransformer is called
        // Used to get the page text view properties to create page splitter.
        viewPager.adapter = VisualizerAdapter(listOf("")) {} // Empty callback, not necessary at the moment
        viewPager.setPageTransformer { view, position ->
            pageTextView = view as? TextView

            setUpPageParsing(view)

            // A new page is shown when position is 0.0f,
            // so we request focus in order to highlight text correctly.
            if(position == 0.0f) setPageTextFocus()
        }
        viewPager.post{
            addPagerCallback()
        }

        scaleDetector = ScaleGestureDetector(this, PinchListener())

        setUIChangesListener()
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
            if(pageTextView?.isShown == true) scaleDetector.onTouchEvent(ev)
            if(scaleDetector.isInProgress) {
                // Cancel long press to avoid showing contextual action menu
                pageTextView?.cancelLongPress()
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
                    val span = pageTextView?.text as? Spannable
                    Selection.removeSelection(span)
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
                // The system bars are visible. Make any desired
                Toast.makeText(this, "Bars are visible", Toast.LENGTH_SHORT).show()
                val handler = Handler()
                handler.postDelayed({ hideSystemUi() }, 3000)
            }
        }
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
                progressBar.visibility = if(it) View.VISIBLE else View.GONE
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

                val showTOCBtn = findViewById<ImageButton>(R.id.show_toc_btn)
                val navPoints = it.tableOfContents.navPoints
                if(navPoints.isEmpty()){
                    showTOCBtn.visibility = View.GONE
                }else{
                    showTOCBtn.visibility = View.VISIBLE
                    showTOCBtn.setOnClickListener { showTableOfContents(navPoints) }
                }
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
        pagesAdapter = VisualizerAdapter(pages) { showTextDialog(it) }
        viewPager.adapter = pagesAdapter

        val position = viewModel.getPage()
        currentPageLabel.text = resources.getString(R.string.reader_current_page_label, position + 1, pages.size) // Example: 1 of 33
        viewPager.setCurrentItem(position, false)

    }

    private fun showBrightnessSettings(view: View) {
        val layout = LayoutInflater.from(this).inflate(R.layout.pop_up_brightness_settings, view.rootView as ViewGroup, false)
        val whiteBtn: Button = layout.findViewById(R.id.white_bg_btn)
        val beigeBtn: Button = layout.findViewById(R.id.beige_bg_btn)
        val blackBtn: Button = layout.findViewById(R.id.black_bg_btn)
        whiteBtn.setOnClickListener {setBackgroundColor(BrightnessTheme.WHITE)}
        beigeBtn.setOnClickListener {setBackgroundColor(BrightnessTheme.BEIGE)}
        blackBtn.setOnClickListener {setBackgroundColor(BrightnessTheme.BLACK)}

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

    private fun setBackgroundColor(theme: BrightnessTheme){
        if(theme != brightnessTheme) {
            saveBrightnessPreference(theme.value)
            viewModel.onFinish()
            finish()
            startActivity(intent)
        }
    }

    private fun saveBrightnessPreference(preference: String){
        val editor = preferences.edit()
        editor.putString(BrightnessTheme.PREFERENCE_KEY, preference)
        editor.apply()
    }

    private fun addPagerCallback(){
        var swipeFirst = false
        viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                viewModel.currentPage = position
                val pageNumber = position + 1
                currentPageLabel.text = resources.getString(R.string.reader_current_page_label, pageNumber, viewModel.pagesSize)
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
        val pageItemPadding = (80 * resources.displayMetrics.density + 0.5f).toInt() // Convert dp to px, 0.5 is for rounding to closest integer

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
            null
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
            detector?.let {
                if(!pinchDetected){
                    if(it.scaleFactor > PINCH_UPPER_LIMIT && !isFullScreen){
                        toggleImmersiveMode()
                        pinchDetected = true
                        return true
                    }
                    if(it.scaleFactor < PINCH_LOWER_LIMIT && isFullScreen){
                        toggleImmersiveMode()
                        pinchDetected = true
                        return true
                    }
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

    companion object{
        const val IMPORTED_TEXT = "imported_text"
        const val EPUB_URI = "epub_uri"

        const val SHOW_EPUB = "epub"
        const val FILE_ID = "fileId"

        const val PINCH_UPPER_LIMIT = 1.3f
        const val PINCH_LOWER_LIMIT = 0.8f
    }
}