package com.guillermonegrete.tts.webreader

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.*
import android.webkit.WebViewClient
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
import com.guillermonegrete.tts.data.DialogState
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.databinding.FragmentWebReaderBinding
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.savedwords.ResultType
import com.guillermonegrete.tts.textprocessing.EditDeleteDialogUI
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
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import kotlin.text.isNotEmpty

@AndroidEntryPoint
class WebReaderFragment : Fragment(R.layout.fragment_web_reader){

    private val viewModel: WebReaderViewModel by viewModels(extrasProducer = {
        defaultViewModelCreationExtras.withCreationCallback<WebReaderViewModel.Factory> { factory ->
            factory.create(args.link)
        }
    })

    private  var _binding: FragmentWebReaderBinding? = null
    private val binding get() = _binding!!

    private val args: WebReaderFragmentArgs by navArgs()

    private var languageFrom: String? = null

    private lateinit var adapter: ParagraphAdapter

    private val loadingDialogVisible = mutableStateOf(false)
    private val deleteDialogVisible = mutableStateOf(false)
    private val addNoteDialogVisible = mutableStateOf<AddNoteDialogUI?>(null)
    private val editWordDialogVisible = mutableStateOf<EditDeleteDialogUI?>(null)
    private val pickNewTypeDialogVisible = mutableStateOf(false)
    private val pickInfoDialogVisible = mutableStateOf(false)
    private val isPageSaved = mutableStateOf(false)

    private val languagesFull: List<String> by lazy { resources.getStringArray(R.array.googleTranslateLanguagesArray).toList() }
    private val languagesISO: List<String> by lazy  { resources.getStringArray(R.array.googleTranslateLanguagesValue).toList() }

    private var sbScope: CoroutineScope? = null
    private val snackbarHostState = mutableStateOf(SnackbarHostState())

    private var pageText = ""

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
            onSentenceSelected = viewModel::sentenceSelected,
            onParagraphSelected = viewModel::paragraphSelected,
            onTextHighlighted =  {
                viewModel.unselectSentence()
                viewModel.clearTextInfo()
            },
            onTranslateHighlightedText = { text, span ->
                viewModel.translateText(text, span, adapter.isOverlappingNotes, adapter.isOverlappingSavedWord)
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

            val langSelection = mutableIntStateOf(-1)
            val langShortNames = resources.getStringArray(R.array.googleTranslateLangsWithAutoValue)

            viewModel.webLink.observe(viewLifecycleOwner) {
                languageFrom = it.language ?: langShortNames.first() // First is always "auto"
                langSelection.intValue = langShortNames.indexOf(languageFrom)
                isPageSaved.value = it.uuid != null
            }

            lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch {
                        viewModel.pageSavedWords.collect { result ->
                            adapter.updateSavedWords(result.words, result.start)
                            adapter.initialWordsLoaded = true
                        }
                    }

                    launch {
                        viewModel.updatedWord.collect { result ->
                            when (result) {
                                is ResultType.Update -> {}
                                is ResultType.Insert -> {
                                    val word = result.word
                                    if (viewModel.showWords) highlightSavedWord(word, getVisibleListItems())
                                }
                                is ResultType.Delete -> adapter.removeWord(viewModel.getWordIndexes(result.id), result.id)
                            }
                        }
                    }

                    launch {
                        viewModel.updatedNote.collect { result ->
                            when(result){
                                is ModifiedNote.Update -> {
                                    val note = result.note
                                    val dialogResult = AddNoteResult(note.text, note.color)
                                    val span = Span(note.position, note.position + note.length)
                                    adapter.updateNote(span, note.id, dialogResult)
                                }
                                is ModifiedNote.Delete -> {
                                    adapter.unselectWord()
                                    adapter.deleteNote(result.noteId)
                                }
                            }
                        }
                    }

                    launch {
                        viewModel.paragraphState.collect { result ->
                            if (result.paragraphIndex != null && result.sentenceIndex != null) {
                                adapter.selectSentence(result.paragraphIndex, result.sentenceIndex)
                            } else {
                                adapter.unselectSentence()
                            }

                            val paragraph = result.paragraph
                            if (paragraph != null) {
                                if (paragraph.isLoading) {
                                    adapter.isLoading = true
                                } else {
                                    adapter.isLoading = false
                                    adapter.selectParagraph(paragraph.index)
                                    if (paragraph.translation != null) adapter.updateTranslation(paragraph.translation.translatedText)
                                }
                                adapter.updateExpanded()
                            } else {
                                adapter.unselectParagraph()
                            }
                        }
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
                            ::getPageVersion,
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
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    private fun setInsetListener() {
        val initialBarSize = appBarSize
        ViewCompat.setOnApplyWindowInsetsListener(binding.paragraphsList) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.updatePadding(top = insets.top)
            binding.composeBar.updatePadding(bottom = insets.bottom)
            binding.composeRoot.updatePadding(top = insets.top)
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
                    "Local" -> viewModel.setPageVersion(PageVersion.LOCAL)
                    "Web" -> viewModel.setPageVersion(PageVersion.WEB)
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
                                val state = result.word
                                val sentenceSpan = adapter.getSelectedSentenceSpan()
                                if (state.span != null) viewModel.setSavedWord(state.dbId, state.span, sentenceSpan)
                            }
                            is ParagraphAdapter.TextClick.Sentence -> {
                                val bottomSheetBehavior = BottomSheetBehavior.from(binding.transSheet.root)
                                if(bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN){
                                    viewModel.unselectSentence()
                                } else {
                                    adapter.getSelectedWordSpan()?.let {
                                        viewModel.translateWordInSentence(result.word, it)
                                        adapter.updateWordInSentence()
                                    }
                                }
                            }
                            is ParagraphAdapter.TextClick.Note -> viewModel.setNoteData(result.item, adapter.getSelectedSentenceSpan())
                            is ParagraphAdapter.TextClick.Overlap -> {
                                if (result.word.span != null)
                                    viewModel.setPickInfoType(DialogType.SavedWord(result.word.toWord(), result.word.span), DialogType.Note(result.note))
                            }
                        }
                    }
                }

                launch {
                    adapter.addNoteClicked.collect { note ->
                        viewModel.startEditing(DialogType.Note(note))
                    }
                }

                launch {
                    adapter.addWordClicked.collect { state ->
                        val lang = viewModel.getLanguage()
                        val newState = if (lang == null) state else state.copy(word = state.word.copy(lang = lang))
                        if (newState.span != null) viewModel.startEditing(DialogType.SavedWord(newState.toWord(), newState.span))
                    }
                }
            }
        }
    }

    private fun showSavedWord(state: WordState) {
        val word = state.word
        val wordSpan = state.span ?: Span(0, 0)
        updateSheet(word, wordSpan, true)
    }

    private fun updateSheet(info: WordUI, span: Span, isWord: Boolean) {
        with(binding.transSheet) {
            if(isSheetVisible() && adapter.isInsideSelectedSentence(span)){
                showWordInfo(info, isWord)
            } else {
                showSheetInfo(info, isWord)
            }
        }
        adapter.unselectWord()
    }

    private fun showWordInfo(info: WordUI, isWord: Boolean) {
        with(binding.transSheet) {
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
        }
    }

    private fun showSheetInfo(info: WordUI, isWord: Boolean) {
        with(binding.transSheet) {
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

    @SuppressLint("ClickableViewAccessibility", "SetJavaScriptEnabled")
    private fun setBottomPanel(){
        with(binding) {
            val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
            val translateSheetBehavior = BottomSheetBehavior.from(transSheet.root)

            val bottomSheetBackCallback = createBackPressedCallback(bottomSheetBehavior)
            requireActivity().onBackPressedDispatcher.addCallback(this@WebReaderFragment, bottomSheetBackCallback)

            bottomSheetBehavior.addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_HIDDEN -> {
                            bottomSheetBackCallback.isEnabled = false
                            viewModel.hideWordLinks()
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

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.linksForWord.collect { state ->
                        when(state) {
                            DialogState.Empty -> bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                            is DialogState.Error -> Timber.e(state.exception, "Error retrieving links for word")
                            DialogState.Loading -> {}
                            is DialogState.Success -> {
                                val links = state.data.links
                                links.forEach { link -> link.link = link.link.replace("{q}", state.data.word) }

                                val adapter = ExternalLinksAdapter(links) { index ->
                                    viewModel.setWordLink(index)
                                }

                                adapter.setFlatButton(true)
                                val selectedPos = viewModel.selectedLink.value
                                adapter.setSelectedPos(selectedPos)
                                linksList.scrollToPosition(selectedPos)
                                linksList.adapter = adapter

                                launch {
                                    viewModel.selectedLink.collect {
                                        infoWebview.loadUrl(links[it].link)
                                        linksList.scrollToPosition(it)
                                    }
                                }

                                composeBar.isVisible = false
                                root.post { bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED }
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setTranslateBottomPanel() {
        with(binding.transSheet) {
            wordTranslation.movementMethod = ScrollingMovementMethod()
            wordTranslation.setHorizontallyScrolling(true)

            val bottomSheetBehavior = BottomSheetBehavior.from(root)

            val backPressedCallback = createBackPressedCallback(bottomSheetBehavior)
            requireActivity().onBackPressedDispatcher.addCallback(this@WebReaderFragment, backPressedCallback)

            bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        backPressedCallback.isEnabled = false
                        adapter.unselectWord()
                        setWordSheetViews(false)
                        binding.composeBar.isVisible = true
                        viewModel.clearTextInfo()
                        updateListBottomPadding(0)
                    } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                        backPressedCallback.isEnabled = true
                        binding.composeBar.isVisible = false
                        updateListBottomPadding(root.height)
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch {
                        viewModel.dialogState.collect(::handleUiDialogState)
                    }

                    launch {
                        viewModel.editDialogs.collect { result ->
                            when(val type = result.isEditingType) {
                                is DialogType.Note -> {
                                    addNoteDialogVisible.value = AddNoteDialogUI(type.item.noteText, type.item.color, type.item.noteSaved)
                                    pickNewTypeDialogVisible.value = false
                                }
                                is DialogType.SavedWord -> {
                                    val word = type.word
                                    val newState = WordState(word.toUI(), word.id, span = type.span)
                                    editWordDialogVisible.value = EditDeleteDialogUI(newState, result.isDeleteDialogShown, LanguagesList(languagesFull, languagesISO))
                                    pickNewTypeDialogVisible.value = false
                                }
                                is DialogType.Translation -> pickNewTypeDialogVisible.value = true
                                null -> {
                                    editWordDialogVisible.value = null
                                    addNoteDialogVisible.value = null
                                    pickNewTypeDialogVisible.value = false
                                }
                            }

                            pickInfoDialogVisible.value = result.isPickingType != null
                        }
                    }
                }
            }

            addNoteBtn.setOnClickListener {
                viewModel.startEditing()
            }

            addWordNoteBtn.setOnClickListener {
                viewModel.startEditing()
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

    private fun getPageVersion() = when(viewModel.pageVersion) {
        PageVersion.LOCAL -> "Local"
        PageVersion.WEB -> "Web"
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
        val span = adapter.getHighlightedTextSpan()
        if (text != null && span != null) {
            viewModel.translateText(text.toString(), span, adapter.isOverlappingNotes, adapter.isOverlappingSavedWord)
            adapter.selectHighlightedText()
        }
    }

    private fun showSheetWithNote(note: EditNote) {
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

    private fun handleUiDialogState(result: WebReaderViewModel.UiDialogState) {
        with(binding.transSheet) {
            val bottomSheetBehavior = BottomSheetBehavior.from(root)
            if (result.isLoading) {
                barLoading.isVisible = true
                translatedText.text = ""
                translatedText.scrollTo(0, 0)
                notesText.text = ""
                moreInfoBtn.isVisible = false
                addNoteBtn.isVisible = false
                setWordSheetViews(false)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else if (result.isWordLoading) {
                barLoading.isVisible = true
                wordTranslation.text = ""
                moreInfoWordBtn.isInvisible = true
                addWordNoteBtn.isInvisible = true
            } else {
                barLoading.isVisible = false
                moreInfoBtn.isVisible = false
                addNoteBtn.isVisible = false
                val state = result.dialogState
                val sentence = result.sentence
                if (sentence != null) {
                    translatedText.text = sentence.text
                    notesText.isVisible = false
                    when(state) {
                        is DialogType.Note -> {
                            val note = state.item
                            val info = WordUI(note.text, "", note.noteText)
                            showWordInfo(info, note.text.isWord())
                        }
                        is DialogType.SavedWord -> {
                            val word = state.word
                            val newState = WordState(word.toUI(), word.id, state.span)
                            showWordInfo(newState.word, true)
                            if (!viewModel.showWords) adapter.selectWordInSentence(sentence.paragraphIndex, state.span)
                        }
                        is DialogType.Translation -> {
                            val translation = state.translation
                            wordTranslation.text = translation.translation
                            moreInfoWordBtn.setOnClickListener {
                                viewModel.getLinksForWord(translation.original, translation.sourceLang)
                            }

                            setWordSheetViews(true)

                            adapter.selectWordInSentence(sentence.paragraphIndex, state.span)
                            addWordNoteBtn.setImageResource(R.drawable.baseline_note_add_24)
                        }
                        else -> setWordSheetViews(false)
                    }
                } else {
                    when(state) {
                        is DialogType.Note -> showSheetWithNote(state.item)
                        is DialogType.SavedWord -> {
                            val word = state.word
                            val newState = WordState(word.toUI(), word.id, state.span)
                            showSavedWord(newState)
                            if (!viewModel.showWords) adapter.selectWord(state.span)
                        }
                        is DialogType.Translation -> {
                            val translation = state.translation
                            translatedText.text = translation.translation

                            // Only show the add note button if selection is not a sentence and doesn't overlap any other note
                            val text = translation.original
                            val isWord = text.isWord()
                            val noteUnavailable = state.overlapsNote || !result.isPageSaved
                            addNoteBtn.isGone = noteUnavailable && !isWord
                            addNoteBtn.setImageResource(R.drawable.baseline_note_add_24)

                            adapter.selectWord(state.span)

                            moreInfoBtn.isVisible = true
                            moreInfoBtn.setOnClickListener {
                                viewModel.getLinksForWord(translation.original, translation.sourceLang)
                            }

                            translatedText.post {
                                updateListBottomPadding(root.height)
                            }
                        }
                        null -> {
                            deleteDialogVisible.value = false
                            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                        }
                    }
                }

                root.post {
                    if (state != null || result.sentence != null) bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }

                binding.composeBar.isVisible = state == null && result.sentence == null
            }
    }
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
            onDismiss = viewModel::stopEditing,
            onDelete = viewModel::deleteCurrentNote,
            onSaveClicked = { newNote ->
                viewModel.saveCurrentNote(newNote.text, newNote.colorHex)
            },
        )
    }

    @Composable
    fun Dialogs() {

        EditDeleteWordDialogs(
            editWordDialogVisible.value,
            onSave = {
                val resultWord = it.toWord()
                val state = editWordDialogVisible.value?.word ?: return@EditDeleteWordDialogs
                resultWord.id = state.dbId
                if (state.span != null) viewModel.upsert(resultWord, state.span)
            },
            onDismiss =  viewModel::stopEditing,
            deleteDialogChange = viewModel::setDeleteSate,
            onDelete = {
                val state = editWordDialogVisible.value?.word ?: return@EditDeleteWordDialogs
                viewModel.deleteWord(state.toWord())
            },
        )

        if (pickNewTypeDialogVisible.value) {
            DialogList(
                list = listOf(resources.getString(R.string.note), resources.getString(R.string.saved_word)),
                title = resources.getString(R.string.create_db_item_dialog_title),
                onItemSelected = { index, _ ->
                    when (index) {
                        0 -> viewModel.newNote()
                        1 -> viewModel.newSavedWord()
                    }
                },
                onDismiss = viewModel::stopEditing
            )
        }

        if (pickInfoDialogVisible.value) {
            DialogList(
                list = listOf(resources.getString(R.string.note), resources.getString(R.string.saved_word)),
                title = resources.getString(R.string.pick_info_dialog_title),
                onItemSelected = { index, _ ->
                    when (index) {
                        0 -> viewModel.pickItem(isNote = true, adapter.getSelectedSentenceSpan())
                        1 -> viewModel.pickItem(isNote = false, adapter.getSelectedSentenceSpan())
                    }
                    viewModel.stopPickingInfo()
                },
                onDismiss = viewModel::stopPickingInfo
            )
        }
    }
}
