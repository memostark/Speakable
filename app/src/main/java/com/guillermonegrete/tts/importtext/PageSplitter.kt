package com.guillermonegrete.tts.importtext

import android.os.Build
import android.text.*

/**
 *  Taken from: https://stackoverflow.com/a/30468884/10244759
 *  More precise version although slower, should implement: https://stackoverflow.com/questions/31837840/paginating-text-in-android
 *
 */
class PageSplitter(
    private val pageWidth: Int,
    private val pageHeight: Int,
    private val lineSpacingMultiplier: Float,
    private val lineSpacingExtra: Float
) {
    private val pages = ArrayList<CharSequence>()
    private val mSpannableStringBuilder = SpannableStringBuilder()

    fun append(charSequence: CharSequence) {
        mSpannableStringBuilder.append(charSequence)
    }

    fun split(textPaint: TextPaint) {
        val formattedText = formatHtml(mSpannableStringBuilder)
        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(formattedText, 0, formattedText.length, textPaint, pageWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
                .setIncludePad(false)
                .build()
        } else {
            StaticLayout(
                formattedText,
                textPaint,
                pageWidth,
                Layout.Alignment.ALIGN_NORMAL,
                lineSpacingMultiplier,
                lineSpacingExtra,
                false
            )
        }

        var startLine = 0
        while (startLine < staticLayout.lineCount) {
            val startLineTop = staticLayout.getLineTop(startLine)
            val endLine = staticLayout.getLineForVertical(startLineTop + pageHeight)
            val endLineBottom = staticLayout.getLineBottom(endLine)

            val lastFullyVisibleLine: Int =
                if (endLineBottom > startLineTop + pageHeight) endLine - 1
                else endLine
            val startOffset = staticLayout.getLineStart(startLine)
            val endOffset = staticLayout.getLineEnd(lastFullyVisibleLine)
            pages.add(formattedText.subSequence(startOffset, endOffset))
            startLine = lastFullyVisibleLine + 1
        }
    }

    private fun formatHtml(text: CharSequence): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(text.toString(), Html.FROM_HTML_MODE_COMPACT)
        } else {
            Html.fromHtml(text.toString())
        }
    }

    fun getPages(): List<CharSequence> {
        return pages
    }
}