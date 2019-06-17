package com.guillermonegrete.tts.importtext

import android.os.Build
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint

/**
 *  Taken from: https://stackoverflow.com/a/30468884/10244759
 *  Also useful: https://stackoverflow.com/a/20204349/10244759
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
        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(mSpannableStringBuilder, 0, mSpannableStringBuilder.length, textPaint, pageWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
                .setIncludePad(false)
                .build()
        } else {
            StaticLayout(
                mSpannableStringBuilder,
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
            pages.add(mSpannableStringBuilder.subSequence(startOffset, endOffset))
            startLine = lastFullyVisibleLine + 1
        }
    }

    fun getPages(): List<CharSequence> {
        return pages
    }
}