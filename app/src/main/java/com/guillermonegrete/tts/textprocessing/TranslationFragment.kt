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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asFlow
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.databinding.FragmentProcessTranslationBinding
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.db.WordsDAO
import com.guillermonegrete.tts.ui.DifferentValuesAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TranslationFragment: Fragment(R.layout.fragment_process_translation) {

    private var word: Words? = null
    private var spinnerIndex: Int? = 0
    private var listener: Listener? = null

    private var _binding: FragmentProcessTranslationBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var wordsDAO: WordsDAO

    private var wordJob: Job? = null
    private val _wordId = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        word = arguments?.getParcelable(ARGUMENT_WORD)
        spinnerIndex = arguments?.getInt(ARGUMENT_SPINNER_INDEX)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProcessTranslationBinding.bind(view)

        word?.let {word ->

            // If spinner index is set then it's a sentence layout
            if(word.id == NOT_SAVED_ID) {
                setSpinnerLayout(word)
            } else {
                // Only query the database when it's a word layout
                _wordId.value = word.id
                launchWordJob()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

    @OptIn(ExperimentalCoroutinesApi::class)
    fun launchWordJob() {
        wordJob = lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                _wordId.flatMapLatest { id ->
                    wordsDAO.loadWordById(id).distinctUntilChanged().asFlow()
                }.collect { dbWord ->
                    if (dbWord == null) {
                        word?.let { setSpinnerLayout(it) }
                    } else {
                        setSavedWordLayout(dbWord)
                    }
                }
            }
        }
    }

    fun setWordId(id: Int) {
        _wordId.value = id
        if (wordJob == null) {
            launchWordJob()
        }
    }

    private fun setSpinnerLayout(word: Words) {
        val arrayAdapter = DifferentValuesAdapter.createFromResource(
            requireContext(),
            R.array.googleTranslateLanguagesValue,
            R.array.googleTranslateLanguagesArray,
            android.R.layout.simple_spinner_item
        )
        // Specify the layout to use when the list of choices appears
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Apply the adapter to the spinner
        with (binding) {
            translateToSpinner.apply {
                adapter = arrayAdapter
                spinnerIndex?.let { setSelection(it, false) }
                post { onItemSelectedListener = SpinnerListener() } // the post{} avoids the listener being called
            }

            definitionGroup.isVisible = false
            notesGroup.isVisible = false
            translationGroup.isVisible = true
            translationText.text = word.definition
            copyTranslationButton.setOnClickListener {
                saveTextToClipboard(word.definition)
            }
        }
    }

    private fun setSavedWordLayout(word: Words) {
        with(binding){
            savedDefinitionText.text = word.definition
            val isEmpty = word.notes.isNullOrBlank()
            notesGroup.isGone = isEmpty
            if(!isEmpty) savedNotesText.text = word.notes

            copyDefinitionButton.setOnClickListener {
                saveTextToClipboard(word.definition)
            }
            definitionGroup.isVisible = true
            translationGroup.isVisible = false
            translateToSpinner.adapter = null
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