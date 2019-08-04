package com.guillermonegrete.tts.importtext

import android.net.Uri
import android.os.Bundle
import android.text.TextPaint
import android.util.Xml
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager2.widget.ViewPager2
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.ViewModelFactory
import com.guillermonegrete.tts.importtext.epub.NavPoint
import dagger.android.AndroidInjection
import org.xmlpull.v1.XmlPullParser
import javax.inject.Inject

class VisualizeTextActivity: AppCompatActivity() {

    @Inject lateinit var viewModel: VisualizeTextViewModel

    private lateinit var viewPager: ViewPager2
    private lateinit var currentPageLabel: TextView
    private lateinit var currentChapterLabel: TextView

    private lateinit var fileReader: ZipFileReader
    private lateinit var parser: XmlPullParser

    private val epubParser = EpubParser()

    private var pagesSize = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        setPreferenceTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visualize_text)

        val uri: Uri = intent.getParcelableExtra(EPUB_URI)
        val rootStream = contentResolver.openInputStream(uri)
        fileReader = ZipFileReader(rootStream)

        parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)

        createViewModel()

        if(SHOW_EPUB == intent.action) {
            viewModel.parseEpub(parser, fileReader)
        } else {
            viewModel.parseSimpleText(intent?.extras?.getString(IMPORTED_TEXT) ?: "No text")
        }

        currentPageLabel= findViewById(R.id.reader_current_page)
        currentChapterLabel = findViewById(R.id.reader_current_chapter)

        val brightnessButton: ImageButton = findViewById(R.id.brightness_settings_btn)
        brightnessButton.setOnClickListener { showBrightnessSettings(it) }

        viewPager = findViewById(R.id.text_reader_viewpager)
        viewPager.post{
            viewModel.splitToPages(createPageSplitter(), createTextPaint())
            addPagerCallback()
        }
    }


    // Change activity theme at runtime: https://stackoverflow.com/a/6390025/10244759
    private fun setPreferenceTheme() {
        when(intent.getStringExtra(BRIGHTNESS_THEME)){
            "White" -> setTheme(R.style.AppMaterialTheme_White)
            "Beige" -> setTheme(R.style.AppMaterialTheme_Beige)
            "Black" -> setTheme(R.style.AppMaterialTheme_Black)
            else -> setTheme(R.style.AppMaterialTheme_White)
        }
    }

    private fun createViewModel() {
        val factory = ViewModelFactory.getInstance(application)
//        viewModel = ViewModelProviders.of(this, factory).get(VisualizeTextViewModel::class.java).apply {
          viewModel.apply {
            pages.observe(this@VisualizeTextActivity, Observer {
                println("Pages observer")
                setUpPagerAndIndexLabel(it)
            })

            book.observe(this@VisualizeTextActivity, Observer {
                println("Book observer")
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

            chapterPath.observe(this@VisualizeTextActivity, Observer {
                println("Path observer")
                viewModel.changeEpubChapter(it, parser, fileReader)
                viewModel.splitToPages(createPageSplitter(), createTextPaint())

                updateCurrentChapterLabel()
            })
        }
    }

    private fun setUpPagerAndIndexLabel(pages: List<CharSequence>){
        currentPageLabel.text = resources.getString(R.string.reader_current_page_label, 1, pages.size) // Example: 1 of 33
        viewPager.adapter = VisualizerAdapter(pages)
        pagesSize = pages.size
    }

    private fun showBrightnessSettings(view: View) {
        val layout = LayoutInflater.from(this).inflate(R.layout.pop_up_brightness_settings, view.rootView as ViewGroup, false)
        val whiteBtn: Button = layout.findViewById(R.id.white_bg_btn)
        val beigeBtn: Button = layout.findViewById(R.id.beige_bg_btn)
        val blackBtn: Button = layout.findViewById(R.id.black_bg_btn)
        whiteBtn.setOnClickListener {setBackgroundColor("White")}
        beigeBtn.setOnClickListener {setBackgroundColor("Beige")}
        blackBtn.setOnClickListener {setBackgroundColor("Black")}

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

    private fun setBackgroundColor(text: String){
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        val intent = intent
        intent.putExtra(BRIGHTNESS_THEME, text)
        finish()
        startActivity(intent)
    }

    private fun addPagerCallback(){
        var swipeFirst = false
        viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                val pageNumber = position + 1
                currentPageLabel.text = resources.getString(R.string.reader_current_page_label, pageNumber, pagesSize)
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
                        } else if(position == pagesSize - 1) {
                            viewModel.swipeChapterRight()
                        }
                    }
                }
            }
        })
    }

    private fun createPageSplitter(): PageSplitter{
        val lineSpacingExtra = resources.getDimension(R.dimen.visualize_page_text_line_spacing_extra)
        val lineSpacingMultiplier = 1f
        val pageItemPadding = (80 * resources.displayMetrics.density + 0.5f).toInt() // Convert dp to px, 0.5 is for rounding to closest integer

        val uri: Uri? = intent.getParcelableExtra(EPUB_URI)
        val imageGetter = if(uri != null) {
            val zipReader = ZipFileReader(contentResolver.openInputStream(uri))
            InputStreamImageGetter(epubParser.basePath, this, zipReader)
        } else null

        return PageSplitter(
            viewPager.width - pageItemPadding,
            viewPager.height - pageItemPadding,
            lineSpacingMultiplier,
            lineSpacingExtra,
            imageGetter
        )
    }

    private fun createTextPaint(): TextPaint{
        val textSize = resources.getDimension(R.dimen.text_size_large)
        val pageTextPaint = TextPaint()
        pageTextPaint.textSize = textSize
        return pageTextPaint
    }

    private fun showTableOfContents(navPoints: List<NavPoint>){

        val filePaths = navPoints.map { it.getContentWithoutTag() }
        val titles = navPoints.map { it.navLabel }

        val adapter = ArrayAdapter(this, android.R.layout.select_dialog_item, titles)

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

    companion object{
        const val IMPORTED_TEXT = "imported_text"
        const val EPUB_URI = "epub_uri"

        const val SHOW_EPUB = "epub"

        private const val BRIGHTNESS_THEME = "theme"
    }
}