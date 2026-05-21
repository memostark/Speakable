package com.guillermonegrete.tts.webreader

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.*
import android.webkit.WebViewClient
import android.widget.TextView
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
import com.guillermonegrete.tts.common.models.toEditNote
import com.guillermonegrete.tts.common.models.toNote
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
import com.guillermonegrete.tts.webreader.db.span
import com.guillermonegrete.tts.webreader.model.ModifiedNote
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import kotlin.text.isNotEmpty
import androidx.core.graphics.toColorInt
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.guillermonegrete.tts.ImporttextDirections
import com.guillermonegrete.tts.common.notes.NotesListFragment
import com.guillermonegrete.tts.common.views.CharacterSmoothScroller
import com.guillermonegrete.tts.db.NoteType
import kotlin.math.abs

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
    private var topInset = 0

    private var jumpToPos: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFragmentResultListener(NotesListFragment.NOTES_LIST_RESULT_KEY) { _, bundle ->
            // We use a String here, but any type that can be put in a Bundle is supported.
            val result = bundle.getInt(NotesListFragment.CHAR_POSITION_KEY)
            jumpToPos = result
        }
    }

    override fun onPause() {
        super.onPause()
        val charPosition = getFirstVisibleCharPosition()
        viewModel.saveWebLink(charPosition)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        appBarSize = resources.getDimensionPixelSize(R.dimen.web_reader_bar_height)
        setupOptionsMenu()
        _binding = FragmentWebReaderBinding.bind(view)
        adapter = ParagraphAdapter(
            viewModel,
            viewModel.getGesturePreferences(),
            onSentenceSelected = viewModel::sentenceSelected,
            onParagraphSelected = viewModel::paragraphSelected,
            onParagraphEvent = {
                when (it) {
                    is ParagraphAdapter.ParagraphEvent.BottomClick -> viewModel.setSentenceInParagraph(it.itemIndex, it.charPos)
                    is ParagraphAdapter.ParagraphEvent.ToggleClick -> viewModel.paragraphSelected(null)
                    is ParagraphAdapter.ParagraphEvent.TopClick -> viewModel.onParagraphWordClicked(it.word, it.position, it.wordSpan)
                }
            },
            onTextHighlighted = {
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
                        Timber.e(it.throwable, "Error loading page")
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
                        viewModel.notes.collect { notes ->
                            val dbNotes = notes.toMutableList()

                            var index = 0
                            val notes = adapter.items.map {
                                val nextIndex = index + it.original.length
                                // Search the notes applied to this paragraph
                                val paragraphNotes = dbNotes.filter { dbNote ->
                                    dbNote.position in index until nextIndex
                                }

                                val noteItems = paragraphNotes.map { note ->
                                    val itemStart = note.position - index
                                    NoteItem(note.text, Span(itemStart, itemStart + note.length), note.color.toColorInt(), note.id)
                                }

                                index = nextIndex
                                dbNotes.removeAll(paragraphNotes)
                                noteItems
                            }

                            adapter.updateNotes(notes, getVisibleListItems())
                        }
                    }

                    launch {
                        viewModel.updatedNote.collect { result ->
                            when(result){
                                is ModifiedNote.Update -> {
                                    val note = result.note
                                    val dialogResult = AddNoteResult(note.text, note.color)
                                    adapter.updateNote(note.span, note.id, dialogResult)
                                }
                                is ModifiedNote.Delete -> {
                                    adapter.unselectWord()
                                    adapter.deleteNote(result.noteId)
                                }
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
        ViewCompat.setOnApplyWindowInsetsListener(binding.paragraphsList) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            topInset = insets.top
            binding.composeBar.updatePadding(bottom = insets.bottom)
            binding.composeRoot.updatePadding(top = insets.top)
            // Handle links bottom sheet insets
            binding.linksList.updatePadding(bottom = insets.bottom)

            appBarSize = initialBarSize + insets.bottom
            handleListPadding(insets.top, appBarSize)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun handleListPadding(top: Int, bottom: Int) {
        val v = binding.paragraphsList
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            v.updatePadding(top = top, bottom = bottom)
        } else {
            // Avoid unnecessarily updating padding because in older version it cancels the action mode (e.g. text selection).
            if (v.paddingTop != top && v.paddingTop != bottom) {
                v.updatePadding(top = top, bottom = bottom)
            }
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
            is WebReaderMenuAction.PageVersionToggle -> viewModel.setPageVersion(action.version)
            is WebReaderMenuAction.ShowWords -> {
                viewModel.showWords = action.shown
                if (action.shown) loadWordsForVisibleItems() else hideSavedWords()
            }

            is WebReaderMenuAction.CopyLink -> {
                val clipboardManager = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val linkText = requireContext().getString(R.string.link_description)
                clipboardManager.setPrimaryClip(ClipData.newPlainText(linkText, args.link))
            }
            WebReaderMenuAction.OpenNotesList -> {
                val id = viewModel.getWebLinkId() ?: return
                findNavController().navigate(ImporttextDirections.toNotesListFragment(id, NoteType.WEB_LINK))
            }
        }
    }

    private fun setParagraphList(page: PageInfo, iconsVisible: MutableState<Boolean>) {
        pageText = page.text

        with(binding) {
            paragraphsList.isVisible = true

            // Split text and parse from HTML
            val newParagraphs =  page.text.split("\n")
                .map { HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_COMPACT).trim() }
                .filter { it.isNotEmpty() }
            // Create items for adapter
            val splitParagraphs = viewModel.createParagraphs(newParagraphs)
            var index = 0
            val paragraphItems = splitParagraphs.map {
                val startIndex = index
                index += it.paragraph.length
                ParagraphAdapter.ParagraphItem(it.paragraph, it.indexes, it.sentences, mutableListOf(), startIndex)
            }

            adapter.isPageSaved = page.isLocalPage
            adapter.updateItems(paragraphItems)
            paragraphsList.adapter = adapter
            paragraphsList.post {
                if (viewModel.firstLoad) {
                    jumpToChar(viewModel.getCharPos())
                    viewModel.firstLoad = false
                }

                jumpToPos?.let { charPos ->
                    jumpToChar(charPos)
                    jumpToPos = null
                }
                loadWordsForVisibleItems()
            }

            iconsVisible.value = true
            setAdapterListeners()

            viewModel.getNotes()
        }
    }

    private fun jumpToChar(charPos: Int) {
        val position = adapter.getPositionInList(charPos)
        val paragraphsList = binding.paragraphsList
        paragraphsList.post {
            val localPos = adapter.getLocalCharPosition(position, charPos)

            val smoothScroller = CharacterSmoothScroller(requireContext(), localPos)
            smoothScroller.targetPosition = position
            val layoutManager = paragraphsList.layoutManager
            layoutManager?.startSmoothScroll(smoothScroller)
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
                            is ParagraphAdapter.TextClick.Note -> viewModel.setNoteData(result.item.toNote(), adapter.getSelectedSentenceSpan())
                            is ParagraphAdapter.TextClick.Overlap -> {
                                if (result.word.span != null)
                                    viewModel.setPickInfoType(DialogType.SavedWord(result.word.toWord(), result.word.span), DialogType.Note(result.note.toNote()))
                            }
                        }
                    }
                }

                launch {
                    adapter.addNoteClicked.collect { note ->
                        viewModel.startEditing(DialogType.Note(note.toNote()))
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
        if(isSheetVisible() && adapter.isInsideSelectedSentence(span)){
            showWordInfo(info, isWord)
        } else {
            showSheetInfo(info, isWord)
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
        adapter.unselectWord()
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
        binding.paragraphsList.adapter = null
        binding.linksList.adapter = null
        binding.infoWebview.destroy() // to avoid memory leaks
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
            val linksSheetCollapsedHeight = resources.getDimensionPixelSize(R.dimen.links_sheet_collapsed_height)

            val bottomSheetBackCallback = createBackPressedCallback(bottomSheetBehavior)
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, bottomSheetBackCallback)

            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            bottomSheetBehavior.addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_HIDDEN -> {
                            bottomSheetBackCallback.isEnabled = false
                            viewModel.hideWordLinks()
                            if (translateSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) composeBar.isVisible = true
                        }
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            // Handle insets for the sheet using padding when it's expanded
                            // Don't use margin because it causes a twitch when changing links due to a bug with the material library
                            bottomSheet.updatePadding(top = topInset)
                            infoWebview.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                                bottomMargin = linksList.height
                            }
                            bottomSheetBackCallback.isEnabled = true
                            viewModel.setLinkSheetState(true)
                        }
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            bottomSheetBackCallback.isEnabled = true
                            viewModel.setLinkSheetState(false)
                            if (bottomSheet.paddingTop != 0) bottomSheet.updatePadding(top = 0)
                            infoWebview.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                                bottomMargin = bottomSheet.top + linksList.height
                            }
                        }
                        else -> {
                            if (bottomSheet.paddingTop != 0) bottomSheet.updatePadding(top = 0)
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    adjustLinksList(linksSheetCollapsedHeight)
                }
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
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                    launch {
                        viewModel.linksForWord.collect { state ->
                            when (state) {
                                DialogState.Empty -> bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                                is DialogState.Error -> { handleLinksError(state.exception) }
                                DialogState.Loading -> {}
                                is DialogState.Success -> {
                                    val links = state.data.links
                                    links.forEach { link ->
                                        link.link = link.link.replace("{q}", state.data.word)
                                    }

                                    val adapter = ExternalLinksAdapter(links) { index ->
                                        viewModel.setWordLink(index)
                                    }

                                    adapter.setFlatButton(true)
                                    val selectedPos = viewModel.selectedLink.value
                                    adapter.setSelectedPos(selectedPos)
                                    linksList.scrollToPosition(selectedPos)
                                    linksList.adapter = adapter

                                    lifecycleScope.launch {
                                        viewModel.selectedLink.collect {
                                            infoWebview.loadUrl(links[it].link)
                                            linksList.scrollToPosition(it)
                                        }
                                    }

                                    composeBar.isVisible = false
                                }
                            }
                        }
                    }

                    launch {
                        viewModel.linksSheetExpanded.collect {
                            it ?: return@collect
                            root.post {
                                bottomSheetBehavior.state = if (it) BottomSheetBehavior.STATE_EXPANDED else BottomSheetBehavior.STATE_COLLAPSED
                                adjustLinksList(linksSheetCollapsedHeight)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleLinksError(exception: Exception) {
        val snackBar = Snackbar.make(
            binding.root,
            getString(R.string.loading_links_error_msg),
            Snackbar.LENGTH_SHORT
        )
        snackBar.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                super.onDismissed(transientBottomBar, event)
                viewModel.hideWordLinks()
            }
        })
        snackBar.show()
        Timber.e(exception, "Error retrieving links for word")
    }

    /**
     * Adjusts the position of the external links list to be always fixed at the bottom of the screen if the sheet is expanded.
     */
    private fun adjustLinksList(linksSheetCollapsedHeight: Int) {
        val bottomSheet = binding.bottomSheet
        val linksList = binding.linksList
        val bottomSheetVisibleHeight = bottomSheet.height - bottomSheet.top + bottomSheet.marginTop
        // Only adjust if the sheet is not collapsed, otherwise the list shouldn't be fixed to the bottom.
        val listPos =
            (if (bottomSheetVisibleHeight > linksSheetCollapsedHeight) bottomSheetVisibleHeight else linksSheetCollapsedHeight)
        linksList.y = (listPos - linksList.height).toFloat()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setTranslateBottomPanel() {
        with(binding.transSheet) {
            wordTranslation.movementMethod = ScrollingMovementMethod()
            wordTranslation.setHorizontallyScrolling(true)

            val bottomSheetBehavior = BottomSheetBehavior.from(root)

            val backPressedCallback = createBackPressedCallback(bottomSheetBehavior)
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)

            bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        backPressedCallback.isEnabled = false
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
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
                                    adapter.setParagraphLoading()
                                } else {
                                    val paragraphUi = ParagraphAdapter.SelectedParagraph(paragraph.index, paragraph.translation?.translatedText, paragraph.highlights, paragraph.selectedWord)
                                    adapter.displayParagraph(paragraphUi)
                                    if (paragraph.selectedWord != null)
                                        adapter.selectParagraphWord(paragraph.selectedWord)
                                    else
                                        adapter.unselectParagraphWord()
                                }
                            } else {
                                adapter.unselectParagraph()
                            }
                        }
                    }

                    launch {
                        viewModel.dialogState.collect(::handleUiDialogState)
                    }

                    launch {
                        viewModel.editDialogs.collect { result ->
                            when(val type = result.isEditingType) {
                                is DialogType.Note -> {
                                    val item = type.item.toEditNote()
                                    addNoteDialogVisible.value = AddNoteDialogUI(item.noteText, item.color, item.noteSaved)
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

    private fun onTranslateClicked() {
        if (viewModel.translateParagraph()) return

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
            } else {
                if (result.error != null) {
                    Timber.e("Error when translating: ${result.error}")
                    sbScope?.launch {
                        snackbarHostState.value.showSnackbar(getString(R.string.error_translation))
                        viewModel.errorShown()
                    }
                }

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
                            val info = WordUI(note.originalText, "", note.text)
                            showWordInfo(info, note.originalText.isWord())
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
                        else -> {
                            adapter.unselectWord()
                            setWordSheetViews(false)
                        }
                    }

                    if (result.isWordLoading) {
                        barLoading.isVisible = true
                        wordTranslation.text = ""
                        moreInfoWordBtn.isInvisible = true
                        addWordNoteBtn.isInvisible = true
                    }
                } else {
                    when(state) {
                        is DialogType.Note -> showSheetWithNote(state.item.toEditNote())
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
                            if (!viewModel.paragraphWordSelected()) adapter.unselectWord()
                        }
                    }
                }
            }
            val dialogVisible = result.dialogState != null || result.sentence != null || result.isLoading || result.isWordLoading
            root.post {
                if (dialogVisible) bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
            binding.composeBar.isGone = dialogVisible
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
        adapter.addSavedWords(dbWord.id, paragraphWords, range.first)
    }

    private fun findWordsInItems(dbWords: List<Words>, range: IntRange): List<List<WordState>> {
        return range.map { pos ->
            viewModel.findWordsInParagraph(dbWords,  adapter.getText(pos), pos)
        }
    }

    private fun updateListBottomPadding(pixels: Int) {
        val extra = if(pixels == 0) appBarSize else requireContext().dpToPixel(16)
        binding.paragraphsList.updatePadding(bottom = pixels + extra)
    }

    private fun loadWordsForVisibleItems() {
        val range = getVisibleListItems()
        adapter.initialRange = range
        val text = adapter.getItemsText(range)
        viewModel.loadLocalWords(text, range.first) {
            adapter.initialWordsLoaded = true
        }
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

    /**
     * Returns the position in absolute terms of the start character of the first visible line.
     */
    private fun getFirstVisibleCharPosition(): Int {
        val layoutManager = binding.paragraphsList.layoutManager as LinearLayoutManager
        val index = layoutManager.findFirstVisibleItemPosition()
        val view = layoutManager.findViewByPosition(index) as? TextView
        if (view != null) {
            // The top property is the distance between the RecyclerView and the item (remove any inset padding)
            // convert to positive because it's usually negative if the top is off-screen due to scroll
            val offscreenHeight = abs(view.top - binding.paragraphsList.paddingTop)
            val layout = view.layout
            val line = layout.getLineForVertical(offscreenHeight)
            val relativePos = layout.getLineStart(line)
            val absolutePos = adapter.getAbsoluteCharPosition(index, relativePos)
            Timber.d("Index: $index, offscreen: $offscreenHeight, line: $line, line start rel: $relativePos, abs: $absolutePos")
            return absolutePos
        }
        return 0
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
