package com.guillermonegrete.tts.TextProcessing

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

class TranslationFragment: Fragment() {

    private var word: Words? = null
    private var spinnerIndex: Int? = 0
    private var listener: Listener? = null

    private lateinit var translationEditText: TextView

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
                    findViewById<TextView>(R.id.saved_notes_label).visibility = View.GONE
                    findViewById<TextView>(R.id.saved_notes_text).visibility = View.GONE
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
            }
        }

        return root
    }

    fun saveTextToClipboard(text: String){
        val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText("simple text", text)
        clipboard.primaryClip = clip
    }

    fun updateTranslation(translation: String){
        translationEditText.text = translation
    }

    fun setListener(listener: Listener){
        this.listener = listener
    }

    private fun setSpinner(root: View) {
        val spinner = root.findViewById<Spinner>(R.id.translate_to_spinner)
        setSpinnerPopUpHeight(spinner)
        val arrayAdapter = ArrayAdapter.createFromResource(
            context,
            R.array.googleTranslateLanguagesArray, android.R.layout.simple_spinner_item
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

    // Taken from: https://stackoverflow.com/questions/20597584/how-to-limit-the-height-of-spinner-drop-down-view-in-android
    private fun setSpinnerPopUpHeight(spinner: Spinner) {
        try {
            val popup = Spinner::class.java.getDeclaredField("mPopup")
            popup.isAccessible = true

            // Get private mPopup member variable and try cast to ListPopupWindow
            val popupWindow = popup.get(spinner) as android.widget.ListPopupWindow

            // Set popupWindow height to 500px
            popupWindow.height = 300
        } catch (e: NoClassDefFoundError) {
            // silently fail...
        } catch (e: ClassCastException) {
        } catch (e: NoSuchFieldException) {
        } catch (e: IllegalAccessException) {
        }

    }

    inner class SpinnerListener: AdapterView.OnItemSelectedListener {

        override fun onNothingSelected(parent: AdapterView<*>?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

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