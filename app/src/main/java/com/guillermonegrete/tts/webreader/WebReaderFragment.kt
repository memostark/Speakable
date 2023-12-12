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
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
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
import com.guillermonegrete.tts.utils.actionBarSize
import com.guillermonegrete.tts.utils.dpToPixel
import com.guillermonegrete.tts.webreader.model.ModifiedNote
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
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

    private var sbScope: CoroutineScope? = null
    private val snackbarHostState = mutableStateOf(SnackbarHostState())

    private var pageText = ""

    /**
     * Contains information used to update/create/delete a note.
     *
     * This information is the one displayed in the bottom sheet
     */
    private var noteInfo: ParagraphAdapter.EditNote? = null

    /**
     * The data for creating a note using the text highlight method.
     *
     * This data is independent from the one displayed in the bottom sheet
     */
    private var highlightNote: ParagraphAdapter.EditNote? = null

    private var appBarSize = 0

    override fun onPause() {
        super.onPause()
        viewModel.saveWebLink()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        appBarSize = requireContext().actionBarSize
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
                val sheet = binding.transSheet
                when(result){
                    is ModifiedNote.Update -> {
                        val note = result.note
                        val dialogResult = AddNoteResult(note.text, note.color)
                        val span = Span(note.position, note.position + note.length)
                        val paragraphAdapter = adapter ?: return@observe
                        paragraphAdapter.updateNote(span, note.id, dialogResult)

                        if(span != highlightNote?.span) {
                            if (paragraphAdapter.selectedSentence.wordSelected ||
                                paragraphAdapter.isInsideSelectedSentence(span)) { // if not word selected but still inside the sentence then it must be a note
                                sheet.wordTranslation.text = note.text
                                sheet.addWordNoteBtn.setImageResource(R.drawable.ic_edit_black_24dp)
                            } else {
                                sheet.translatedText.text = note.text
                                sheet.addNoteBtn.setImageResource(R.drawable.ic_edit_black_24dp)
                            }
                            noteInfo = ParagraphAdapter.EditNote(note.originalText, note.text, span, Color.parseColor(note.color), note.id)
                        } else {
                            highlightNote = null
                        }
                    }
                    is ModifiedNote.Delete -> {
                        val paragraphAdapter = adapter
                        if(paragraphAdapter != null) {
                            if (binding.transSheet.wordTranslation.isVisible) {
                                // Note info is visible, because the note was deleted hide the word/note views
                                setWordSheetViews(false)
                            } else {
                                // Otherwise just hide the regular sheet
                                val behavior = BottomSheetBehavior.from(binding.transSheet.root)
                                behavior.state = BottomSheetBehavior.STATE_HIDDEN
                            }
                            paragraphAdapter.unselectWord()
                        }

                        adapter?.deleteNote(result.noteId)
                        sheet.addNoteBtn.setImageResource(R.drawable.baseline_note_add_24)
                    }
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
                            highlightNote?.noteText ?: noteInfo?.noteText ?: "",
                            highlightNote?.color ?: noteInfo?.color ?: 0,
                            onDismiss = {
                                addNoteVisible = false
                                highlightNote = null
                            },
                            onDelete = {
                                if (highlightNote != null) highlightNote = null
                                val noteItem = noteInfo ?: return@AddNoteDialog
                                viewModel.deleteNote(noteItem.id)
                                addNoteVisible = false
                            },
                            onSaveClicked = { newNote ->
                                val noteItem = highlightNote ?: noteInfo ?: return@AddNoteDialog
                                viewModel.saveNote(noteItem.text, newNote.text, noteItem.span, noteItem.id, newNote.colorHex)
                                addNoteVisible = false
                            },
                        )

                    }
                }
            }

            composeRoot.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AppTheme {
                        sbScope = rememberCoroutineScope()
                        SnackbarHost(hostState = snackbarHostState.value)
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

            val newAdapter = ParagraphAdapter(paragraphItems, page.isLocalPage, viewModel,
                onSentenceSelected = { hideBottomSheets() },
                onTextHighlighted = {
                val highlightSpan = adapter?.getHighlightedTextSpan()
                val sheetSpan = noteInfo?.span
                if(highlightSpan != sheetSpan) {
                    hideBottomSheets()
                }
            })
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
                        } else {
                            viewModel.translateWordInSentence(it)
                            paragraphAdapter.updateWordInSentence()
                        }
                    }
                }

                launch {
                    paragraphAdapter.addNoteClicked.collect { note ->
                        if (note.id == 0L) {
                            // zero means new note, show dialog to add note
                            highlightNote = note
                            addNoteDialogVisible.value = true
                        } else {
                            noteInfo = note
                            showSheetWithNote(paragraphAdapter, note)
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

            bottomSheetBehavior.addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
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
                        setWordSheetViews(false)
                        binding.composeBar.isVisible = true
                        updateListBottomPadding(0)
                    } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                        binding.composeBar.isVisible = false
                        updateListBottomPadding(root.height)
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
                        if (paragraphAdapter != null) {
                            addNoteBtn.isGone = !paragraphAdapter.isPageSaved || wordResult.isSentence || paragraphAdapter.isOverlappingNotes
                            addNoteBtn.setImageResource(R.drawable.baseline_note_add_24)
                            val span = paragraphAdapter.getSelectedWordSpan() ?: paragraphAdapter.getHighlightedTextSpan()
                            if(span != null) {
                                noteInfo = ParagraphAdapter.EditNote(word.word, word.definition, span, 0, 0)
                            }
                        }

                        moreInfoBtn.isGone = wordResult.isSentence
                        moreInfoBtn.setOnClickListener {
                            viewModel.getLinksForWord(word.word, word.lang)
                        }

                        if(bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        } else {
                            // Sheet layout modified but the sheet was already expanded, manually update padding
                            translatedText.post {
                                updateListBottomPadding(root.height)
                            }
                        }
                        true
                    }
                    is LoadResult.Error -> {
                        Toast.makeText(context, "Couldn't translate text", Toast.LENGTH_SHORT).show()
                        Timber.e(result.exception, "Error translating selected text")
                        true
                    }
                    LoadResult.Loading -> {
                        translatedText.text = ""
                        translatedText.scrollTo(0, 0)
                        notesText.text = ""
                        moreInfoBtn.isVisible = false
                        addNoteBtn.isVisible = false
                        setWordSheetViews(false)
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
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

                        val paragraphAdapter = adapter
                        if (paragraphAdapter != null) {
                            addWordNoteBtn.isGone = !paragraphAdapter.isPageSaved
                            val span = paragraphAdapter.getSelectedWordSpan()
                            if(span != null) noteInfo = ParagraphAdapter.EditNote(word.word, word.definition, span, 0, 0)
                        }

                        addWordNoteBtn.setOnClickListener {
                            addNoteDialogVisible.value = true
                        }
                        addWordNoteBtn.setImageResource(R.drawable.baseline_note_add_24)
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
                        addWordNoteBtn.isInvisible = true
                        false
                    }
                }
            }

            addNoteBtn.setOnClickListener {
                addNoteDialogVisible.value = true
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
            viewModel.translateSentence(selected.paragraphIndex, selected.sentenceIndex)

        val text = adapter.getHighlightedText()
        if (text != null) {
            viewModel.translateText(text.toString())
        }
    }

    private fun showSheetWithNote(paragraphAdapter: ParagraphAdapter, note: ParagraphAdapter.EditNote) {
        with(binding.transSheet) {
            val text = note.text
            val isWord = text.split(" ").size == 1
            // Check if sheet is visible and if the note is within the sentence
            if(isSheetVisible() && paragraphAdapter.isInsideSelectedSentence(note.span)){
                // Update the word text and buttons
                wordTranslation.text = note.noteText
                setWordSheetViews(true)
                moreInfoWordBtn.isVisible = isWord
                moreInfoWordBtn.setOnClickListener {
                    getLinksForWord(note.text)
                }

                addWordNoteBtn.setImageResource(R.drawable.ic_edit_black_24dp)
                addWordNoteBtn.setOnClickListener {
                    addNoteDialogVisible.value = true
                }
            } else {
                // Otherwise show the regular sheet
                translatedText.text = note.noteText
                translatedText.scrollTo(0, 0)
                addNoteBtn.isVisible = true
                addNoteBtn.setImageResource(R.drawable.ic_edit_black_24dp)

                moreInfoBtn.isVisible = isWord
                moreInfoBtn.setOnClickListener {
                    getLinksForWord(text)
                }
                setWordSheetViews(false)

                val translateSheetBehavior = BottomSheetBehavior.from(root)
                translateSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                paragraphAdapter.unselectSentence()
            }
        }
        paragraphAdapter.unselectWord()
    }

    private fun getLinksForWord(text: String) {
        val language = viewModel.getLanguage()
        if (language == null) {
            sbScope?.launch {
                snackbarHostState.value.showSnackbar(getString(R.string.pick_language_web_reader))
            }
        } else {
            viewModel.getLinksForWord(text)
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
        with(binding.transSheet) {
            topBorderWordView.isVisible = isVisible
            wordTranslation.isVisible = isVisible
            addWordNoteBtn.isVisible = isVisible
            moreInfoWordBtn.isVisible = isVisible
            if(isVisible) {
                wordTranslation.post {
                    updateListBottomPadding(root.height)
                }
            }
            val padding = if (isVisible) 0 else resources.getDimensionPixelSize(R.dimen.default_dialog_padding)
            constraintLayout.updatePadding(bottom = padding)
        }
    }

    private fun updateListBottomPadding(pixels: Int) {
        val extra = if(pixels == 0) appBarSize else requireContext().dpToPixel(16)
        binding.paragraphsList.updatePadding(bottom = pixels + extra)
    }

    private fun isSheetVisible(): Boolean {
        val behavior = BottomSheetBehavior.from(binding.transSheet.root)
        return behavior.state == BottomSheetBehavior.STATE_EXPANDED
    }
}
