package com.guillermonegrete.tts.TextProcessing

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.db.Words

class TranslationFragment: Fragment() {

    private var word: Words? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        word = arguments?.getParcelable(ARGUMENT_WORD)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_process_translation, container, false)

        word?.let {word ->
            with(root){
                findViewById<TextView>(R.id.saved_definition_text).text = word.definition
                if(word.notes.isNullOrBlank()) {
                    findViewById<TextView>(R.id.saved_notes_label).visibility = View.GONE
                    findViewById<TextView>(R.id.saved_notes_text).visibility = View.GONE
                }else findViewById<TextView>(R.id.saved_notes_text).text = word.notes

                findViewById<ImageButton>(R.id.copy_definition_button).setOnClickListener {
                    val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip: ClipData = ClipData.newPlainText("simple text", word.definition)
                    clipboard.primaryClip = clip
                }
            }
        }

        return root
    }

    companion object {

         private const val ARGUMENT_WORD = "word"

         @JvmStatic fun newInstance(word: Words?) = TranslationFragment().apply {
            arguments = Bundle().apply { putParcelable(ARGUMENT_WORD, word) }
         }
    }
}