package com.guillermonegrete.tts.importtext.visualize

import android.os.Build
import android.text.*
import android.widget.TextView
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PageSplitter(
    textView: TextView,
    private val imageGetter: Html.ImageGetter?,
) {
    private val pages = ArrayList<CharSequence>()
    private val mSpannableStringBuilder = SpannableStringBuilder()

    private val pageWidth = textView.width - textView.paddingStart - textView.paddingEnd
    private val pageHeight = textView.height - textView.paddingTop - textView.paddingBottom

    private val lineSpacingMultiplier = textView.lineSpacingMultiplier
    private val lineSpacingExtra = textView.lineSpacingExtra
    private val textPaint = textView.paint
    private val includeFontPadding = textView.includeFontPadding
    private val alignment: Layout.Alignment = textView.layout.alignment
    private val maxLines = textView.maxLines

    @RequiresApi(Build.VERSION_CODES.M)
    private var breakStrategy = 0
    @RequiresApi(Build.VERSION_CODES.M)
    private var hyphenationFrequency = 0

    @RequiresApi(Build.VERSION_CODES.O)
    private var justificationMode = 0

    @RequiresApi(Build.VERSION_CODES.P)
    private var isFallbackLineSpacing = false

    init {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            breakStrategy = textView.breakStrategy
            hyphenationFrequency = textView.hyphenationFrequency
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            justificationMode = textView.justificationMode
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            isFallbackLineSpacing = textView.isFallbackLineSpacing
        }
    }

    fun setText(charSequence: CharSequence){
        pages.clear()
        mSpannableStringBuilder.clear()
        append(charSequence)
    }

    private fun append(charSequence: CharSequence) {
        mSpannableStringBuilder.append(charSequence)
    }

    suspend fun split() {
        withContext(Dispatchers.Default){
            val formattedText = formatHtml(mSpannableStringBuilder)
            val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(formattedText, 0, formattedText.length, textPaint, pageWidth)
                    .setAlignment(alignment)
                    .setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
                    .setIncludePad(includeFontPadding)
                    .setUseLineSpacingFromFallbacks()
                    .setBreakStrategy(breakStrategy)
                    .setHyphenationFrequency(hyphenationFrequency)
                    .setJustificationMode()
                    .setMaxLines(maxLines)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(
                    formattedText,
                    textPaint,
                    pageWidth,
                    Layout.Alignment.ALIGN_NORMAL,
                    lineSpacingMultiplier,
                    lineSpacingExtra,
                    includeFontPadding
                )
            }

            splitLineByLine(staticLayout)
        }
    }

    /**
     * Splitting method taken from: https://stackoverflow.com/questions/31837840/paginating-text-in-android
     */
    private fun splitLineByLine(layout: StaticLayout){
        val lines = layout.lineCount
        val text = layout.text
        var startOffset = 0
        var height = pageHeight

        for (i in 0 until lines) {
            if (height < layout.getLineBottom(i)) {
                // When the layout height has been exceeded
                pages.add(text.subSequence(startOffset, layout.getLineStart(i)))
                startOffset = layout.getLineStart(i)
                height = layout.getLineTop(i) + pageHeight
            }

            if (i == lines - 1) {
                // Put the rest of the text into the last page
                pages.add(text.subSequence(startOffset, layout.getLineEnd(i)))
                return
            }
        }
    }

    private fun formatHtml(text: CharSequence): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(text.toString(), Html.FROM_HTML_MODE_COMPACT, imageGetter, null)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(text.toString(), imageGetter, null)
        }
    }

    private fun StaticLayout.Builder.setUseLineSpacingFromFallbacks(): StaticLayout.Builder {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            this.setUseLineSpacingFromFallbacks(isFallbackLineSpacing)
        }

        return this
    }

    private fun StaticLayout.Builder.setJustificationMode(): StaticLayout.Builder {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.setJustificationMode(justificationMode)
        }

        return this
    }

    fun getPages(): List<CharSequence> {
        return pages
    }
}