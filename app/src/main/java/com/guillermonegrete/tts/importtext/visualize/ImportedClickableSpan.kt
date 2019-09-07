package com.guillermonegrete.tts.importtext.visualize

import android.content.Intent
import android.graphics.Color
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import com.guillermonegrete.tts.textprocessing.ProcessTextActivity

class ImportedClickableSpan(val word: String): ClickableSpan() {

    var clicked = false
    private var highLightColor = 0x6633B5E5

    override fun onClick(widget: View) {
        val textview = widget as TextView
        highLightColor = textview.highlightColor
        clicked = true
        widget.invalidate()

        val context = widget.context
        val wiktionaryIntent = Intent(context, ProcessTextActivity::class.java)
        wiktionaryIntent.action = ProcessTextActivity.NO_SERVICE
        wiktionaryIntent.putExtra("android.intent.extra.PROCESS_TEXT", word)
        context.startActivity(wiktionaryIntent)
    }

    override fun updateDrawState(ds: TextPaint) {
        ds.bgColor = if(clicked)  highLightColor else Color.TRANSPARENT
        clicked = false
    }


}