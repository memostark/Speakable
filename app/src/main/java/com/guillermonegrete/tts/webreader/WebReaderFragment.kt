package com.guillermonegrete.tts.webreader

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.Menu
import android.view.MenuInflater
import android.view.MotionEvent
import android.view.View
import android.webkit.WebViewClient
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.databinding.FragmentWebReaderBinding
import com.guillermonegrete.tts.textprocessing.ExternalLinksAdapter
import com.guillermonegrete.tts.textprocessing.TextInfoDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WebReaderFragment : Fragment(R.layout.fragment_web_reader){

    private val viewModel: WebReaderViewModel by viewModels()

    private  var _binding: FragmentWebReaderBinding? = null
    private val binding get() = _binding!!

    private val args: WebReaderFragmentArgs by navArgs()

    private var clickedWord: String? = null
    private var languageFrom: String? = null

    private var adapter: ParagraphAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onPause() {
        super.onPause()
        viewModel.saveWebLink()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        _binding = FragmentWebReaderBinding.bind(view)

        with(binding){
            loadingIcon.isVisible = true

            viewModel.page.observe(viewLifecycleOwner) {
                bodyText.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(it, Html.FROM_HTML_MODE_COMPACT)
                } else {
                    Html.fromHtml(it)
                }

                bodyText.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        val offset = bodyText.getOffsetForPosition(event.x, event.y)
                        val possibleWord = findWordForRightHanded(bodyText.text.toString(), offset)
                        clickedWord = possibleWord.ifBlank { null }
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
            }

            viewModel.translatedParagraph.observe(viewLifecycleOwner) { result ->
                val adapter = adapter ?: return@observe
                adapter.isLoading = when (result) {
                    LoadResult.Loading -> true
                    is LoadResult.Success, is LoadResult.Error -> false
                }
                adapter.updateExpanded()
            }

            viewModel.webLink.observe(viewLifecycleOwner) {
                val langShortNames = resources.getStringArray(R.array.googleTranslateLangsWithAutoValue)
                languageFrom = it.language ?: langShortNames.first() // First is always "auto"
                val langAdapter = ArrayAdapter.createFromResource(
                    requireContext(),
                    R.array.googleTranslateLangsWithAutoArray,
                    android.R.layout.simple_spinner_dropdown_item
                )
                setLanguage.adapter = langAdapter
                val index = langShortNames.indexOf(languageFrom)
                setLanguage.prompt = getString(R.string.web_reader_lang_spinner_prompt)
                setLanguage.setSelection(index, false)
                setLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val langShort = if (position == 0) null else langShortNames[position]
                        languageFrom = langShort
                        viewModel.setLanguage(langShort)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }

            setBottomPanel()
        }

        viewModel.loadDoc(args.link)
    }

    private fun onListToggleClick() {
        with(binding){
            val listVisible = paragraphsList.isVisible
            if (listVisible){
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

            listToggle.setImageResource(if(listVisible) R.drawable.ic_list_grey_24dp else R.drawable.ic_baseline_arrow_left_24)
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
            languageFrom = languageFrom
        )
        dialog.show(childFragmentManager, "Text_info")
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setBottomPanel(){
        with(binding) {
            val bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            bottomSheetBehavior.addBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) menuBar.isVisible = true
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })

            infoWebview.webViewClient = WebViewClient()
            infoWebview.settings.javaScriptEnabled = true
            // This disables scrolling the bottom layout when scrolling the WebView
            // This is done to allow the web view to scroll up
            infoWebview.setOnTouchListener { view, _ ->
                view.parent.requestDisallowInterceptTouchEvent(true)
                false
            }

            // This allows to scroll both the WebView and the list, otherwise only the list scrolls
            linksList.isNestedScrollingEnabled = false

            viewModel.clickedWord.observe(viewLifecycleOwner) {
                val link = it.links.firstOrNull()
                if (link != null) infoWebview.loadUrl(link.link.replace("{q}", it.word))
                val adapter = ExternalLinksAdapter(it.word, it.links) { url ->
                    infoWebview.loadUrl(url)
                }
                adapter.setWrapped(true)
                linksList.adapter = adapter
                menuBar.isVisible = false
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
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