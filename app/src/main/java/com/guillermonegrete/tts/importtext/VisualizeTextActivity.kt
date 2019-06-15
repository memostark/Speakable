package com.guillermonegrete.tts.importtext

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.guillermonegrete.tts.R
import java.text.BreakIterator
import java.util.*



class VisualizeTextActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visualize_text)

        val text = intent?.extras?.getString(IMPORTED_TEXT) ?: "No text"

        val page1Text: TextView = findViewById(R.id.page1)
        page1Text.movementMethod = LinkMovementMethod.getInstance()
        page1Text.setText(text, TextView.BufferType.SPANNABLE)
        page1Text.highlightColor = Color.TRANSPARENT
        setSpannables(page1Text, text)
    }

    private fun setSpannables(view: TextView, text: String){
        val spans = view.text as Spannable
        val iterator = BreakIterator.getWordInstance(Locale.US)
        iterator.setText(text)
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            val possibleWord = text.substring(start, end)
            if (Character.isLetterOrDigit(possibleWord.first())) {
                val clickSpan = ImportedClickableSpan(possibleWord)
                spans.setSpan(
                    clickSpan, start, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            start = end
            end = iterator.next()
        }
    }

    companion object{
        const val IMPORTED_TEXT = "imported_text"
    }
}