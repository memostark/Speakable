package com.guillermonegrete.tts.webreader

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.*
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.guillermonegrete.tts.common.compose.DialogList
import com.guillermonegrete.tts.common.compose.LanguagesList
import com.guillermonegrete.tts.common.compose.StringList
import com.guillermonegrete.tts.common.models.EditNote
import com.guillermonegrete.tts.common.models.NoteItem
import com.guillermonegrete.tts.common.models.Span
import com.guillermonegrete.tts.common.models.WordUI
import com.guillermonegrete.tts.common.models.toUI
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.databinding.FragmentWebReaderBinding
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.savedwords.ResultType
import com.guillermonegrete.tts.textprocessing.EditDeleteWordDialogs
import com.guillermonegrete.tts.textprocessing.ExternalLinksAdapter
import com.guillermonegrete.tts.textprocessing.WordState
import com.guillermonegrete.tts.textprocessing.toWord
import com.guillermonegrete.tts.ui.theme.AppTheme
import com.guillermonegrete.tts.utils.createBackPressedCallback
import com.guillermonegrete.tts.utils.dpToPixel
import com.guillermonegrete.tts.utils.isWord
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

    private lateinit var adapter: ParagraphAdapter

    private val loadingDialogVisible = mutableStateOf(false)
    private val deleteDialogVisible = mutableStateOf(false)
    private val addNoteDialogVisible = mutableStateOf(false)
    private val editWordDialogVisible = mutableStateOf(false)
    private val deleteWordDialogShown = mutableStateOf(false)
    private val pickNewTypeDialogVisible = mutableStateOf(false)
    private val pickInfoDialogVisible = mutableStateOf(false)
    private val wordState = mutableStateOf<WordState?>(null)
    private val isPageSaved = mutableStateOf(false)

    private val languagesFull: List<String> by lazy { resources.getStringArray(R.array.googleTranslateLanguagesArray).toList() }
    private val languagesISO: List<String> by lazy  { resources.getStringArray(R.array.googleTranslateLanguagesValue).toList() }

    private var sbScope: CoroutineScope? = null
    private val snackbarHostState = mutableStateOf(SnackbarHostState())

    private var pageText = ""

    /**
     * Contains information used to update/create/delete a note.
     */
    private var noteInfo: EditNote? = null
    private var sheetInfo: Sheet? = null

    private var appBarSize = 0

    override fun onPause() {
        super.onPause()
        viewModel.saveWebLink()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        appBarSize = resources.getDimensionPixelSize(R.dimen.web_reader_bar_height)
        setupOptionsMenu()
        _binding = FragmentWebReaderBinding.bind(view)
        adapter = ParagraphAdapter(viewModel,
            onSentenceSelected = { hideBottomSheets() },
            onTextHighlighted = {
                val highlightSpan = adapter.getHighlightedTextSpan()
                val sheetSpan = noteInfo?.span
                if(highlightSpan != sheetSpan) {
                    hideBottomSheets()
                }
            },
            onTranslateHighlightedText = {
                viewModel.translateText(it)
                adapter.selectHighlightedText()
            },
            loadDatabaseWord = { text, pos ->
                viewModel.loadLocalWords(listOf(text.toString()), pos)
            },
            scanParagraph = viewModel::findWordsInParagraph,
        )

        val iconsVisible = mutableStateOf(false)

        with(binding) {
            paragraphsList.adapter = adapter
            linksList.adapter = ExternalLinksAdapter()
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
                        adapter.updateNote(span, note.id, dialogResult)

                        if(isSheetVisible()) {
                            if (adapter.selectedSentence.wordSelected ||
                                adapter.isInsideSelectedSentence(span)) { // if not word selected but still inside the sentence then it must be a note
                                sheet.wordTranslation.text = note.text
                                sheet.addWordNoteBtn.setImageResource(R.drawable.ic_edit_black_24dp)
                            } else {
                                sheet.translatedText.text = note.text
                                sheet.addNoteBtn.setImageResource(R.drawable.ic_edit_black_24dp)
                            }
                            val newNote = EditNote(note.originalText, note.text, span, Color.parseColor(note.color), true, note.id)
                            noteInfo = newNote
                            sheetInfo = Sheet.Note(newNote)
                        } else {
                            // If sheet not visible note was added using the selected text menu, no UI to update
                            noteInfo = null
                        }
                    }
                    is ModifiedNote.Delete -> {
                        if (binding.transSheet.wordTranslation.isVisible) {
                            // Note info is visible, because the note was deleted hide the word/note views
                            setWordSheetViews(false)
                        } else {
                            // Otherwise just hide the regular sheet
                            val behavior = BottomSheetBehavior.from(binding.transSheet.root)
                            behavior.state = BottomSheetBehavior.STATE_HIDDEN
                        }

                        adapter.unselectWord()
                        adapter.deleteNote(result.noteId)
                        sheet.addNoteBtn.setImageResource(R.drawable.baseline_note_add_24)
                    }
                }

            }

            viewModel.updatedWord.observe(viewLifecycleOwner) { result ->
                when (result) {
                    is ResultType.Update -> {
                        val word = result.word
                        val newState = WordState(word.toUI(), word.id, wordState.value?.span)
                        showSavedWord(newState)
                    }
                    is ResultType.Insert -> {
                        val word = result.word
                        if (viewModel.showWords) highlightSavedWord(word, getVisibleListItems())
                        viewModel.setSavedWord(word.word, word.lang)
                    }
                    is ResultType.Delete -> {
                        val span = wordState.value?.span
                        if (span != null && adapter.isInsideSelectedSentence(span)) {
                            setWordSheetViews(false)
                        } else {
                            hideTranslationSheet()
                        }
                        adapter.removeWord(viewModel.getWordIndexes(result.id), result.id)
                        wordState.value = null
                        deleteWordDialogShown.value = false
                    }
                }
                editWordDialogVisible.value = false
            }

            val langSelection = mutableIntStateOf(-1)
            val langShortNames = resources.getStringArray(R.array.googleTranslateLangsWithAutoValue)

            viewModel.webLink.observe(viewLifecycleOwner) {
                languageFrom = it.language ?: langShortNames.first() // First is always "auto"
                langSelection.intValue = langShortNames.indexOf(languageFrom)
                isPageSaved.value = it.uuid != null
            }

            lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.pageSavedWords.collect { result ->
                        adapter.updateSavedWords(result.words, result.start)
                        adapter.initialWordsLoaded = true
                    }
                }
            }

            val spinnerItems = StringList(resources.getStringArray(R.array.googleTranslateLangsWithAutoArray).toList())

            composeBar.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AppTheme {
                        WebReaderBottomBar(
                            spinnerItems,
                            langSelection,
                            iconsVisible,
                            isPageSaved,
                            viewModel.showWords,
                            ::onTranslateClicked,
                            ::onArrowClicked,
                            ::onBarMenuItemClicked,
                        ) { index, _ ->
                            val langShort = if (index == 0) null else langShortNames[index]
                            languageFrom = langShort
                            viewModel.setLanguage(langShort)
                        }

                        WebReaderDialogs()
                    }
                }
            }

            composeRoot.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AppTheme {
                        sbScope = rememberCoroutineScope()
                        SnackbarHost(hostState = snackbarHostState.value)

                        Dialogs()
                    }
                }
            }

            retryButton.setOnClickListener {
                viewModel.loadDoc(args.link)
            }

            setTranslateBottomPanel()
            setBottomPanel()
            setInsetListener()
        }

        viewModel.folderPath = context?.getExternalFilesDir(null)?.absolutePath.toString()
        viewModel.loadDoc(args.link)
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    private fun setInsetListener() {
        val initialBarSize = appBarSize
        ViewCompat.setOnApplyWindowInsetsListener(binding.paragraphsList) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val barShown = false
            if (!barShown) v.updatePadding(top = insets.top)
            val extra = if (barShown) initialBarSize else 0
            binding.composeBar.updatePadding(bottom = insets.bottom + extra)
            appBarSize = initialBarSize + insets.bottom
            updateListBottomPadding(0)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun onBarMenuItemClicked(action: WebReaderMenuAction) {
        when(action) {
            WebReaderMenuAction.PageStatus -> {
                if (isPageSaved.value) {
                    deleteDialogVisible.value = true
                } else {
                    loadingDialogVisible.value = true
                    val externalDir = context?.getExternalFilesDir(null)?.absolutePath.toString()
                    viewModel.saveWebLinkFolder(externalDir, UUID.randomUUID(), pageText)

                    isPageSaved.value = true
                    loadingDialogVisible.value = false

                    adapter.isPageSaved = true
                }
            }
            is WebReaderMenuAction.PageVersion -> {
                when(action.version) {
                    "Local" -> viewModel.loadLocalPage()
                    "Web" -> viewModel.loadPageFromWeb()
                }
            }
            is WebReaderMenuAction.ShowWords -> {
                viewModel.showWords = action.shown
                if (action.shown) loadWordsForVisibleItems() else hideSavedWords()
            }
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
                    NoteItem(note.text, Span(itemStart, itemStart + note.length), Color.parseColor(note.color), note.id)
                }

                paragraphItems.add(ParagraphAdapter.ParagraphItem(it.paragraph, it.indexes, it.sentences, noteItems.toMutableList(), index))
                index = nextIndex
                dbNotes.removeAll(paragraphNotes)
            }

            adapter.isPageSaved = page.isLocalPage
            adapter.updateItems(paragraphItems)
            paragraphsList.adapter = adapter
            paragraphsList.post {
                loadWordsForVisibleItems()
            }

            iconsVisible.value = true
            setAdapterListeners()
        }
    }

    private fun setAdapterListeners() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    adapter.textClicked.collect { result ->
                        when(result) {
                            is ParagraphAdapter.TextClick.SavedWord -> {
                                wordState.value = result.word
                                val wordData = result.word.word
                                sheetInfo = Sheet.Word(result.word)
                                viewModel.setSavedWord(wordData.word, wordData.lang)
                            }
                            is ParagraphAdapter.TextClick.Sentence -> {
                                val bottomSheetBehavior = BottomSheetBehavior.from(binding.transSheet.root)
                                if(bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN){
                                    adapter.unselectSentence()
                                } else {
                                    viewModel.translateWordInSentence(result.word)
                                    adapter.updateWordInSentence()
                                }
                            }
                            is ParagraphAdapter.TextClick.Note -> showSheetWithNote(result.item)
                            is ParagraphAdapter.TextClick.Overlap -> {
                                noteInfo = result.note
                                wordState.value = result.word
                                pickInfoDialogVisible.value = true
                            }
                        }
                    }
                }

                launch {
                    adapter.addNoteClicked.collect { note ->
                        noteInfo = note
                        addNoteDialogVisible.value = true
                    }
                }
                launch {
                    adapter.addWordClicked.collect { state ->
                        val lang = viewModel.getLanguage()
                        val newState = if (lang == null) state else state.copy(word = state.word.copy(lang = lang))
                        wordState.value = newState
                        editWordDialogVisible.value = true
                    }
                }
            }
        }
    }

    private fun showSavedWord(state: WordState) {
        wordState.value = state
        sheetInfo = Sheet.Word(state)
        val word = state.word
        val wordSpan = state.span ?: Span(0, 0)
        updateSheet(word, wordSpan, true)
    }

    private fun updateSheet(info: WordUI, span: Span, isWord: Boolean) {
        with(binding.transSheet) {
            if(isSheetVisible() && adapter.isInsideSelectedSentence(span)){
                val hasNote = !info.notes.isNullOrBlank()
                val text = if (hasNote) "${info.definition} (${info.notes})" else info.definition
                wordTranslation.text = text
                setWordSheetViews(true)
                moreInfoWordBtn.isVisible = isWord

                moreInfoWordBtn.setOnClickListener {
                    if (info.lang.isNotEmpty()) viewModel.getLinksForWord(info.word, info.lang)
                    else getLinksForWord(info.word)
                }

                addWordNoteBtn.setImageResource(R.drawable.ic_edit_black_24dp)
            } else {
                setWordSheetViews(false)
                translatedText.text = info.definition
                notesText.isGone = info.notes.isNullOrEmpty()
                notesText.text = info.notes

                addNoteBtn.isVisible = true
                addNoteBtn.setImageResource(R.drawable.ic_edit_black_24dp)

                moreInfoBtn.isVisible = isWord
                moreInfoBtn.setOnClickListener {
                    if (info.lang.isNotEmpty()) viewModel.getLinksForWord(info.word, info.lang)
                    else getLinksForWord(info.word)
                }

                val bottomSheetBehavior = BottomSheetBehavior.from(root)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                adapter.unselectSentence()
            }
        }
        adapter.unselectWord()
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

    @SuppressLint("ClickableViewAccessibility", "SetJavaScriptEnabled")
    private fun setBottomPanel(){
        with(binding) {
            val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
            val translateSheetBehavior = BottomSheetBehavior.from(transSheet.root)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

            val bottomSheetBackCallback = createBackPressedCallback(bottomSheetBehavior)
            requireActivity().onBackPressedDispatcher.addCallback(this@WebReaderFragment, bottomSheetBackCallback)

            bottomSheetBehavior.addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_HIDDEN -> {
                            bottomSheetBackCallback.isEnabled = false
                            if (translateSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) composeBar.isVisible = true
                        }
                        BottomSheetBehavior.STATE_EXPANDED -> bottomSheetBackCallback.isEnabled = true
                        else -> {}
                    }
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
            viewModel.linksForWord.observe(viewLifecycleOwner) {
                val links = it.links
                links.forEach { link -> link.link = link.link.replace("{q}", it.word) }

                // if out of index, default to the first item (zero index)
                if(selectedPos >= links.size) selectedPos = 0

                val link = links.getOrNull(selectedPos)
                if (link != null) infoWebview.loadUrl(link.link)
                val adapter = ExternalLinksAdapter(it.links) { index ->
                    selectedPos = index
                    infoWebview.loadUrl(links[index].link)
                    linksList.scrollToPosition(index)
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
            wordTranslation.movementMethod = ScrollingMovementMethod()
            wordTranslation.setHorizontallyScrolling(true)

            val bottomSheetBehavior = BottomSheetBehavior.from(root)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

            val backPressedCallback = createBackPressedCallback(bottomSheetBehavior)

            requireActivity().onBackPressedDispatcher.addCallback(this@WebReaderFragment, backPressedCallback)

            bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        backPressedCallback.isEnabled = false
                        adapter.unselectWord()
                        setWordSheetViews(false)
                        binding.composeBar.isVisible = true
                        updateListBottomPadding(0)
                    } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                        backPressedCallback.isEnabled = true
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

                        // Only show the add note button if selection is not a sentence and doesn't overlap any other note
                        val text = word.word
                        val isWord = text.isWord()
                        val noteUnavailable = adapter.isOverlappingNotes || !adapter.isPageSaved
                        addNoteBtn.isGone = wordResult.isSentence || (noteUnavailable && !isWord)
                        addNoteBtn.setImageResource(R.drawable.baseline_note_add_24)

                        val span = adapter.getSelectedWordSpan() ?: adapter.getHighlightedTextSpan()
                        if(span != null) {
                            val note = EditNote(text, word.definition, span, 0, false, 0)
                            sheetInfo = when {
                                isWord && !adapter.isOverlappingSavedWord -> {
                                    val state = WordState(word.toUI(), span = span)
                                    wordState.value = state
                                    if (noteUnavailable) Sheet.Word(state) else Sheet.Multiple
                                }
                                else -> Sheet.Note(note)
                            }
                            noteInfo = note
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

                        val span = adapter.getSelectedWordSpan()
                        if (span != null) {
                            val state = WordState(word.toUI(), span = span)
                            wordState.value = state
                            val noteUnavailable = !adapter.isPageSaved
                            sheetInfo = if (noteUnavailable) Sheet.Word(state) else Sheet.Multiple
                            noteInfo = EditNote(word.word, word.definition, span, 0, false, 0)
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
                when (val sheet = sheetInfo) {
                    Sheet.Multiple -> pickNewTypeDialogVisible.value = true
                    is Sheet.Note -> {
                        noteInfo = sheet.info
                        addNoteDialogVisible.value = true
                    }
                    is Sheet.Word -> {
                        wordState.value = sheet.state
                        editWordDialogVisible.value = true
                    }
                    null -> Timber.e("Sheet info is missing")
                }
            }

            addWordNoteBtn.setOnClickListener {
                when (val sheet = sheetInfo) {
                    Sheet.Multiple -> pickNewTypeDialogVisible.value = true
                    is Sheet.Note -> {
                        noteInfo = sheet.info
                        addNoteDialogVisible.value = true
                    }
                    is Sheet.Word -> {
                        wordState.value = sheet.state
                        editWordDialogVisible.value = true
                    }
                    null -> Timber.e("Sheet info is missing")
                }
            }
        }
    }

    private fun onArrowClicked(isLeft: Boolean){
        if (isLeft) {
            adapter.previousSentence()
        } else {
            adapter.nextSentence()
        }
        val pos = adapter.selectedSentence.paragraphIndex
        if(pos != -1) binding.paragraphsList.smoothScrollToPosition(pos)

    }

    private fun onTranslateClicked(){
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
            adapter.selectHighlightedText()
        }
    }

    private fun showSheetWithNote(note: EditNote) {
        noteInfo = note
        sheetInfo = Sheet.Note(note)
        val info = WordUI(note.text, "", note.noteText)
        updateSheet(info, note.span, note.text.isWord())
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

    private fun hideBottomSheets() {
        val webSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        webSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        hideTranslationSheet()
    }

    private fun hideTranslationSheet() {
        val bottomSheetBehavior = BottomSheetBehavior.from(binding.transSheet.root)
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

    private fun highlightSavedWord(dbWord: Words, range: IntRange) {
        adapter.setScanNewWords()
        val paragraphWords = findWordsInItems(listOf(dbWord), range)
        adapter.newWords.add(dbWord)
        adapter.addSavedWords(paragraphWords, range.first)
    }

    private fun findWordsInItems(dbWords: List<Words>, range: IntRange): List<List<WordState>> {
        val paragraphWords = arrayListOf<List<WordState>>()
        range.forEach { pos ->
            val words = viewModel.findWordsInParagraph(dbWords,  adapter.getText(pos), pos)
            paragraphWords.add(words)
        }
        return paragraphWords
    }

    private fun updateListBottomPadding(pixels: Int) {
        val extra = if(pixels == 0) appBarSize else requireContext().dpToPixel(16)
        binding.paragraphsList.updatePadding(bottom = pixels + extra)
    }

    private fun loadWordsForVisibleItems() {
        val range = getVisibleListItems()
        val text = adapter.getItemsText(range)
        viewModel.loadLocalWords(text, range.first)
    }

    private fun hideSavedWords() {
        viewModel.resetWordData()
        adapter.removeWords(getVisibleListItems())
    }

    private fun getVisibleListItems(): IntRange {
        val list = binding.paragraphsList
        val start = list.getChildLayoutPosition(list.getChildAt(0))
        val end = list.getChildLayoutPosition(list.getChildAt(list.childCount - 1))
        return start..end
    }

    private fun isSheetVisible(): Boolean {
        val behavior = BottomSheetBehavior.from(binding.transSheet.root)
        return behavior.state == BottomSheetBehavior.STATE_EXPANDED
    }

    @Composable
    fun WebReaderDialogs() {
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
                adapter.isPageSaved = false
            }
        )

        var addNoteVisible by remember { addNoteDialogVisible }

        AddNoteDialog(
            addNoteVisible,
            noteInfo?.noteText ?: "",
            noteInfo?.color ?: 0,
            noteInfo?.noteSaved == true,
            onDismiss = { addNoteVisible = false },
            onDelete = {
                val noteItem = noteInfo ?: return@AddNoteDialog
                viewModel.deleteNote(noteItem.id)
                noteInfo = null
                addNoteVisible = false
            },
            onSaveClicked = { newNote ->
                val noteItem = noteInfo ?: return@AddNoteDialog
                viewModel.saveNote(noteItem.text, newNote.text, noteItem.span, noteItem.id, newNote.colorHex)
                addNoteVisible = false
            },
        )
    }

    @Composable
    fun Dialogs() {
        val state = wordState.value ?: return
        val languages = LanguagesList(languagesFull, languagesISO)

        EditDeleteWordDialogs(
            state,
            editWordDialogVisible,
            deleteWordDialogShown,
            languages,
            onSave = {
                val resultWord = it.toWord()
                resultWord.id = state.dbId
                viewModel.upsert(resultWord)
            },
            onDelete = { viewModel.deleteWord(state.toWord()) },
        )

        if (pickNewTypeDialogVisible.value) {
            DialogList(
                list = listOf(resources.getString(R.string.note), resources.getString(R.string.saved_word)),
                title = resources.getString(R.string.create_db_item_dialog_title),
                onItemSelected = { index, _ ->
                    when (index) {
                        0 -> addNoteDialogVisible.value = true
                        1 -> editWordDialogVisible.value = true
                    }
                    pickNewTypeDialogVisible.value = false
                },
                onDismiss = { pickNewTypeDialogVisible.value = false }
            )
        }

        if (pickInfoDialogVisible.value) {
            DialogList(
                list = listOf(resources.getString(R.string.note), resources.getString(R.string.saved_word)),
                title = resources.getString(R.string.pick_info_dialog_title),
                onItemSelected = { index, _ ->
                    when (index) {
                        0 -> noteInfo?.let { showSheetWithNote(it) }
                        1 -> {
                            val wordUI = wordState.value?.word
                            if (wordUI != null) viewModel.setSavedWord(wordUI.word, wordUI.lang)
                        }
                    }
                    pickInfoDialogVisible.value = false
                },
                onDismiss = { pickInfoDialogVisible.value = false }
            )
        }
    }

    sealed interface Sheet {
        data class Note(val info: EditNote): Sheet
        data class Word(val state: WordState): Sheet
        data object Multiple: Sheet
    }
}
