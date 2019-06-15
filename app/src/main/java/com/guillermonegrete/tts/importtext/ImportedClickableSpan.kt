package com.guillermonegrete.tts.importtext

import android.content.Intent
import android.graphics.Color
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Toast
import com.guillermonegrete.tts.textprocessing.ProcessTextActivity

class ImportedClickableSpan(val word: String): ClickableSpan() {

    override fun onClick(widget: View) {
        Toast.makeText(widget.context, word, Toast.LENGTH_SHORT).show()
        val context = widget.context
        val wiktionaryIntent = Intent(context, ProcessTextActivity::class.java)
        wiktionaryIntent.action = ProcessTextActivity.NO_SERVICE
        wiktionaryIntent.putExtra("android.intent.extra.PROCESS_TEXT", word)
        context.startActivity(wiktionaryIntent)
    }

    override fun updateDrawState(ds: TextPaint) {
        ds.color = Color.BLACK
        ds.isUnderlineText = false
    }
}