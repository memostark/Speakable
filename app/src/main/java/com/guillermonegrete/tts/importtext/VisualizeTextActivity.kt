package com.guillermonegrete.tts.importtext

import android.net.Uri
import android.os.Bundle
import android.text.TextPaint
import android.util.Xml
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.importtext.epub.Book
import com.guillermonegrete.tts.importtext.epub.NavPoint
import org.xmlpull.v1.XmlPullParser

class VisualizeTextActivity: AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var currentPageLabel: TextView
    private lateinit var currentChapterLabel: TextView

    private val epubParser = EpubParser()

    private var book: Book? = null
    private var currentChapter = 0
    private var pagesSize = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visualize_text)

        val text = if(SHOW_EPUB == intent.action) {
            book = readEpubFile()
            book?.currentChapter ?: "No text"
        } else {
            intent?.extras?.getString(IMPORTED_TEXT) ?: "No text"
        }

        currentPageLabel= findViewById(R.id.reader_current_page)
        currentChapterLabel = findViewById(R.id.reader_current_chapter)

        viewPager = findViewById(R.id.text_reader_viewpager)
        viewPager.post{

            val pages = splitTextToPages(text)
            setUpPagerAndIndexLabel(pages)
            addPagerCallback()

        }

        val showTOCBtn = findViewById<ImageButton>(R.id.show_toc_btn)
        val navPoints = book?.tableOfContents?.navPoints

        if(navPoints == null || navPoints.isEmpty()){
            showTOCBtn.visibility = View.GONE
        }else{
            showTOCBtn.setOnClickListener { showTableOfContents(navPoints) }
        }

        updateCurrentChapterLabel(book?.spine)

    }

    private fun splitTextToPages(text: String): List<CharSequence>{
        val pageTextPaint = createTextPaint()

        val pageSplitter =  createPageSplitter()
        pageSplitter.append(text)
        pageSplitter.split(pageTextPaint)
        val mutablePages = pageSplitter.getPages().toMutableList()
        if (mutablePages.size == 1 && book != null) mutablePages.add("") // Don't allow one page chapters because you can't swipe to other chapters.
        return mutablePages
    }

    private fun setUpPagerAndIndexLabel(pages: List<CharSequence>){
        currentPageLabel.text = resources.getString(R.string.reader_current_page_label, 1, pages.size) // Example: 1 of 33
        viewPager.adapter = VisualizerAdapter(pages)
        pagesSize = pages.size
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
                            book?.let { swipeChapter(it, currentChapter - 1) }
                        } else if(position == pagesSize - 1) {
                            book?.let { swipeChapter(it, currentChapter + 1) }
                        }
                    }
                }
            }
        })
    }

    private fun readEpubFile(): Book {
        val uri: Uri = intent.getParcelableExtra(EPUB_URI)

        val rootStream = contentResolver.openInputStream(uri)
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)

        val zipReader = ZipFileReader(rootStream)

        return epubParser.parseBook(parser, zipReader)
    }

    private fun changeChapter(path: String){
        val uri: Uri = intent.getParcelableExtra(EPUB_URI)

        val rootStream = contentResolver.openInputStream(uri)
        if(rootStream != null) {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)

            val newText = epubParser.getChapterBodyTextFromPath(path, parser, rootStream)
            val pages = splitTextToPages(newText)

            setUpPagerAndIndexLabel(pages)
        }
    }

    private fun createPageSplitter(): PageSplitter{
        val lineSpacingExtra = resources.getDimension(R.dimen.visualize_page_text_line_spacing_extra)
        val lineSpacingMultiplier = 1f
        val pageItemPadding = (80 * resources.displayMetrics.density + 0.5f).toInt() // Convert dp to px, 0.5 is for rounding to closest integer

        val uri: Uri = intent.getParcelableExtra(EPUB_URI)
        val zipReader = ZipFileReader(contentResolver.openInputStream(uri))

        return PageSplitter(
            viewPager.width - pageItemPadding,
            viewPager.height - pageItemPadding,
            lineSpacingMultiplier,
            lineSpacingExtra,
            InputStreamImageGetter(epubParser.basePath, this, zipReader)
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
                changeChapter(path)
                book?.let {
                    val key = it.manifest.filterValues { value -> value == path }.keys.first()
                    val index = it.spine.indexOf(key)
                    if(index != -1) {
                        currentChapter = index
                        updateCurrentChapterLabel(it.spine)
                    }
                }
            }
            .create()
        dialog.show()
    }

    private fun swipeChapter(book: Book, position: Int){
        val spineSize = book.spine.size
        if(position in 0 until spineSize){
            val spineItem = book.spine[position]
            val newChapterPath = book.manifest[spineItem]
            if(newChapterPath != null) {

                currentChapter = position
                updateCurrentChapterLabel(book.spine)

                changeChapter(newChapterPath)
            }
        }
    }

    private fun updateCurrentChapterLabel(spine: List<String>?){
        if(spine == null || spine.isEmpty()){
            currentChapterLabel.visibility = View.GONE
        }else{
            currentChapterLabel.text = resources.getString(R.string.reader_current_chapter_label, currentChapter + 1, spine.size)
            currentChapterLabel.visibility = View.VISIBLE

        }
    }

    companion object{
        const val IMPORTED_TEXT = "imported_text"
        const val EPUB_URI = "epub_uri"

        const val SHOW_EPUB = "epub"
    }
}