package com.guillermonegrete.tts.textprocessing

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.distinctUntilChanged
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.databinding.FragmentProcessTranslationBinding
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.db.WordsDAO
import com.guillermonegrete.tts.ui.DifferentValuesAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TranslationFragment: Fragment(R.layout.fragment_process_translation) {

    private var word: Words? = null
    private var spinnerIndex: Int? = 0
    private var listener: Listener? = null

    private var _binding: FragmentProcessTranslationBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var wordsDAO: WordsDAO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        word = arguments?.getParcelable(ARGUMENT_WORD)
        spinnerIndex = arguments?.getInt(ARGUMENT_SPINNER_INDEX)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProcessTranslationBinding.bind(view)

        word?.let {word ->
            setWord(word)

            // If spinner index is set then it's a sentence layout
            if(spinnerIndex != -1) {
                with(binding) {
                    setSpinner(root)
                    definitionGroup.isVisible = false
                    translationGroup.isVisible = true
                    translationText.text = word.definition
                    copyTranslationButton.setOnClickListener {
                        saveTextToClipboard(word.definition)
                    }
                }
            } else {
                // Only query the database when it's a word layout
                wordsDAO.loadWordById(word.id).distinctUntilChanged().observe(viewLifecycleOwner) {
                    it ?: return@observe
                    setWord(it)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setWord(word: Words){
        with(binding){
            savedDefinitionText.text = word.definition
            val isEmpty = word.notes.isNullOrBlank()
            notesGroup.isGone = isEmpty
            if(!isEmpty) savedNotesText.text = word.notes

            copyDefinitionButton.setOnClickListener {
                saveTextToClipboard(word.definition)
            }
        }
    }

    private fun saveTextToClipboard(text: String){
        val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText("simple text", text)
        clipboard.setPrimaryClip(clip)
    }

    fun updateTranslation(word: Words){
        if(isAdded) {
            binding.translationText.text = word.definition
        }else{
            arguments?.putParcelable(ARGUMENT_WORD, word)
        }
    }

    fun setErrorLayout(){
        if(view != null) {
            binding.errorLayout.visibility = View.VISIBLE
            binding.allGroup.visibility = View.GONE
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