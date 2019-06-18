package com.guillermonegrete.tts.importtext

import android.os.Bundle
import android.text.TextPaint
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.guillermonegrete.tts.R

class VisualizeTextActivity: AppCompatActivity() {

    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visualize_text)

        val text = intent?.extras?.getString(IMPORTED_TEXT) ?: "No text"

        val currentPageLabel: TextView = findViewById(R.id.reader_current_page)

        viewPager = findViewById(R.id.text_reader_viewpager)
        viewPager.post{
            val pageTextPaint = createTextPaint()

            val pageSplitter =  createPageSplitter()
            pageSplitter.append(text)
            pageSplitter.split(pageTextPaint)
            val pages = pageSplitter.getPages()

            currentPageLabel.text = resources.getString(R.string.reader_current_page_label, 1, pages.size) // Example: 1 of 33
            viewPager.adapter = VisualizerAdapter(pages)
            viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){
                override fun onPageSelected(position: Int) {
                    val pageNumber = position + 1
                    currentPageLabel.text = resources.getString(R.string.reader_current_page_label, pageNumber, pages.size)
                }
            })
        }

    }

    private fun createPageSplitter(): PageSplitter{
        val lineSpacingExtra = resources.getDimension(R.dimen.visualize_page_text_line_spacing_extra)
        val lineSpacingMultiplier = 1f
        val pageItemPadding = (80 * resources.displayMetrics.density + 0.5f).toInt() // Convert dp to px, 0.5 is for rounding to closest integer

        return PageSplitter(viewPager.width - pageItemPadding, viewPager.height - pageItemPadding, lineSpacingMultiplier, lineSpacingExtra)
    }

    private fun createTextPaint(): TextPaint{
        val textSize = resources.getDimension(R.dimen.text_size_large)
        val pageTextPaint = TextPaint()
        pageTextPaint.textSize = textSize
        return pageTextPaint
    }

    companion object{
        const val IMPORTED_TEXT = "imported_text"
    }
}