package com.guillermonegrete.tts.webreader

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.databinding.FragmentWebReaderBinding
import com.guillermonegrete.tts.textprocessing.TextInfoDialog

class WebReaderFragment : Fragment(R.layout.fragment_web_reader){

    private val viewModel: WebReaderViewModel by viewModels()

    private  var _binding: FragmentWebReaderBinding? = null
    private val binding get() = _binding!!

    val args: WebReaderFragmentArgs by navArgs()

    private var clickedWord: String? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        _binding = FragmentWebReaderBinding.bind(view)

        viewModel.page.observe(viewLifecycleOwner, {
            binding.bodyText.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(it, Html.FROM_HTML_MODE_COMPACT)
            } else {
                Html.fromHtml(it)
            }

            binding.bodyText.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val offset = binding.bodyText.getOffsetForPosition(event.x, event.y)
                    clickedWord = findWordForRightHanded(binding.bodyText.text.toString(), offset)
                }
                return@setOnTouchListener false
            }

            binding.bodyText.setOnClickListener {
                clickedWord?.let { word -> showTextDialog(word) }
                clickedWord = null
            }
        })

        viewModel.loadDoc(args.link)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
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
            while (str[startIndex] != ' ' && str[startIndex] != '\n') {
                startIndex--
            }
        } catch (e: StringIndexOutOfBoundsException) {
            startIndex = 0
        }
        try {
            while (str[endIndex] != ' ' && str[endIndex] != '\n') {
                endIndex++
            }
        } catch (e: StringIndexOutOfBoundsException) {
            endIndex = str.length
        }

        // without this code, you will get 'here!' instead of 'here'
        // if you use only english, just check whether this is alphabet,
        // but 'I' use korean, so i use below algorithm to get clean word.
        val last = str[endIndex - 1]
        if (last == ',' || last == '.' || last == '!' || last == '?' || last == ':' || last == ';') {
            endIndex--
        }
        return str.substring(startIndex, endIndex)
    }
}