package com.guillermonegrete.tts.webreader

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.*
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.text.HtmlCompat
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.common.models.Span
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.databinding.FragmentWebReaderBinding
import com.guillermonegrete.tts.textprocessing.ExternalLinksAdapter
import com.guillermonegrete.tts.ui.theme.AppTheme
import com.guillermonegrete.tts.webreader.model.ModifiedNote
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

@AndroidEntryPoint
class WebReaderFragment : Fragment(R.layout.fragment_web_reader){

    private val viewModel: WebReaderViewModel by viewModels()

    private  var _binding: FragmentWebReaderBinding? = null
    private val binding get() = _binding!!

    private val args: WebReaderFragmentArgs by navArgs()

    private var languageFrom: String? = null

    private var adapter: ParagraphAdapter? = null

    private val loadingDialogVisible = mutableStateOf(false)
    private val deleteDialogVisible = mutableStateOf(false)
    private val addNoteDialogVisible = mutableStateOf(false)
    private val isPageSaved = mutableStateOf(false)

    private var pageText = ""

    private var paragraphTextSelection: ParagraphAdapter.NoteItem? = null

    override fun onPause() {
        super.onPause()
        viewModel.saveWebLink()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        setupOptionsMenu()
        _binding = FragmentWebReaderBinding.bind(view)

        val iconsVisible = mutableStateOf(false)

        with(binding){
            loadingIcon.isVisible = true

            viewModel.page.observe(viewLifecycleOwner) {
                var isError = false
                var isLoading = false
                when(it){
                    is LoadResult.Error -> {
                        Timber.e(it.exception,"Error loading page")
                        isError = true
                    }
                    LoadResult.Loading -> isLoading = true
                    is LoadResult.Success -> setParagraphList(it.data, iconsVisible)
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

            viewModel.updatedNote.observe(viewLifecycleOwner) { result ->
                when(result){
                    is ModifiedNote.Update -> {
                        val note = result.note
                        val dialogResult = AddNoteResult(note.text, note.color)
                        adapter?.updateNote(Span(note.position, note.position + note.length), note.id, dialogResult)
                    }
                    is ModifiedNote.Delete -> adapter?.deleteNote(result.noteId)
                }

            }

            val spinnerItems = mutableStateOf(emptyList<String>())
            val langSelection = mutableStateOf(-1)

            val langShortNames = resources.getStringArray(R.array.googleTranslateLangsWithAutoValue)
            viewModel.webLink.observe(viewLifecycleOwner) {
                spinnerItems.value = resources.getStringArray(R.array.googleTranslateLangsWithAutoArray).toList()

                languageFrom = it.language ?: langShortNames.first() // First is always "auto"
                langSelection.value = langShortNames.indexOf(languageFrom)
                isPageSaved.value = it.uuid != null
            }

            composeBar.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AppTheme {
                        WebReaderBottomBar(
                            spinnerItems,
                            langSelection,
                            iconsVisible,
                            isPageSaved,
                            { onTranslateClicked() },
                            { onArrowClicked(it) },
                            { onBarMenuItemClicked(it) },
                            { onPageVersionChanged(it) },
                        ) { index, _ ->
                            val langShort = if (index == 0) null else langShortNames[index]
                            languageFrom = langShort
                            viewModel.setLanguage(langShort)
                        }

                        val loadingVisible by remember { loadingDialogVisible }
                        LoadingDialog(loadingVisible)

                        var deleteVisible by remember { deleteDialogVisible }
                        DeletePageDialog(
                            deleteVisible,
                            onDismiss = { deleteVisible = false },
                            okClicked = {
                                deleteVisible = false
                                isPageSaved.value = false
                                val externalDir = context?.getExternalFilesDir(null)?.absolutePath.toString()
                                viewModel.deleteLinkFolder(externalDir)
                                adapter?.isPageSaved = false
                            }
                        )

                        var addNoteVisible by remember { addNoteDialogVisible }

                        AddNoteDialog(
                            addNoteVisible,
                            paragraphTextSelection?.text ?: "",
                            paragraphTextSelection?.color ?: 0,
                            onDismiss = {
                                addNoteVisible = false
                                adapter?.textSelectionRemoved()
                            },
                            onDelete = {
                                val noteItem = paragraphTextSelection ?: return@AddNoteDialog
                                viewModel.deleteNote(noteItem.id)
                                addNoteVisible = false
                            },
                            onSaveClicked = {
                                val noteItem = paragraphTextSelection ?: return@AddNoteDialog
                                viewModel.saveNote(it.text, noteItem.span, noteItem.id, it.colorHex)
                                addNoteVisible = false
                            },
                        )

                    }
                }
            }

            retryButton.setOnClickListener {
                viewModel.loadDoc(args.link)
            }

            setBottomPanel()
            setTranslateBottomPanel()

            setBackButtonNav()
        }

        viewModel.folderPath = context?.getExternalFilesDir(null)?.absolutePath.toString()
        viewModel.loadDoc(args.link)
    }

    private fun onPageVersionChanged(pageVersion: String) {
        when(pageVersion) {
            "Local" -> viewModel.loadLocalPage()
            "Web" -> viewModel.loadPageFromWeb()
        }
    }

    private fun onBarMenuItemClicked(index: Int) {
        if (isPageSaved.value) {
            deleteDialogVisible.value = true
        } else {
            loadingDialogVisible.value = true
            val externalDir = context?.getExternalFilesDir(null)?.absolutePath.toString()
            viewModel.saveWebLinkFolder(externalDir, UUID.randomUUID(), pageText)

            isPageSaved.value = true
            loadingDialogVisible.value = false

            adapter?.isPageSaved = true
        }
    }

    private fun setParagraphList(page: PageInfo, iconsVisible: MutableState<Boolean>) {
        pageText = page.text

        with(binding){
            paragraphsList.isVisible = true

            // Split text and parse from html
            val newParagraphs =  page.text.split("\n")
                .map { HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_COMPACT).trim() }
                .filter { it.isNotEmpty() }
            // Create items for adapter
            val splitParagraphs = viewModel.createParagraphs(newParagraphs)
            var index = 0
            val paragraphItems = mutableListOf<ParagraphAdapter.ParagraphItem>()
            val dbNotes = page.notes.toMutableList()

            splitParagraphs.forEach {
                val nextIndex = index + it.paragraph.length
                // Search the notes applied to this paragraph
                val paragraphNotes = dbNotes.filter { dbNote ->
                    dbNote.position in index until nextIndex
                }

                val noteItems = paragraphNotes.map { note ->
                    val itemStart = note.position - index
                    ParagraphAdapter.NoteItem(note.text, Span(itemStart, itemStart + note.length), Color.parseColor(note.color), note.id)
                }

                paragraphItems.add(ParagraphAdapter.ParagraphItem(it.paragraph, it.indexes, it.sentences, noteItems.toMutableList(), index))
                index = nextIndex
                dbNotes.removeAll(paragraphNotes)
            }

            val newAdapter = ParagraphAdapter(paragraphItems, page.isLocalPage, viewModel) {
                hideBottomSheets()
            }
            adapter = newAdapter
            paragraphsList.adapter = adapter

            iconsVisible.value = true
            setAdapterListeners()
        }
    }

    private fun setAdapterListeners() {
        val paragraphAdapter = adapter ?: return
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    paragraphAdapter.sentenceClicked.collect {

                        val bottomSheetBehavior = BottomSheetBehavior.from(binding.transSheet.root)
                        if(bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN){
                            paragraphAdapter.unselectSentence()
                            setWordSheetViews(false)
                        } else {
                            viewModel.translateWordInSentence(it)
                            paragraphAdapter.updateWordInSentence()
                        }
                    }
                }

                launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        paragraphAdapter.addNoteClicked.collect {
                            paragraphTextSelection = it
                            addNoteDialogVisible.value = true
                        }
                    }
                }
            }
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
            val translateSheetBehavior = BottomSheetBehavior.from(transSheet.root)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            bottomSheetBehavior.addBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN
                        && translateSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) composeBar.isVisible = true
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
                composeBar.isVisible = false
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }

            transSheet.addNoteBtn.setOnClickListener {
                val paragraphAdapter = adapter ?: return@setOnClickListener
                val span = paragraphAdapter.getSelectedWordSpan() ?: return@setOnClickListener
                val textView = binding.transSheet.translatedText
                paragraphTextSelection = ParagraphAdapter.NoteItem(textView.text.toString(), span, 0, 0)
                addNoteDialogVisible.value = true
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setTranslateBottomPanel() {
        with(binding.transSheet) {
            translatedText.movementMethod = ScrollingMovementMethod()
            // This disables scrolling the bottom sheet when scrolling the translation text
            // This is done to allow the TextView to scroll up
            translatedText.setOnTouchListener { view, _ ->
                view.parent.requestDisallowInterceptTouchEvent(true)
                false
            }
            val bottomSheetBehavior = BottomSheetBehavior.from(root)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

            bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        adapter?.unselectWord()
                        binding.composeBar.isVisible = true
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

                        // Only show the add note button if selection is a word and doesn't overlap any other note
                        val paragraphAdapter = adapter
                        if (paragraphAdapter != null)
                            addNoteBtn.isGone = !paragraphAdapter.isPageSaved || wordResult.isSentence || paragraphAdapter.isOverlappingNotes

                        moreInfoBtn.isGone = wordResult.isSentence
                        moreInfoBtn.setOnClickListener {
                            viewModel.getLinksForWord(word.word, word.lang)
                        }

                        if(bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        binding.composeBar.isVisible = false
                        true
                    }
                    is LoadResult.Error -> {
                        Toast.makeText(context, "Couldn't translate text", Toast.LENGTH_SHORT).show()
                        Timber.e(result.exception, "Error translating selected text")
                        true
                    }
                    LoadResult.Loading -> {
                        translatedText.text = ""
                        notesText.text = ""
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        moreInfoBtn.isVisible = false
                        addNoteBtn.isVisible = false
                        // When showing the sheet, the word views are hidden by default
                        setWordSheetViews(false)
                        false
                    }
                }
            }

            viewModel.wordInfo.observe(viewLifecycleOwner) { result ->
                barLoading.isInvisible = when(result){
                    is LoadResult.Success -> {
                        val word = result.data.word
                        wordTranslation.text = word.definition
                        moreInfoWordBtn.setOnClickListener {
                            viewModel.getLinksForWord(word.word, word.lang)
                        }
                        setWordSheetViews(true)
                        true
                    }
                    is LoadResult.Error -> {
                        Toast.makeText(context, "Couldn't translate word", Toast.LENGTH_SHORT).show()
                        Timber.e(result.exception,"Error translating word in selected text")
                        true
                    }
                    LoadResult.Loading -> {
                        wordTranslation.text = ""
                        moreInfoWordBtn.isInvisible = true
                        false
                    }
                }
            }
        }
    }

    private fun onArrowClicked(isLeft: Boolean){
        val paragraphAdapter = adapter ?: return
        if (isLeft) {
            paragraphAdapter.previousSentence()
        } else {
            paragraphAdapter.nextSentence()
        }
        val pos = paragraphAdapter.selectedSentence.paragraphIndex
        if(pos != -1) binding.paragraphsList.smoothScrollToPosition(pos)

    }

    private fun onTranslateClicked(){
        val adapter = adapter ?: return
        val expandedItemPos = adapter.expandedItemPos
        if (expandedItemPos != -1) {
            viewModel.translateParagraph(expandedItemPos)
            return
        }

        val selected = adapter.selectedSentence
        if(selected.paragraphIndex != -1 && selected.sentenceIndex != -1)
            viewModel.translateSelected(selected.paragraphIndex, selected.sentenceIndex)

        val text = adapter.getHighlightedText()
        if (text != null) {
            viewModel.translateText(text.toString())
        }
    }

    private fun setBackButtonNav() {
        val webSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        val bottomSheetBehavior = BottomSheetBehavior.from(binding.transSheet.root)
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
        val bottomSheetBehavior = BottomSheetBehavior.from(binding.transSheet.root)
        webSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun setWordSheetViews(isVisible: Boolean){
        val sheet = binding.transSheet
        sheet.topBorderWordView.isVisible = isVisible
        sheet.wordTranslation.isVisible = isVisible
        sheet.moreInfoWordBtn.isVisible = isVisible
    }
}
