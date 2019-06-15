package com.guillermonegrete.tts.importtext

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.guillermonegrete.tts.R

class ImportTextFragment: Fragment() {

    private lateinit var clipboardManager: ClipboardManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        clipboardManager = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_import_text, container, false)
        val editText: TextView = root.findViewById(R.id.import_text_edit)

        val pasteButton: Button = root.findViewById(R.id.paste_btn)
        pasteButton.setOnClickListener { editText.text = getClipboardText() }

        val visualizeButton: Button = root.findViewById(R.id.visualize_btn)
        visualizeButton.setOnClickListener {
            val intent = Intent(context, VisualizeTextActivity::class.java)
            intent.putExtra(VisualizeTextActivity.IMPORTED_TEXT, editText.text.toString())
            startActivity(intent)
        }

        return root
    }

    private fun getClipboardText(): String{

        val clip = clipboardManager.primaryClip ?: return ""
        if (clip.itemCount <= 0) return ""
        val pasteData = clip.getItemAt(0).text
        return pasteData?.toString() ?: ""
    }

}