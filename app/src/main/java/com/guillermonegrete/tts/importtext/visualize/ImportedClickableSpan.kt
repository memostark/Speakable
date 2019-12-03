package com.guillermonegrete.tts.importtext.visualize

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import com.guillermonegrete.tts.textprocessing.ProcessTextActivity

class ImportedClickableSpan(private val word: String): ClickableSpan() {

    private var clicked = false
    /**
     * Color taken from member variable mHighlightColor from TextView class.
     */
    private var highLightColor = 0x6633B5E5

    override fun onClick(widget: View) {
        clicked = true
        showTextDialog(widget.context)
    }

    override fun updateDrawState(ds: TextPaint) {
        ds.bgColor = if(clicked) highLightColor else Color.TRANSPARENT
        clicked = false
    }

    private fun showTextDialog(context: Context){
        val wiktionaryIntent = Intent(context, ProcessTextActivity::class.java)
        wiktionaryIntent.action = ProcessTextActivity.NO_SERVICE
        wiktionaryIntent.putExtra("android.intent.extra.PROCESS_TEXT", word)
        context.startActivity(wiktionaryIntent)
    }
}