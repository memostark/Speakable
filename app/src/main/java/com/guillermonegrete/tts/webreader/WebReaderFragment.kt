package com.guillermonegrete.tts.webreader

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.Menu
import android.view.MenuInflater
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.guillermonegrete.tts.EventObserver
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.databinding.FragmentWebReaderBinding
import com.guillermonegrete.tts.textprocessing.TextInfoDialog
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class WebReaderFragment : Fragment(R.layout.fragment_web_reader){

    private val viewModel: WebReaderViewModel by viewModels()

    private  var _binding: FragmentWebReaderBinding? = null
    private val binding get() = _binding!!

    val args: WebReaderFragmentArgs by navArgs()

    private var clickedWord: String? = null

    private var adapter: ParagraphAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        _binding = FragmentWebReaderBinding.bind(view)

        with(binding){
            loadingIcon.isVisible = true

            viewModel.page.observe(viewLifecycleOwner, {
                bodyText.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(it, Html.FROM_HTML_MODE_COMPACT)
                } else {
                    Html.fromHtml(it)
                }

                bodyText.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        val offset = bodyText.getOffsetForPosition(event.x, event.y)
                        val possibleWord = findWordForRightHanded(bodyText.text.toString(), offset)
                        clickedWord = if(possibleWord.isBlank()) null else possibleWord
                    }
                    return@setOnTouchListener false
                }

                bodyText.setOnClickListener {
                    clickedWord?.let { word -> showTextDialog(word) }
                    clickedWord = null
                }

                loadingIcon.isVisible = false

                listToggle.isVisible = true
                listToggle.setOnClickListener {
                    onListToggleClick()
                }
            })

            viewModel.paragraphIndex.observe(viewLifecycleOwner, EventObserver {
                adapter?.notifyItemChanged(it)
            })
        }

        viewModel.loadDoc(args.link)
    }

    private fun onListToggleClick() {
        with(binding){
            if (paragraphsList.isVisible){
                paragraphsList.isVisible = false
                bodyText.isVisible = true
            } else {
                bodyText.isVisible = false
                paragraphsList.isVisible = true
                if(adapter == null) {
                    val paragraphs = viewModel.createParagraphs(bodyText.text.toString())
                    adapter = ParagraphAdapter(paragraphs, viewModel)
                }
                paragraphsList.adapter = adapter
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }

    private fun showTextDialog(text: CharSequence){
        // TODO try to add this as destination in jetpack navigation
        val dialog = TextInfoDialog.newInstance(
            text.toString(),
            TextInfoDialog.NO_SERVICE,
            null,
        )
        dialog.show(childFragmentManager, "Text_info")
    }

    private fun findWordForRightHanded(
        str: String,
        offset: Int
    ): String { // when you touch ' ', this method returns left word.
        var newOffset = offset
        if (str.length == newOffset) {
            newOffset-- // without this code, you will get exception when touching end of the text
        }
        if (str[newOffset] == ' ') {
            newOffset--
        }
        var startIndex = newOffset
        var endIndex = newOffset
        try {
            while (Character.isLetterOrDigit(str[startIndex])) {
                startIndex--
            }
        } catch (e: StringIndexOutOfBoundsException) {
            startIndex = 0
        }
        try {
            while (Character.isLetterOrDigit(str[endIndex])) {
                endIndex++
            }
        } catch (e: StringIndexOutOfBoundsException) {
            endIndex = str.length
        }

        return str.substring(startIndex, endIndex)
    }
}