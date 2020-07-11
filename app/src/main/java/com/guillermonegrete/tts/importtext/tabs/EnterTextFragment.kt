package com.guillermonegrete.tts.importtext.tabs

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.databinding.EnterTextLayoutBinding
import com.guillermonegrete.tts.importtext.visualize.VisualizeTextActivity

class EnterTextFragment: Fragment(R.layout.enter_text_layout) {

    private  var _binding: EnterTextLayoutBinding? = null
    private val binding get() = _binding!!

    private lateinit var clipboardManager: ClipboardManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        clipboardManager = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = EnterTextLayoutBinding.bind(view)

        val editText: EditText = binding.importTextEdit

        binding.pasteBtn.setOnClickListener { editText.setText(getClipboardText()) }

        binding.visualizeBtn.setOnClickListener { visualizeText(editText.text.toString()) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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