package com.guillermonegrete.tts.importtext.visualize

import android.os.Build
import android.text.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 *  Taken from: https://stackoverflow.com/a/30468884/10244759
 *  More precise version although slower, should implement: https://stackoverflow.com/questions/31837840/paginating-text-in-android
 *
 */
class PageSplitter(
    private val pageWidth: Int,
    private val pageHeight: Int,
    private val lineSpacingMultiplier: Float,
    private val lineSpacingExtra: Float,
    private val textPaint: TextPaint,
    private val includeFontPadding: Boolean,
    val imageGetter: Html.ImageGetter?
) {
    private val pages = ArrayList<CharSequence>()
    private val mSpannableStringBuilder = SpannableStringBuilder()

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
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
                    .setIncludePad(includeFontPadding)
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

    fun getPages(): List<CharSequence> {
        return pages
    }
}