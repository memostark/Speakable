package com.guillermonegrete.tts.webreader

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Html
import android.view.*
import android.webkit.WebViewClient
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.databinding.FragmentWebReaderBinding
import com.guillermonegrete.tts.db.Words
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

    override fun onPause() {
        super.onPause()
        viewModel.saveWebLink()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        setupOptionsMenu()
        _binding = FragmentWebReaderBinding.bind(view)

        with(binding){
            loadingIcon.isVisible = true

            viewModel.page.observe(viewLifecycleOwner) {
                var isError = false
                var isLoading = false
                when(it){
                    is LoadResult.Error -> isError = true
                    LoadResult.Loading -> isLoading = true
                    is LoadResult.Success -> setBodyText(binding, it.data)
                }

                retryButton.isVisible = isError
                errorText.isVisible = isError

                loadingIcon.isVisible = isLoading
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

            retryButton.setOnClickListener {
                viewModel.loadDoc(args.link)
            }

            setBottomPanel()
            setTranslateBottomPanel()
        }

        viewModel.loadDoc(args.link)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setBodyText(binding: FragmentWebReaderBinding, text: String) {
        with(binding){
            bodyText.text = HtmlCompat.fromHtml(text, Html.FROM_HTML_MODE_COMPACT)

            bodyText.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val offset = bodyText.getOffsetForPosition(event.x, event.y)
                    val possibleWord = findWordForRightHanded(bodyText.text.toString(), offset)
                    clickedWord = possibleWord.ifBlank { null }
                }
                return@setOnTouchListener false
            }

            bodyText.setOnClickListener {
                clickedWord?.let { word -> viewModel.translateText(word.trim()) }
                clickedWord = null
            }

            listToggle.isVisible = true
            listToggle.setOnClickListener {
                onListToggleClick()
            }
        }
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

    private fun setupOptionsMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object: MenuProvider{
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
            }

            override fun onMenuItemSelected(menuItem: MenuItem) = false
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showTextDialog(words: Words, isSaved: Boolean){
        // TODO try to add this as destination in jetpack navigation
        val dialog = TextInfoDialog.newInstance(
            words.word,
            TextInfoDialog.NO_SERVICE,
            words,
            languageFrom = languageFrom,
            wordIsSaved = isSaved
        )
        dialog.show(childFragmentManager, "Text_info")
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setBottomPanel(){
        with(binding) {
            val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
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
            val decor = DividerItemDecoration(context, (linksList.layoutManager as LinearLayoutManager).orientation)
            linksList.addItemDecoration(decor)

            var selectedPos = 0
            viewModel.clickedWord.observe(viewLifecycleOwner) {
                val links = it.links
                links.forEach { link -> link.link = link.link.replace("{q}", it.word) }

                // if out of index, default to the first item (zero index)
                if(selectedPos >= links.size) selectedPos = 0

                val link = links.getOrNull(selectedPos)
                if (link != null) infoWebview.loadUrl(link.link)
                val adapter = ExternalLinksAdapter(it.links) { index ->
                    selectedPos = index
                    infoWebview.loadUrl(links[index].link)
                }
                adapter.setFlatButton(true)
                adapter.setSelectedPos(selectedPos)
                linksList.scrollToPosition(selectedPos)
                linksList.adapter = adapter
                menuBar.isVisible = false
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    private fun setTranslateBottomPanel() {
        with(binding) {
            val bottomSheetBehavior = BottomSheetBehavior.from(translationBottomSheet)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

            bottomSheetBehavior.addBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) menuBar.isVisible = true
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })

            viewModel.textInfo.observe(viewLifecycleOwner) { result ->
                barLoading.isInvisible = when(result){
                    is LoadResult.Success -> {
                        val word = result.data.word
                        translatedText.text = word.definition
                        notesText.isGone = word.notes.isNullOrEmpty()
                        notesText.text = word.notes
                        moreInfoBtn.isVisible = true
                        moreInfoBtn.setOnClickListener { showTextDialog(word, result.data.isSaved) }

                        menuBar.isVisible = false
                        true
                    }
                    is LoadResult.Error -> {
                        Toast.makeText(context, "Couldn't translate text", Toast.LENGTH_SHORT).show()
                        true
                    }
                    LoadResult.Loading -> {
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        moreInfoBtn.isVisible = false
                        false
                    }
                }
            }

            // If translate sheet is showing, hide it otherwise use normal back press
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                else if(isEnabled){
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
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