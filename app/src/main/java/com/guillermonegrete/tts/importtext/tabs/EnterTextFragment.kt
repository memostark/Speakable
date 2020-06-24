package com.guillermonegrete.tts.importtext.tabs

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.importtext.visualize.VisualizeTextActivity

class EnterTextFragment: Fragment() {

    private lateinit var clipboardManager: ClipboardManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        clipboardManager = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.enter_text_layout, container, false)

        val editText: EditText = root.findViewById(R.id.import_text_edit)

        val pasteButton: Button = root.findViewById(R.id.paste_btn)
        pasteButton.setOnClickListener { editText.setText(getClipboardText()) }

        val visualizeButton: Button = root.findViewById(R.id.visualize_btn)
        visualizeButton.setOnClickListener {
            visualizeText(editText.text.toString())
        }

        return root
    }

    private fun getClipboardText(): String{

        val clip = clipboardManager.primaryClip ?: return ""
        if (clip.itemCount <= 0) return ""
        val pasteData = clip.getItemAt(0).text
        return pasteData?.toString() ?: ""
    }

    private fun visualizeText(text: String){
        val intent = Intent(context, VisualizeTextActivity::class.java)
        intent.putExtra(VisualizeTextActivity.IMPORTED_TEXT, text)
        startActivity(intent)
    }
}