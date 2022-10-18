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
import com.guillermonegrete.tts.textprocessing.ExternalLinksAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WebReaderFragment : Fragment(R.layout.fragment_web_reader){

    private val viewModel: WebReaderViewModel by viewModels()

    private  var _binding: FragmentWebReaderBinding? = null
    private val binding get() = _binding!!

    private val args: WebReaderFragmentArgs by navArgs()

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
                    is LoadResult.Success -> setParagraphList(it.data)
                }

                retryButton.isVisible = isError
                errorText.isVisible = isError

                loadingIcon.isVisible = isLoading
            }

            viewModel.translatedParagraph.observe(viewLifecycleOwner) { result ->
                val adapter = adapter ?: return@observe
                adapter.isLoading = when (result) {
                    LoadResult.Loading -> true
                    is LoadResult.Error -> false
                    is LoadResult.Success -> {
                        val translation = viewModel.translatedParagraphs[result.data]?.translatedText
                        if(translation != null) adapter.updateTranslation(translation)
                        false
                    }
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

            translate.setOnClickListener {
                viewModel.translateSelected(adapter?.selectedSentenceText)
            }

            retryButton.setOnClickListener {
                viewModel.loadDoc(args.link)
            }

            setBottomPanel()
            setTranslateBottomPanel()

            setBackButtonNav()
        }

        viewModel.loadDoc(args.link)
    }

    private fun setParagraphList(text: String) {
        with(binding){
            paragraphsList.isVisible = true
            if(adapter == null) {
                val newParagraphs =  text.split("\n")
                    .map { HtmlCompat.fromHtml(it, Html.FROM_HTML_MODE_COMPACT).trim() }
                    .filter { it.isNotEmpty() }
                viewModel.createParagraphs(newParagraphs)
                val splitParagraphs = viewModel.splitBySentence(newParagraphs)
                adapter = ParagraphAdapter(splitParagraphs.map { ParagraphAdapter.ParagraphItem(it.paragraph, it.indexes, it.sentences ) }, viewModel) {
                    hideBottomSheets()
                }
            }
            paragraphsList.adapter = adapter

            translate.isVisible = true
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

    @SuppressLint("ClickableViewAccessibility")
    private fun setBottomPanel(){
        with(binding) {
            val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
            val translateSheetBehavior = BottomSheetBehavior.from(translationBottomSheet)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            bottomSheetBehavior.addBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN
                        && translateSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) menuBar.isVisible = true
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

            bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        adapter?.unselectWord()
                        menuBar.isVisible = true
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })

            viewModel.textInfo.observe(viewLifecycleOwner) { result ->
                barLoading.isInvisible = when(result){
                    is LoadResult.Success -> {
                        val wordResult = result.data
                        val word = wordResult.word
                        translatedText.text = word.definition
                        notesText.isGone = word.notes.isNullOrEmpty()
                        notesText.text = word.notes
                        moreInfoBtn.isGone = wordResult.isSentence
                        moreInfoBtn.setOnClickListener {
                            viewModel.onWordClicked(word.word, adapter?.selectedSentence?.paragraphIndex ?: -1)
                        }

                        menuBar.isVisible = false
                        true
                    }
                    is LoadResult.Error -> {
                        Toast.makeText(context, "Couldn't translate text", Toast.LENGTH_SHORT).show()
                        true
                    }
                    LoadResult.Loading -> {
                        translatedText.text = ""
                        notesText.text = ""
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        moreInfoBtn.isVisible = false
                        false
                    }
                }
            }
        }
    }

    private fun setBackButtonNav() {
        val webSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        val bottomSheetBehavior = BottomSheetBehavior.from(binding.translationBottomSheet)
        // If translate sheet is showing, hide it otherwise use normal back press
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            when {
                webSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED -> webSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED -> bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                isEnabled -> {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        }
    }

    private fun hideBottomSheets() {
        val webSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        val bottomSheetBehavior = BottomSheetBehavior.from(binding.translationBottomSheet)
        webSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }
}
