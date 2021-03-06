package com.guillermonegrete.tts.textprocessing

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.Fragment
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.ui.DifferentValuesAdapter

class TranslationFragment: Fragment() {

    private var word: Words? = null
    private var spinnerIndex: Int? = 0
    private var listener: Listener? = null

    private lateinit var translationEditText: TextView
    private lateinit var errorLayout: View
    private lateinit var allGroup: Group

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        word = arguments?.getParcelable(ARGUMENT_WORD)
        spinnerIndex = arguments?.getInt(ARGUMENT_SPINNER_INDEX)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_process_translation, container, false)

        word?.let {word ->
            with(root){
                findViewById<TextView>(R.id.saved_definition_text).text = word.definition
                if(word.notes.isNullOrBlank()) {
                    findViewById<Group>(R.id.notes_group).visibility = View.GONE
                }else findViewById<TextView>(R.id.saved_notes_text).text = word.notes

                findViewById<ImageButton>(R.id.copy_definition_button).setOnClickListener {
                    saveTextToClipboard(word.definition)
                }

                if(spinnerIndex != -1) {
                    setSpinner(this)
                    findViewById<Group>(R.id.definition_group).visibility = View.GONE
                    findViewById<Group>(R.id.translation_group).visibility = View.VISIBLE
                    translationEditText = findViewById(R.id.translation_text)
                    translationEditText.text = word.definition
                    findViewById<ImageButton>(R.id.copy_translation_button).setOnClickListener {
                        saveTextToClipboard(word.definition)
                    }
                }

                errorLayout = findViewById(R.id.error_layout)
                allGroup = findViewById(R.id.all_group)
            }
        }

        return root
    }

    private fun saveTextToClipboard(text: String){
        val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText("simple text", text)
        clipboard.primaryClip = clip
    }

    fun updateTranslation(word: Words){
        if(isAdded) {
            translationEditText.text = word.definition
        }else{
            arguments?.putParcelable(ARGUMENT_WORD, word)
        }
    }

    fun setErrorLayout(){
        if(view != null) {
            errorLayout.visibility = View.VISIBLE
            allGroup.visibility = View.GONE
        }
    }

    fun setListener(listener: Listener){
        this.listener = listener
    }

    private fun setSpinner(root: View) {
        val spinner = root.findViewById<Spinner>(R.id.translate_to_spinner)
        val arrayAdapter = DifferentValuesAdapter.createFromResource(
            requireContext(),
            R.array.googleTranslateLanguagesValue,
            R.array.googleTranslateLanguagesArray,
            android.R.layout.simple_spinner_item
        )
        // Specify the layout to use when the list of choices appears
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Apply the adapter to the spinner
        spinner.apply {
            adapter = arrayAdapter
            spinnerIndex?.let { setSelection(it, false) }
            onItemSelectedListener = SpinnerListener()
        }

    }

    inner class SpinnerListener: AdapterView.OnItemSelectedListener {

        override fun onNothingSelected(parent: AdapterView<*>?) {}

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            listener?.onItemSelected(position)
        }

    }

    interface Listener{
        fun onItemSelected(position: Int)
    }

    companion object {

         private const val ARGUMENT_WORD = "word"
        private const val ARGUMENT_SPINNER_INDEX = "spinnerIndex"

         @JvmStatic fun newInstance(word: Words, spinnerIndex: Int) = TranslationFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARGUMENT_WORD, word)
                putInt(ARGUMENT_SPINNER_INDEX, spinnerIndex)
            }
         }
    }
}