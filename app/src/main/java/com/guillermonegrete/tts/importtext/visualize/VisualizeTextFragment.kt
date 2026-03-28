package com.guillermonegrete.tts.importtext.visualize

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.IntentCompat
import androidx.core.content.edit
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.guillermonegrete.tts.EventObserver
import com.guillermonegrete.tts.ImporttextDirections
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.common.compose.DialogList
import com.guillermonegrete.tts.common.compose.ExternalLinkList
import com.guillermonegrete.tts.common.compose.ExternalLinksDialog
import com.guillermonegrete.tts.common.models.EditNote
import com.guillermonegrete.tts.common.models.NoteItem
import com.guillermonegrete.tts.common.models.Span
import com.guillermonegrete.tts.common.models.toEditNote
import com.guillermonegrete.tts.common.models.toNote
import com.guillermonegrete.tts.common.models.toUI
import com.guillermonegrete.tts.data.DialogState
import com.guillermonegrete.tts.databinding.FragmentVisualizeTextBinding
import com.guillermonegrete.tts.db.ExternalLink
import com.guillermonegrete.tts.db.NoteType
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.importtext.epub.NavPoint
import com.guillermonegrete.tts.importtext.visualize.io.EpubFileManager
import com.guillermonegrete.tts.importtext.visualize.model.BookChapter
import com.guillermonegrete.tts.importtext.visualize.model.SplitPageSpan
import com.guillermonegrete.tts.textprocessing.TextInfoDialog
import com.guillermonegrete.tts.textprocessing.WordState
import com.guillermonegrete.tts.textprocessing.toWord
import com.guillermonegrete.tts.ui.BrightnessTheme
import com.guillermonegrete.tts.ui.theme.VisualizerTheme
import com.guillermonegrete.tts.utils.getScreenSizes
import com.guillermonegrete.tts.webreader.AddNoteDialog
import com.guillermonegrete.tts.webreader.AddNoteDialogUI
import com.guillermonegrete.tts.webreader.DialogType
import com.guillermonegrete.tts.webreader.db.spanBook
import com.guillermonegrete.tts.webreader.model.ModifiedNote
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.BreakIterator
import javax.inject.Inject
import kotlin.math.abs
import androidx.core.graphics.toColorInt
import androidx.fragment.app.setFragmentResultListener
import com.guillermonegrete.tts.common.notes.NotesListFragment
import com.guillermonegrete.tts.webreader.db.BookPosition
import com.guillermonegrete.tts.webreader.db.Note

@AndroidEntryPoint
class VisualizeTextFragment: Fragment(R.layout.fragment_visualize_text), DialogInterface.OnCancelListener {

    private val viewModel: VisualizeTextViewModel by viewModels()

    private var _binding: FragmentVisualizeTextBinding? = null
    private val binding get() = _binding!!

    private var _viewPager: ViewPager2? = null
    private val viewPager get() = _viewPager!!

    // Bottom sheet layout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ViewGroup>

    private var pageItemView: View? = null

    private lateinit var pagesAdapter: VisualizerAdapter
    private lateinit var callback: OnBackPressedCallback
    private var pagerCallback: ViewPager2.OnPageChangeCallback? = null

    @Inject
    lateinit var preferences: SharedPreferences
    @Inject
    lateinit var brightnessTheme: BrightnessTheme
    @Inject
    lateinit var fileManager: EpubFileManager
    @StyleRes
    private var themeRes = R.style.AppMaterialTheme_Black

    private val addNoteDialogVisible = mutableStateOf<AddNoteDialogUI?>(null)
    private val noteSheetVisible = mutableStateOf(false)
    private var linksDialogShown = mutableStateOf(false)
    private val pickInfoDialogVisible = mutableStateOf(false)
    private val contentsMenuVisible = mutableStateOf(false)
    private val wordLinks = mutableStateOf(ExternalLinkList(emptyList()))
    private var selectedLinkPos = mutableIntStateOf(0)

    private var noteInfo = mutableStateOf<EditNote?>(null)
    private var clickedWord = ""

    private var splitterCreated = false

    private var scaleDetector: ScaleGestureDetector? = null

    private var cardWidth = 0
    /**
     * The vertical pixel distance between the center of the card and the center of the screen.
     *
     * A positive distance means the screen's center is below the card's, negative means the card's center is below.
     */
    private var cardYOffset = 0f

    /**
     * The ratio between the size of the screen and card view, ratio = cardWith / screenWidth
     * Used to get the desired dimensions of the card.
     */
    private var ratio = 0.8f

    private var sheetBarHeight = 0

    private var jumpToPos: BookPosition? = null

    private var dialog: TextInfoDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPreferenceTheme()
        callback = requireActivity().onBackPressedDispatcher.addCallback(this, false) {
            viewModel.hideDialog()
        }
        val infoDialog = childFragmentManager.findFragmentByTag(TextInfoDialog.TAG)
        if (infoDialog is TextInfoDialog) dialog = infoDialog
        if (viewModel.fullScreen) hideSystemUi()

        setFragmentResultListener(NotesListFragment.NOTES_LIST_RESULT_KEY) { _, bundle ->
            jumpToPos = Note.getBookPosition(bundle.getInt(NotesListFragment.CHAR_POSITION_KEY))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentVisualizeTextBinding.bind(view)

        // Bottom sheet
        bottomSheetBehavior = BottomSheetBehavior.from(binding.visualizerBottomSheet)
        sheetBarHeight = resources.getDimensionPixelSize(R.dimen.visualize_sheet_bar_height)

        binding.brightnessSettingsBtn.setOnClickListener { showSettingsPopUp(binding.brightnessSettingsBtn) }

        if(SHOW_EPUB != requireActivity().intent.action) {
            binding.readerCurrentChapter.isGone = true
        }

        _viewPager = binding.textReaderViewpager
        // Creates one item so setPageTransformer is called
        // Used to get the page text view properties to create page splitter.
        pagesAdapter = VisualizerAdapter(
            listOf(VisualizerAdapter.PageItem.EMPTY),
            onCreateNote = {
                viewModel.startEditing(DialogType.Note(it.toNote()))
            },
            onTextClick = { result ->
                when(result) {
                    is VisualizerAdapter.TextClick.Note -> viewModel.setNoteData(result.note.toNote())
                    is VisualizerAdapter.TextClick.SavedWord -> {
                        val word = result.state.toWord()
                        val span = result.state.span
                        if (span != null) viewModel.setSavedWord(word, span)
                        showTextDialog(word.word)
                    }
                    is VisualizerAdapter.TextClick.Overlap -> {
                        clickedWord = result.word
                        viewModel.setPickInfoType(Words(result.word, "", ""), Span(0, 0), result.note.toNote())
                    }
                    is VisualizerAdapter.TextClick.Word -> {
                        viewModel.translateWord(result.word, result.span)
                        showTextDialog(result.word)
                    }
                }
            },
            measuringPage = true)
        viewPager.adapter = pagesAdapter

        viewPager.post{
            setBottomSheetPeekHeight()
            setBottomSheetCallbacks()
        }

        scaleDetector = ScaleGestureDetector(requireContext(), PinchListener(binding.textReaderCardView))

        setupCompose()
        setUIChangesListener()
        setUpSeekBar()

    }

    private fun setupCompose() {
        binding.composeRoot.apply {
            setContent {
                VisualizerTheme(theme = brightnessTheme) {
                    Sheet()

                    Dialogs()
                }
            }
        }
    }

    /**
     *  Changes the dimensions of the card to have the same aspect ratio as the screen
     *  and smaller size given by the defined ratio.
     */
    private fun setUpCardViewDimensions(insets: WindowInsetsCompat) {
        val screenSizes = requireContext().getScreenSizes()
        // Remove cutout height because it's not used
        val screenHeight = screenSizes.height

        val textCardView = binding.textReaderCardView
        val cardHeight = textCardView.height
        val cardCenterY = textCardView.y + cardHeight / 2
        cardYOffset = (screenHeight / 2) - cardCenterY
        ratio = cardHeight / screenHeight.toFloat()
        cardWidth = (screenSizes.width * ratio).toInt()

        val cutoutInsets = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
        setPageMargin(cutoutInsets)

        val cardParams = textCardView.layoutParams
        cardParams.width = cardWidth
        textCardView.layoutParams = cardParams
        if (viewModel.fullScreen) {
            val invRatio = 1f / ratio
            textCardView.post {
                textCardView.scaleX = invRatio
                textCardView.scaleY = invRatio
                textCardView.translationY = cardYOffset
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.saveBookData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ViewCompat.setOnApplyWindowInsetsListener(requireActivity().window.decorView, null)
        pageItemView = null
        scaleDetector = null
        splitterCreated = false
        pagerCallback = null
        viewPager.adapter = null
        _viewPager = null
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.pageSplitter = null
    }

    override fun onCancel(dialog: DialogInterface?) {
        viewModel.hideDialog()
    }

    private fun setPageTransformListener() {
        // Before setting the transformer make sure the card has finished updating.
        binding.textReaderCardView.post {
            viewPager.setPageTransformer { view, _ ->
                pageItemView = view

                setUpPageParsing(view)

                removeSelection()
            }
        }
    }

    /**
     * Handle scaling in text view with selectable text and clickable spans. Intercept touch event if it's scaling.
     *
     * Inspired by: https://stackoverflow.com/a/5369880/10244759
     */
    private var eventInProgress = false
    private var scaleInProgress = false

    fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val detector = scaleDetector ?: return false

        if (eventInProgress) {
            if (pageItemView?.isShown == true) detector.onTouchEvent(ev)
            if (detector.isInProgress) {
                // Cancel long press to avoid showing contextual action menu
                pageItemView?.cancelLongPress()
                scaleInProgress = true
                // Don't pass event when scaling
                return true
            }
        }

        when(ev.actionMasked){
            MotionEvent.ACTION_DOWN -> eventInProgress = true
            MotionEvent.ACTION_UP -> {
                eventInProgress = false

                if(scaleInProgress){
                    // Removes lingering highlight from text
                    removeSelection()
                    scaleInProgress = false
                    return true
                }
            }
        }

        // When scaling don't handle other events, this avoids unexpected clicks and changes of page
        return scaleInProgress
    }

    private fun removeSelection(){
        val item = pageItemView
        if(item is TextView && item.hasSelection()){
            val span = item.text as? Spannable
            Selection.removeSelection(span)
        }
    }

    /**
     * Hide the UI if it gets shown again (e.g. when opening an external link in the browser).
     *
     * This is for older devices because newer devices re-hide the UI automatically.
     */
    fun onWindowFocusChanged(hasFocus: Boolean) {
        if(hasFocus && viewModel.fullScreen) hideSystemUi()
    }

    private fun setUIChangesListener() {

        val window = requireActivity().window
        val view = window.decorView
        val initialMargin = resources.getDimensionPixelSize(R.dimen.visualize_default_margin)
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val systemBarInsets = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.statusBars())

            with(binding) {

                if (readerCurrentChapter.isVisible) {
                    readerCurrentChapter.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        topMargin = initialMargin + systemBarInsets.top
                    }
                } else {
                    textReaderCardView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        goneTopMargin = initialMargin + systemBarInsets.top
                    }
                }

                // Margin for the bottom icons
                readerCurrentPage.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = initialMargin + systemBarInsets.bottom
                }

                if (!splitterCreated) {
                    textReaderCardView.post {
                        setUpCardViewDimensions(insets)
                        setPageTransformListener()
                    }
                }
            }
            insets
        }
    }

    private fun setUpSeekBar(){

        binding.pagesSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser) viewPager.currentItem = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

    }

    /**
     * Change activity value at runtime: https://stackoverflow.com/a/6390025/10244759
     *
     * Make sure the fragment is committed before the parent activity calls setContentView() so the theme is applied correctly.
     */
    private fun setPreferenceTheme() {
        themeRes = when(brightnessTheme){
            BrightnessTheme.WHITE -> R.style.AppMaterialTheme_White
            BrightnessTheme.BEIGE -> R.style.AppMaterialTheme_Beige
            BrightnessTheme.BLACK -> R.style.AppMaterialTheme_Black
        }
        val activity = requireActivity()
        activity.setTheme(themeRes)
        if (activity is VisualizeTextActivity) activity.themeUpdated(brightnessTheme == BrightnessTheme.BLACK)
    }

    private fun createViewModel() {
        viewModel.apply {
            dataLoading.observe(viewLifecycleOwner) {
                binding.visualizerProgressBar.isVisible = it
            }

            bookChapter.observe(viewLifecycleOwner, EventObserver { chapterInfo ->
                updateCurrentChapterLabel()
                setUpPagerAndIndexLabel(chapterInfo)
            })

            book.observe(viewLifecycleOwner) {
                if (it.spine.isEmpty()) {
                    binding.readerCurrentChapter.visibility = View.GONE
                } else {
                    updateCurrentChapterLabel()
                    binding.readerCurrentChapter.visibility = View.VISIBLE
                }

                binding.showTocBtn.setOnClickListener { contentsMenuVisible.value = true }
                binding.showTocBtn.isVisible = true
            }

            val bottomText = binding.pageBottomTextView

            translatedPageIndex.observe(viewLifecycleOwner, EventObserver {
                val translation = viewModel.translatedPages[it]
                bottomText.text = translation?.translatedText
                pagesAdapter.notifyItemChanged(it)
            })

            translationLoading.observe(viewLifecycleOwner) {
                binding.pageTranslationProgress.isVisible = it
                if (it) bottomText.text = ""
            }

            translationError.observe(viewLifecycleOwner, EventObserver {
                Timber.e("Error translating page: $it")
                Toast.makeText(context, getString(R.string.error_translation), Toast.LENGTH_SHORT).show()
                bottomText.text = getString(R.string.click_to_translate_msg)
            })

            lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch {
                        pageSavedWords.collect { words ->
                            highlightSavedWords(words)
                        }
                    }

                    launch {
                        updatedNote.collect { result ->
                            when(result) {
                                is ModifiedNote.Update -> {
                                    val note = result.note
                                    val span = note.spanBook
                                    val noteItem = NoteItem(note.text, span, note.color.toColorInt(), note.id)
                                    pagesAdapter.updateNote(noteItem)
                                    noteInfo.value = EditNote(note.originalText, note.text, span, note.color.toColorInt(), true, note.id)
                                }

                                is ModifiedNote.Delete -> {
                                    pagesAdapter.deleteNote(result.noteId)
                                    noteSheetVisible.value = false
                                }
                            }
                        }
                    }

                    launch {
                        dialogState.collect(::handleUiDialogState)
                    }

                    launch {
                        editDialogs.collect { result ->
                            when(val type = result.isEditingType) {
                                is DialogType.Note -> {
                                    val item = type.item.toEditNote()
                                    addNoteDialogVisible.value = AddNoteDialogUI(item.noteText, item.color, item.noteSaved)
                                }
                                is DialogType.SavedWord -> {}
                                is DialogType.Translation -> {}
                                null -> addNoteDialogVisible.value = null
                            }

                            result.isPickingType?.let {
                                clickedWord = it.word.word.word
                            }
                            pickInfoDialogVisible.value = result.isPickingType != null
                        }
                    }

                    launch {
                        linksForWord.collect { state ->
                            when(state) {
                                DialogState.Empty -> linksDialogShown.value = false
                                is DialogState.Error -> {
                                    Timber.e(state.exception, "Error retrieving links for word")
                                    linksDialogShown.value = false
                                }
                                DialogState.Loading -> {}
                                is DialogState.Success -> {
                                    val links = state.data
                                    wordLinks.value = ExternalLinkList(links.map(ExternalLink::toUI))

                                    linksDialogShown.value = true
                                }
                            }
                        }
                    }

                    launch {
                        selectedLink.collect { selectedLinkPos.intValue = it }
                    }
                }
            }


            languagesISO = resources.getStringArray(R.array.googleTranslateLanguagesValue)
        }
    }

    private fun highlightSavedWords(dbWords: List<Words>) {
        val text = pagesAdapter.getPageText(viewPager.currentItem).toString()
        val words = arrayListOf<WordState>()

        val iterator = BreakIterator.getWordInstance()
        iterator.setText(text)
        var start = iterator.first()
        var end = iterator.next()

        while (end != BreakIterator.DONE) {
            val possibleWord = text.substring(start, end)
            val dbWord = dbWords.find { it.word == possibleWord }
            if (dbWord != null) {
                words.add(WordState(dbWord.toUI(), dbWord.id, Span(start, end)))
            }
            start = end
            end = iterator.next()
        }
        pagesAdapter.updateSavedWords(words, viewPager.currentItem)
    }

    private fun initParse(fileReader: DefaultZipFileReader?) {
        val intent = requireActivity().intent
        if(SHOW_EPUB == intent.action) {
            if (fileReader == null) {
                Toast.makeText(context, "Couldn't open file", Toast.LENGTH_SHORT).show()
                requireActivity().finish()
                return
            }
            val uri: Uri = IntentCompat.getParcelableExtra(intent, EPUB_URI, Uri::class.java) ?: return
            viewModel.fileReader = fileReader
            viewModel.fileUri = uri.toString()
            viewModel.fileId = intent.getIntExtra(FILE_ID, -1)
            viewModel.parseEpub()
        } else {
            viewModel.parseSimpleText(getIntentText())
        }
    }

    private fun setUpPagerAndIndexLabel(chapter: BookChapter){
        pagesAdapter.measuringPage = false
        pagesAdapter.hasBottomSheet = viewModel.hasBottomSheet
        pagesAdapter.isPageSplit = viewModel.isSheetExpanded
        pagesAdapter.updateItems(createPageItems(chapter))
        viewPager.adapter = pagesAdapter
        addPagerCallback()

        val pages = chapter.pages
        val position = viewModel.currentPage

        // Set the page and chapter.
        jumpToPos?.let {
            viewModel.jumpToChapter(it.chapter) {
                val position = pagesAdapter.getCharListIndex(it.charPos)
                viewPager.setCurrentItem(position, false)
            }
            jumpToPos = null
        } ?: run {
            binding.readerCurrentPage.text = resources.getString(R.string.reader_current_page_label, position + 1, pages.size) // Example: 1 / 33
            viewPager.setCurrentItem(position, false)
        }

        // Subtract 1 because seek bar is zero based numbering
        binding.pagesSeekBar.max = pages.size - 1
        binding.pagesSeekBar.progress = position

        // Restore UI state in case of config change
        binding.visualizerBottomSheet.isVisible = viewModel.hasBottomSheet
        setFullBottomSheet(viewModel.isSheetExpanded)
    }

    private fun createPageItems(chapter: BookChapter): List<VisualizerAdapter.PageItem> {
        var index = 0
        val paragraphItems = mutableListOf<VisualizerAdapter.PageItem>()
        val dbNotes = chapter.notes.toMutableList()

        val currentPage = viewModel.getInitialPage()
        chapter.pages.forEachIndexed { i, page ->
            val nextIndex = index + page.length
            val pageSpan = Span(index, nextIndex - 1)
            // Search the notes applied to this paragraph
            val paragraphNotes = dbNotes.filter { dbNote ->
                dbNote.spanBook.intersects(pageSpan)
            }.map { it.copy(position = it.getPosInChapter()) }

            val noteItems = paragraphNotes.map { note ->
                val itemStart = note.position - index
                NoteItem(note.text, Span(itemStart, itemStart + note.length), note.color.toColorInt(), note.id)
            }.toMutableList()

            paragraphItems.add(VisualizerAdapter.PageItem(page, noteItems, index))
            index = nextIndex
            dbNotes.removeAll(paragraphNotes)

            if (i == currentPage) viewModel.verifySpanInPage(pageSpan)
        }

        return paragraphItems
    }

    private fun showSettingsPopUp(view: View) {

        val languagesISO = viewModel.languagesISO

        val popUpCallback = object: VisualizerSettingsWindow.Callback{

            override fun onBackgroundColorSet(theme: BrightnessTheme) {
                setBackgroundColor(theme)
            }

            override fun onPageMode(isSplit: Boolean) {
                setSplitPageMode(isSplit)
            }

            override fun onLanguageToChanged(position: Int) {
                viewModel.languageTo = languagesISO[position]
            }

            override fun onLanguageFromChanged(position: Int) {
                viewModel.languageFrom = if (position == 0) "auto" else languagesISO[position - 1]
            }
        }

        val lang = viewModel.languageFrom
        VisualizerSettingsWindow(
            view,
            themeRes,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            viewModel.hasBottomSheet,
            languagesISO,
            lang,
            viewModel.languageTo,
            popUpCallback,
        ).apply {
            isFocusable = true
            elevation = 8f
            showAtLocation(view, Gravity.START or Gravity.BOTTOM, 24, 24)
        }
    }

    private fun setSplitPageMode(isEnabled: Boolean){
        val position = viewModel.currentPage

        viewModel.hasBottomSheet = isEnabled
        pagesAdapter.hasBottomSheet = isEnabled
        viewPager.adapter = pagesAdapter
        viewPager.setCurrentItem(position, false)

        binding.visualizerBottomSheet.isVisible = isEnabled
    }

    private fun setBackgroundColor(theme: BrightnessTheme){
        if(theme != brightnessTheme) {
            saveBrightnessPreference(theme.value)
            val activity = requireActivity()
            activity.finish()
            startActivity(activity.intent)
        }
    }

    private fun saveBrightnessPreference(preference: String){
        preferences.edit{ putString(BrightnessTheme.PREFERENCE_KEY, preference) }
    }

    private fun addPagerCallback(){
        if (pagerCallback == null) {
            var swipeFirst = false
            val callback = object : ViewPager2.OnPageChangeCallback() {

                var previousPage = -1

                override fun onPageSelected(position: Int) {
                    viewModel.currentPage = position

                    val pageNumber = position + 1
                    binding.readerCurrentPage.text = resources.getString(R.string.reader_current_page_label, pageNumber, viewModel.pagesSize)
                    binding.pagesSeekBar.progress = position

                    if (pagesAdapter.hasBottomSheet)
                        binding.pageBottomTextView.text = viewModel.translatedPages[position]?.translatedText ?: getString(R.string.click_to_translate_msg)

                    if (previousPage != -1) {
                        // Can't update items directly in the pager callback methods, need to wait until layout measurements are done.
                        viewPager.post {
                            viewModel.hideDialog()
                            pagesAdapter.notifyItemChanged(previousPage, VisualizerAdapter.UNSELECT_SENTENCE)
                        }
                    }

                    // Load saved words when reaching new page
                    val text = pagesAdapter.getPageText(position)
                    val words = splitByWords(text.toString())
                    viewModel.loadLocalWords(words)

                    previousPage = position
                }

                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    if (state == ViewPager2.SCROLL_STATE_DRAGGING) swipeFirst = true
                }

                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {

                    if (positionOffset > 0) {
                        swipeFirst = false
                    } else {
                        if (swipeFirst) {
                            swipeFirst = false
                            if (position == 0) {
                                viewModel.swipeChapterLeft()
                            } else if (position == viewModel.pagesSize - 1) {
                                viewModel.swipeChapterRight()
                            }
                        }
                    }
                }
            }
            viewPager.registerOnPageChangeCallback(callback)
            pagerCallback = callback
        }
    }

    private fun setUpPageParsing(focusedView: View){
        // Execute only one time
        if(!splitterCreated) {

            createViewModel()

            val pageTextView: TextView = focusedView.findViewById(R.id.page_text_view)
            val fileReader = createFileReader()
            val splitter = if (fileReader != null) InputStreamImageGetter(requireContext(), fileReader) else null
            viewModel.pageSplitter = PageSplitter(pageTextView, focusedView.width, splitter)
            initParse(fileReader)
            splitterCreated = true
        }
    }

    private fun setPageMargin(insets: Insets) {
        var shouldUpdate = false

        val defaultMargin = resources.getDimensionPixelSize(R.dimen.visualize_page_horizontal_margin)
        // Get the biggest horizontal inset, calculate how much padding the cards needs to not overlap it.
        // If it's bigger than the default padding then update it.
        val requiredMargin = (ratio * insets.left.coerceAtLeast(insets.right)).toInt()
        if (requiredMargin > defaultMargin && requiredMargin != pagesAdapter.horizontalMargin) {
            pagesAdapter.horizontalMargin = requiredMargin
            shouldUpdate = true
        }

        val defaultTopMargin = resources.getDimensionPixelSize(R.dimen.visualize_page_top_margin)
        val requiredTopMargin = (ratio * insets.top.coerceAtLeast(insets.bottom)).toInt()
        if (requiredTopMargin > defaultTopMargin && requiredTopMargin != pagesAdapter.topMargin) {
            pagesAdapter.topMargin = requiredTopMargin
            shouldUpdate = true
        }

        if (shouldUpdate) binding.textReaderViewpager.adapter = pagesAdapter // Adapter remakes the items
    }

    private fun createFileReader(): DefaultZipFileReader? {
        val uri = IntentCompat.getParcelableExtra(requireActivity().intent, EPUB_URI, Uri::class.java)
        if (uri != null) {
            try {
                val stream = requireContext().contentResolver.openInputStream(uri)
                return DefaultZipFileReader(stream, fileManager)
            } catch (e: Exception) {
                Timber.e(e, "Couldn't open URI stream")
            }
        }
        return null
    }

    private fun showTableOfContents(navPoints: List<NavPoint>){

        val filePaths = navPoints.map { it.getContentWithoutTag() }
        val titles = navPoints.map { it.navLabel }

        val adapter = ArrayAdapter(requireContext(), R.layout.dialog_item, titles)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(resources.getString(R.string.table_of_contents))
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setAdapter(adapter) { _, i ->
                val path = filePaths[i]
                viewModel.jumpToChapter(path)
            }
            .create()
        dialog.show()
    }

    private fun updateCurrentChapterLabel(){
        binding.readerCurrentChapter.text = resources.getString(R.string.reader_current_chapter_label, viewModel.currentChapter + 1, viewModel.spineSize)
    }

    private fun showTextDialog(text: CharSequence){
        dialog = TextInfoDialog.newInstance(
            text.toString(),
            TextInfoDialog.NO_SERVICE,
            null,
            brightnessTheme.value
        )
        dialog?.show(childFragmentManager, TextInfoDialog.TAG)
    }

    private fun handleUiDialogState(result: VisualizeTextViewModel.UiDialogState) {
        if (!result.isLoading) {
            when(val state = result.dialogState) {
                is DialogType.Note -> {
                    noteInfo.value = state.item.toEditNote()
                    noteSheetVisible.value = true
                }
                is DialogType.SavedWord -> noteSheetVisible.value = false
                is DialogType.Translation -> {
                    noteSheetVisible.value = false
                    pagesAdapter.selectText(state.span)
                }
                null -> {
                    noteSheetVisible.value = false
                    dialog?.dismiss()
                    dialog = null
                    pagesAdapter.unselectText()
                }
            }
        }
    }

    inner class PinchListener(private val textCardView: View): ScaleGestureDetector.OnScaleGestureListener{

        private var pinchDetected = false

        private val screenWidth = resources.displayMetrics.widthPixels

        /**
         * The inverse of the ratio between the widths of the card and the screen.
         * This is the ratio/scale the card should have when fully expanded (max scale).
         */
        private var invRatio = 1f
        private val minScale = 1f
        private var scale = 1f

        private var constantTerm = 0f

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            viewPager.isUserInputEnabled = false
            pinchDetected = false
            invRatio = 1f / ratio
            scale = textCardView.scaleX
            constantTerm = (cardYOffset / (invRatio - minScale))
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            viewPager.isUserInputEnabled = true

            val factor = detector.scaleFactor

            // Check if it should toggle off full screen mode
            if(viewModel.fullScreen && factor < 1.0f){
                val lastWidth = screenWidth * factor
                val middleWidth = cardWidth + (screenWidth - cardWidth) / 2f
                if(lastWidth < middleWidth){
                    toggleImmersiveMode()
                    scale = 1f
                }
            }

            textCardView.scaleX = scale
            textCardView.scaleY = scale
            if (!viewModel.fullScreen) textCardView.translationY = 0f
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {

            if(!pinchDetected){

                val factor = detector.scaleFactor
                val newScale = scale * factor

                // Avoid making the card smaller than the original size
                if (newScale >= minScale) {
                    textCardView.scaleX = newScale
                    textCardView.scaleY = newScale
                    // To calculate the new Y offset, using cross-multiplication: newScale / invRatio = newYOffset / cardYOffset
                    // To normalize the scale/ratio to start from 0 , the min ratio is subtracted, therefore solving for newYOffset yields:
                    // newYOffset = (newScale - minScale) * (cardYOffset / (invRatio - minScale))
                    val newYOffset = (newScale - minScale) * constantTerm
                    if (newScale <= invRatio) textCardView.translationY = newYOffset
                } else {
                    textCardView.scaleX = minScale
                    textCardView.scaleY = minScale
                    textCardView.translationY = 0f
                }

                val fullScreen = viewModel.fullScreen

                if((detector.scaleFactor > PINCH_UPPER_LIMIT || newScale >= invRatio) && !fullScreen){
                    toggleImmersiveMode()
                    pinchDetected = true
                    scale = invRatio
                    textCardView.scaleX = invRatio
                    textCardView.scaleY = invRatio
                    textCardView.translationY = cardYOffset
                    return true
                }
            }

            return false
        }
    }

    private fun toggleImmersiveMode() {
        val position = viewModel.currentPage

        viewModel.fullScreen = !viewModel.fullScreen

        if(viewModel.fullScreen){
            hideSystemUi()
        }else{
            val window = requireActivity().window
            val controllerCompat = WindowCompat.getInsetsController(window, window.decorView)
            controllerCompat.show(WindowInsetsCompat.Type.systemBars())
            activity?.actionBar?.show()
        }

        viewPager.post { setBottomSheetPeekHeight() }

        viewPager.adapter = pagesAdapter
        viewPager.setCurrentItem(position, false)

    }

    private fun hideSystemUi(){
        val window = requireActivity().window
        val controllerCompat = WindowCompat.getInsetsController(window, window.decorView)
        controllerCompat.hide(WindowInsetsCompat.Type.systemBars())
        controllerCompat.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        activity?.actionBar?.hide()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setBottomSheetCallbacks(){
        binding.pageTranslateBtn.setOnClickListener {
            val position = viewPager.currentItem
            viewModel.translatePage(position)

            setFullBottomSheet(true)
        }

        binding.arrowBtn.setOnClickListener {
            val isFull = pagesAdapter.isPageSplit
            setFullBottomSheet(!isFull)
        }

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

            override fun onSlide(bottomSheet: View, slideOffset: Float) { binding.arrowBtn.rotation = slideOffset * 180 }

            override fun onStateChanged(bottomSheet: View, newState: Int) {

                when(newState){
                    BottomSheetBehavior.STATE_EXPANDED -> setSplitMode(true)
                    BottomSheetBehavior.STATE_COLLAPSED -> setSplitMode(false)
                    else -> {}
                }
            }

            private fun setSplitMode(isSplit: Boolean){
                pagesAdapter.notifyItemRangeChanged(0, pagesAdapter.itemCount, isSplit)
                viewModel.isSheetExpanded = isSplit
                pagesAdapter.isPageSplit = isSplit
            }
        })

        val metrics = resources.displayMetrics
        val cardHalfHeight = (metrics.heightPixels * ratio / 2f).toInt()

        var startX = 0f
        var startY = 0f
        val radius = 40f
        // To detect the click, onTouchListener is used to get the touch coordinates and because onClickListener consumes the touch event
        binding.pageBottomTextView.setOnTouchListener { _, event ->
            val duration = event.eventTime - event.downTime

            when(event.action){
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                }
                MotionEvent.ACTION_UP -> {
                    if(duration < 300
                        && abs(event.x - startX) < radius && abs(event.y - startY) < radius) {
                        val index = binding.pageBottomTextView.getOffsetForPosition(event.x, event.y)
                        val splitSpans = viewModel.findSelectedSentence(viewPager.currentItem, index) ?: return@setOnTouchListener true
                        highlightSpans(splitSpans)
                    }
                }
            }

            // Need to dispatch the touch event to the ViewPager otherwise scrolling won't work on the bottom text
            // The y position of the touch event is in terms of the bottom text origin, add offset to make it in terms of the card view origin
            val newEvent = MotionEvent.obtain(event.downTime, event.eventTime, event.action, event.x, event.y + cardHalfHeight, event.metaState)
            try{
                viewPager.dispatchTouchEvent(newEvent)
            }catch (exception: IllegalArgumentException){
                // Sometimes an exception will be thrown "pointerIndex out of range".
                // Ignoring it seems to not affect the scaling gesture
                Timber.e("Dispatching touch event to ViewPager error: ${exception.message}")
            }
            true
        }
    }

    private fun setFullBottomSheet(full: Boolean){
        bottomSheetBehavior.state = if(full) BottomSheetBehavior.STATE_EXPANDED else BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun setBottomSheetPeekHeight(){
        val peekHeight = sheetBarHeight + viewPager.height / 2
        bottomSheetBehavior.peekHeight = peekHeight
    }

    private fun highlightSpans(pageSpans: SplitPageSpan) {
        val text = SpannableString(binding.pageBottomTextView.text)

        //Remove previous
        text.getSpans(0, text.length, BackgroundColorSpan::class.java).forEach { span -> text.removeSpan(span) }

        text.setSpan(BackgroundColorSpan(0x6633B5E5), pageSpans.bottomSpan.start, pageSpans.bottomSpan.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.pageBottomTextView.setText(text, TextView.BufferType.SPANNABLE)

        // Notify the adapter to set the highlight, send span payload
        pagesAdapter.notifyItemChanged(viewPager.currentItem, pageSpans.topSpan)
    }

    /**
     * First tries to get the text coming from the imported screen.
     * If null then it tries to get the text from the ClipData of the sharing intent (Intent.ACTION_SEND).
     */
    private fun getIntentText(): String {
        val intent = requireActivity().intent
        val extras = intent.extras
        return extras?.getString(IMPORTED_TEXT) ?: return intent.getStringExtra(Intent.EXTRA_TEXT) ?: "No text"
    }

    private fun splitByWords(text: String): List<String> {
        val words = arrayListOf<String>()
        val iterator = BreakIterator.getWordInstance()
        iterator.setText(text)
        var start = iterator.first()
        var end = iterator.next()

        while (end != BreakIterator.DONE) {
            val possibleWord = text.substring(start, end)
            if (possibleWord.isNotBlank()) words.add(possibleWord)
            start = end
            end = iterator.next()
        }
        return words
    }

    @Composable
    fun Dialogs() {

        ExternalLinksDialog(
            isShown = linksDialogShown.value,
            links = wordLinks.value,
            selection = selectedLinkPos.intValue,
            onItemClick = viewModel::setWordLink,
            onDismiss = viewModel::hideWordLinks,
        )

        var addNoteVisible by remember { addNoteDialogVisible }

        AddNoteDialog(
            addNoteVisible,
            onDismiss =  viewModel::stopEditing,
            onDelete = viewModel::deleteCurrentNote,
            onSaveClicked = {
                viewModel.saveCurrentNote(it.text, it.colorHex)
            },
        )
        
        if (pickInfoDialogVisible.value) {
            DialogList(
                list = listOf(resources.getString(R.string.note), resources.getString(R.string.saved_word)),
                title = resources.getString(R.string.pick_info_dialog_title),
                onItemSelected = { index, _ ->
                    when (index) {
                        0 -> viewModel.pickItem(true)
                        1 -> showTextDialog(clickedWord)
                    }
                    viewModel.stopPickingInfo()
                },
                onDismiss = viewModel::stopPickingInfo
            )
        }

        if (contentsMenuVisible.value) {
            val navPoints = viewModel.getBook()?.tableOfContents?.navPoints
            val hasToC = navPoints?.isNotEmpty() ?: false
            ContentMenu(
                hasToC,
                { contentsMenuVisible.value = false }
            ) { option ->
                when(option) {
                    ContentMenuItem.TABLE_OF_CONTENTS -> navPoints?.let { showTableOfContents(it) }
                    ContentMenuItem.NOTES -> {
                        val id = viewModel.getFileId() ?: return@ContentMenu
                        findNavController().navigate(ImporttextDirections.toNotesListFragment(id, NoteType.FILE))
                    }
                }
                contentsMenuVisible.value = false
            }
        }
    }

    @Composable
    fun Sheet() {
        val noteInfo by remember { noteInfo }
        var noteSheetVisible by remember { noteSheetVisible }
        callback.isEnabled = noteSheetVisible

        NoteSheet(
            noteSheetVisible,
            { noteInfo?.noteText ?: "" },
            infoButtonVisibility = {
                val noteSpanText = noteInfo?.text
                val isWord = noteSpanText != null && noteSpanText.split(" ").size == 1
                return@NoteSheet viewModel.languageFrom != "auto" && isWord
            },
            onEditClicked = viewModel::startEditing,
            onInfoClicked = {
                val word = noteInfo?.text ?: return@NoteSheet
                viewModel.getExternalLinks(word)
            },
            onDismiss = {
                viewModel.hideDialog()
            }
        )
    }

    companion object{
        const val IMPORTED_TEXT = "imported_text"
        const val EPUB_URI = "epub_uri"

        const val SHOW_EPUB = "epub"
        const val FILE_ID = "fileId"

        const val PINCH_UPPER_LIMIT = 1.15f
    }

}