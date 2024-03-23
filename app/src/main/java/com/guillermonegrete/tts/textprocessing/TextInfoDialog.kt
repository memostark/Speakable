package com.guillermonegrete.tts.textprocessing

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.distinctUntilChanged
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.common.compose.YesNoDialog
import com.guillermonegrete.tts.common.models.Span
import com.guillermonegrete.tts.customviews.ButtonsPreference
import com.guillermonegrete.tts.data.Translation
import com.guillermonegrete.tts.data.WordResult
import com.guillermonegrete.tts.databinding.DialogFragmentWordBinding
import com.guillermonegrete.tts.db.ExternalLink
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.importtext.visualize.model.SplitPageSpan
import com.guillermonegrete.tts.main.SettingsFragment
import com.guillermonegrete.tts.savedwords.ResultType
import com.guillermonegrete.tts.savedwords.SaveWordDialogFragment
import com.guillermonegrete.tts.savedwords.SaveWordDialogFragment.TAG_DIALOG_UPDATE_WORD
import com.guillermonegrete.tts.savedwords.SaveWordDialogViewModel
import com.guillermonegrete.tts.services.ScreenTextService
import com.guillermonegrete.tts.services.ScreenTextService.NO_FLOATING_ICON_SERVICE
import com.guillermonegrete.tts.textprocessing.domain.model.GetLayoutResult
import com.guillermonegrete.tts.textprocessing.domain.model.StatusTTS
import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem
import com.guillermonegrete.tts.ui.BrightnessTheme
import com.guillermonegrete.tts.ui.DifferentValuesAdapter
import com.guillermonegrete.tts.ui.theme.AppTheme
import com.guillermonegrete.tts.utils.dpToPixel
import com.guillermonegrete.tts.utils.findWord
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sqrt

@AndroidEntryPoint
class TextInfoDialog: DialogFragment(), ProcessTextContract.View, SaveWordDialogFragment.Callback {

    private var window: Window? = null

    private var dictionaryAdapter: WiktionaryAdapter? = null

    private var inputText: String? = null

    private lateinit var mFoundWords: Words
    private var dbWord: Words? = null

    @Inject
    internal lateinit var presenter: ProcessTextContract.Presenter
    private val saveWordViewModel: SaveWordDialogViewModel by viewModels()

    private  var _bindingWord: DialogFragmentWordBinding? = null
    private val bindingWord get() = _bindingWord!!

    private var pager: ViewPager2? = null
    private var pagerAdapter: MyPageAdapter? = null

    private lateinit var playButton: ImageButton
    private lateinit var playProgressBar: ProgressBar

    private val translatedText = mutableStateOf("")
    private val isLoadingTTS = mutableStateOf(false)
    private val isPlaying = mutableStateOf(false)
    private val isTTSAvailable = mutableStateOf(true)
    private val detectedLanguage = mutableStateOf<Int?>(null)
    private val selectedSpans = mutableStateOf<SplitPageSpan?>(null)
    private val wordState = mutableStateOf(WordState())

    private val wordLinks = mutableStateOf(emptyList<ExternalLink>())
    private val selectedLink = mutableIntStateOf(0)

    private val editDialogShown = mutableStateOf(false)
    private val deleteDialogShown = mutableStateOf(false)
    private var linksDialogShown = mutableStateOf(false)

    private var selectedWordSpan = Span(0, 0)

    @Inject
    internal lateinit var preferences: SharedPreferences

    /**
     * List of languages without auto-detect entry.
     */
    private var languages: List<String> = listOf()
    private var languagesISO: List<String> = listOf()

    private var languageFromIndex: Int = -1
    private var languagePreferenceIndex: Int = 0
    private var languageFrom: String? = null
    private var languageToISO: String? = null

    private var yAxis = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setBrightnessTheme()

        inputText = arguments?.getString(TEXT_KEY)?.trim()

        languageToISO = getPreferenceISO()
        languageFrom = getLanguageFromPreference()

        presenter.setView(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        window = dialog.window
        window?.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        keepImmersiveMode()

        val text = inputText ?: ""

        val splitText = text.split(" ")

        if(splitText.size > 1) {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            val languagesFrom = resources.getStringArray(R.array.googleTranslateLangsWithAutoArray).toList()
            languages = resources.getStringArray(R.array.googleTranslateLanguagesArray).toList()
            return ComposeView(requireContext()).apply {
                setContent {

                    AppTheme {
                        SentenceDialog(
                            isVisible = isVisible,
                            text = text,
                            translation = translatedText.value,
                            languagesFrom = languagesFrom,
                            languagesTo = languages,
                            targetLangIndex = languagePreferenceIndex,
                            isPlaying = isPlaying.value,
                            isLoading = isLoadingTTS.value,
                            isTTSAvailable = isTTSAvailable.value,
                            sourceLangIndex = languageFromIndex,
                            detectedLanguageIndex = detectedLanguage.value,
                            highlightedSpan = selectedSpans.value,
                            wordState = wordState.value,
                            onPlayButtonClick = { onPlayButtonClick(text) },
                            onTopTextClick = { findWord(it) },
                            onBottomTextClick = { findSelectedSentence(it) },
                            onBookmarkClicked = { editDialogShown.value = true },
                            onMoreInfoClicked = { onMoreInfoClicked() },
                            onSourceLangChanged = { updateLanguageFrom(it) },
                            onTargetLangChanged = { updateLanguageTo(it) },
                            onDismiss = { dismiss() },
                        )


                        val state = wordState.value
                        val word = state.word

                        if (word != null) {
                            EditWordDialog(
                                isShown = editDialogShown.value,
                                word = word.word,
                                language = word.lang,
                                translation = word.definition,
                                notes = word.notes,
                                languages = languages,
                                languagesISO = languagesISO,
                                isSaved = state.isSaved,
                                onSave = {
                                    if(state.isSaved) saveWordViewModel.update(it) else saveWordViewModel.save(it)
                                },
                                onDelete = { deleteDialogShown.value = true },
                                onDismiss = { editDialogShown.value = false }
                            )
                        }

                        if (deleteDialogShown.value && word != null) {

                            YesNoDialog(
                                onDismissRequest = { deleteDialogShown.value = false },
                                onConfirmation = { presenter.onClickDeleteWord(word.word) },
                                dialogTitle = getString(R.string.delete_word_message),
                                dialogText = getString(R.string.delete_word_message),
                            )
                        }

                        ExternalLinksDialog(
                            isShown = linksDialogShown.value,
                            links = wordLinks.value,
                            selection = selectedLink.intValue,
                            onItemClick = { selectedLink.intValue = it },
                            onDismiss = { linksDialogShown.value = false },
                        )
                    }
                }
            }
        }

        _bindingWord = DialogFragmentWordBinding.inflate(inflater, container, false)

        bindingWord.textTts.text = text

        setLanguageFromSpinner(bindingWord.spinnerLanguageFrom)
        setPlayButton(text)
        bindingWord.textLanguageCode.visibility = if (languageFrom == "auto") View.VISIBLE else View.GONE

        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        return bindingWord.root
    }

    private fun onMoreInfoClicked() {
        val word = wordState.value.word ?: return
        (presenter as ProcessTextPresenter).getExternalLinks(word)
    }

    private fun findWord(offset: Int) {
        // If true the selected word was tapped, unselect
        if (selectedWordSpan.inside(offset)) {
            selectedWordSpan = Span(0, 0)
            wordState.value = WordState()
            return
        }

        val text = inputText ?: return

        val span = text.findWord(offset)
        selectedWordSpan = span
        val word = text.substring(span.start, span.end)
        val detectedIndex = detectedLanguage.value
        val language = if (languageFromIndex == 0 && detectedIndex != null) {
            languagesISO.getOrNull(detectedIndex) ?: languageFrom
        } else languageFrom
        (presenter as ProcessTextPresenter).setSelectedWord(word, language, languageToISO)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (_bindingWord != null) setSwipeListener()

        val presenterImp = (presenter as ProcessTextPresenter)
        presenterImp.layoutResult.observe(viewLifecycleOwner){
            onLayoutResult(it)
        }

        presenterImp.statusTTS.observe(viewLifecycleOwner){
            val available = when(it) {
                StatusTTS.LanguageReady -> true
                StatusTTS.Unavailable -> false
            }
            isLoadingTTS.value = false
            isTTSAvailable.value = available
        }

        val extraWord: Words? = arguments?.getParcelable(WORD_KEY)
        if(extraWord != null){
            val isSaved = requireArguments().getBoolean(WORD_SAVED_KEY)
            presenter.getDictionaryEntry(extraWord, isSaved)
        }else{
            val action = arguments?.getString(ACTION_KEY)
            if(NO_SERVICE == action){
                presenter.getLayout(inputText, languageFrom, languageToISO)
            }else{
                presenter.startWithService(inputText, languageFrom, languageToISO)
            }
        }

        presenter.wordStream(inputText, languageFrom).distinctUntilChanged().observe(this) {
            dbWord = it
        }

        presenterImp.wordInfo().observe(this) {result ->
            when(result) {
                is WordResult.Local -> wordState.value = WordState(result.word,true, selectedWordSpan)
                is WordResult.Remote -> wordState.value = WordState(Words(result.translation.originalText, result.translation.src, result.translation.translatedText), false, selectedWordSpan)
                is WordResult.Error -> { Toast.makeText(context, "Error: ${result.exception}", Toast.LENGTH_SHORT).show() }
            }
        }

        presenterImp.wordLinks.observe(this) { links ->
            wordLinks.value = links
            // If out of index, default to the first item
            if(selectedLink.intValue >= links.size) selectedLink.intValue = 0
            linksDialogShown.value = true
        }

        saveWordViewModel.update.observe(this) {result ->
            when(result) {
                is ResultType.Insert -> {
                    editDialogShown.value = false
                    wordState.value = wordState.value.copy(word = result.word, isSaved = true)
                }
                ResultType.Update -> editDialogShown.value = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.start()
    }

    override fun onStop() {
        super.onStop()
        presenter.stop()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        presenter.destroy()
        val parent = activity
        if(parent is DialogInterface.OnDismissListener) parent.onDismiss(dialog)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _bindingWord = null
    }

    private fun getPreferenceISO(): String {
        val englishIndex = 15
        languagePreferenceIndex = preferences.getInt(LANGUAGE_PREFERENCE, englishIndex)
        languagesISO = resources.getStringArray(R.array.googleTranslateLanguagesValue).toList()
        return languagesISO[languagePreferenceIndex]
    }

    private fun getLanguageFromPreference(): String {
        // Give highest priority to the language set in the arguments, if null use the preference language
        val setLang = arguments?.getString(LANG_FROM_KEY)
        val preference = setLang ?: preferences.getString(SettingsFragment.PREF_LANGUAGE_FROM, "auto") ?: "auto"
        languageFromIndex = languagesISO.indexOf(preference)
        languageFromIndex++ // Increment because the list we searched is missing one element "auto"
        return preference
    }

    /**
     * In case the parent activity has immersive mode, this code prevents the dialog from interrupting
     * by removing focus before showing the window. More info: https://stackoverflow.com/a/24549869/10244759
     */
    private fun keepImmersiveMode(){
        window?.let {w ->
            // Remove focus
            w.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

            // Copy parent activity window configuration
            activity?.window?.decorView?.systemUiVisibility?.let {
                w.decorView.systemUiVisibility = it
            }

            // Restore focus after showing dialog
            dialog?.setOnShowListener {
                w.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

                val wm = activity?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                wm?.updateViewLayout(w.decorView, w.attributes)
            }
        }
    }

    private fun setWordLayout(word: Words) {
        bindingWord.textLanguageCode.text = word.lang

        bindingWord.saveIcon.setOnClickListener { showSaveDialog(mFoundWords) }
    }

    private fun setWiktionaryLayout(word: Words, items: List<WikiItem>) {
        val isLargeWindow = preferences.getBoolean(SettingsFragment.PREF_WINDOW_SIZE, ButtonsPreference.DEFAULT_VALUE)
        if (isLargeWindow) setCenterDialog() else setBottomDialog()
        dictionaryAdapter = WiktionaryAdapter(items)

        mFoundWords = word
        setWordLayout(word)

        if (isLargeWindow) createViewPager() else createSmallViewPager()
    }


    override fun setSavedWordLayout(word: Words) {
        setBottomDialog()
        mFoundWords = word

//        setWordLayout(word)
        createSmallViewPager()

        setSavedWordToolbar()
        languagePreferenceIndex = -1 // Indicates spinner not visible

    }

    override fun setDictWithSaveWordLayout(word: Words, items: List<WikiItem>) {
        setWiktionaryLayout(word, items)
        setSavedWordToolbar()
        languagePreferenceIndex = -1 // Indicates spinner not visible
    }

    override fun showTranslationError(error: String) {
        Toast.makeText(context, "No translation found: $error", Toast.LENGTH_SHORT).show()
    }

    private fun setTranslationLayout(word: Words) {
        setBottomDialog()
        mFoundWords = word
        setWordLayout(word)
        createSmallViewPager()
    }

    private fun setSentenceLayout(translation: Translation) {
        translatedText.value = translation.translatedText
        updateDetectedLanguage(translation.src)
    }

    private fun updateDetectedLanguage(langISO: String) {
        if (languageFromIndex == 0) {
            val index = languagesISO.indexOf(langISO)
            if (index != -1) detectedLanguage.value = index
        }
    }

    override fun setExternalDictionary(links: List<ExternalLink>) {
        if(!isAdded) return

        val pagerAdapter = MyPageAdapter(this)
        if (dictionaryAdapter != null) pagerAdapter.addFragment(
            DefinitionFragment.newInstance(dictionaryAdapter)
        )
        val translationFragment = TranslationFragment.newInstance(mFoundWords, languagePreferenceIndex)
        translationFragment.setListener(translationFragListener)
        pagerAdapter.addFragment(translationFragment)
        pagerAdapter.addFragment(
            ExternalLinksFragment.newInstance(inputText, links as ArrayList<ExternalLink>)
        )

        pager?.let {
            it.adapter = pagerAdapter
            TabLayoutMediator(bindingWord.pagerMenuDots, it, true) { _, _ -> }.attach() // Tab without text
        }

        this.pagerAdapter = pagerAdapter
    }

    private fun onLayoutResult(result: GetLayoutResult) {
        when(result){
            is GetLayoutResult.WordSuccess -> {
                when(result.type){
                    ProcessTextLayoutType.WORD_TRANSLATION -> setTranslationLayout(result.word)
                    ProcessTextLayoutType.SAVED_WORD -> setSavedWordLayout(result.word)
                    else -> Timber.e( "Unknown layout type: ${result.type}")
                }
            }
            is GetLayoutResult.Sentence -> setSentenceLayout(result.translation)
            is GetLayoutResult.DictionarySuccess ->
                if(requireArguments().getBoolean(WORD_SAVED_KEY)) setDictWithSaveWordLayout(result.word, result.items) else setWiktionaryLayout(result.word, result.items)
            is GetLayoutResult.Error -> {
                Timber.e(result.exception, "Error getting the layout")
                showTranslationError(result.exception.message ?: result.exception.toString())
            }
        }
    }

    override fun setTranslationErrorMessage() {
        // If pager is not null, means we are using word layout,
        // otherwise is sentence layout
        pagerAdapter?.let {
            // Check if the adapter has dictionary fragment
            val index: Int = if (dictionaryAdapter != null && it.itemCount == 3) 1 else 0

            val fragment = it.fragments[index]
            if (fragment is TranslationFragment) fragment.setErrorLayout()
        }
    }

    override fun showSaveDialog(word: Words) {
        val dialogFragment = SaveWordDialogFragment.newInstance(dbWord ?: mFoundWords)
        dialogFragment.show(childFragmentManager, "New word process")
    }

    override fun showDeleteDialog(word: String) {

        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage(getString(R.string.delete_word_message))
            .setPositiveButton(R.string.yes) { dialog, _ ->
                presenter.onClickDeleteWord(word)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }

        builder.create().show()
    }

    override fun showWordDeleted() {
        if (_bindingWord != null) {
            bindingWord.editIcon.visibility = View.GONE
            bindingWord.saveIcon.setImageResource(R.drawable.ic_bookmark_border_black_24dp)
            bindingWord.saveIcon.setOnClickListener { showSaveDialog(mFoundWords) }
        } else {
            val oldWord = wordState.value.word ?: return
            val word = Words(oldWord.word, oldWord.lang, oldWord.definition) // Deleted word has same values but no id and notes
            deleteDialogShown.value = false
            editDialogShown.value = false
            wordState.value = wordState.value.copy(word = word, isSaved = false)
        }
    }

    override fun showErrorPlayingAudio() {
        showPlayIcon()
        Toast.makeText(context, "Could not play audio", Toast.LENGTH_SHORT).show()
    }

    override fun startService() {
        val intentService = Intent(context, ScreenTextService::class.java)
        intentService.action = NO_FLOATING_ICON_SERVICE
        context?.startService(intentService)
    }

    override fun showLanguageNotAvailable() {
        _bindingWord ?: return
        playButton.setOnClickListener {
            Toast.makeText(context, "Language not available for TTS", Toast.LENGTH_SHORT).show()
        }
        playButton.setImageResource(R.drawable.baseline_volume_off_24)
        playProgressBar.visibility = View.INVISIBLE
        playButton.visibility = View.VISIBLE
    }

    override fun showLoadingTTS() {
        if (_bindingWord != null) {
            playProgressBar.visibility = View.VISIBLE
            playButton.visibility = View.INVISIBLE
        } else {
            isLoadingTTS.value = true
        }
    }

    override fun showPlayIcon() {
        if (_bindingWord != null) {
            playButton.setImageResource(R.drawable.ic_volume_up_black_24dp)
            playProgressBar.visibility = View.INVISIBLE
            playButton.visibility = View.VISIBLE
        } else {
            isLoadingTTS.value = false
            isPlaying.value = false
        }
    }

    override fun showStopIcon() {
        if (_bindingWord != null) {
            playButton.setImageResource(R.drawable.ic_stop_black_24dp)
            playProgressBar.visibility = View.INVISIBLE
            playButton.visibility = View.VISIBLE
        } else {
            isLoadingTTS.value = false
            isPlaying.value = true
        }
    }

    override fun updateTranslation(translation: Translation) {

        // If pager is not null, means we are using activity_processtext layout,
        // otherwise is sentence layout
        if (pager != null) {
            bindingWord.textLanguageCode.visibility = if (languageFrom == "auto") View.VISIBLE else  View.GONE
            val fragIndex = if (dictionaryAdapter != null && pagerAdapter?.itemCount == 3) 1 else 0

            val fragment = pagerAdapter?.fragments?.get(fragIndex)
            val word = Words(inputText ?: "", translation.src, translation.translatedText)
            if (fragment is TranslationFragment) fragment.updateTranslation(word)
        } else {
            selectedSpans.value = null
            translatedText.value = translation.translatedText
            updateDetectedLanguage(translation.src)
        }
    }

    override fun updateExternalLinks(links: MutableList<ExternalLink>?) {

        pagerAdapter?.let {
            val index: Int = if (dictionaryAdapter != null && it.itemCount == 3) 2 else 1

            val fragment = it.fragments[index]
            if (fragment is ExternalLinksFragment) fragment.setExternalLinks(links)
        }
    }

    override fun setPresenter(presenter: ProcessTextContract.Presenter) {
        this.presenter = presenter
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setSwipeListener() {
        val card = bindingWord.textDialogCard

        val params = dialog?.window?.attributes

        var downActionX = 0.0f
        var downActionY = 0.0f

        var iParamX = 0
        var iParamY = 0

        var initialScreenY = 0

        var directionSet = false
        var horizontalAxis = true

        // pixels
        val minDismissDistance = 300
        val radioThreshold = 8

        card.setOnTouchListener { _, event ->
            params ?: return@setOnTouchListener false

            when(event.actionMasked){
                MotionEvent.ACTION_DOWN ->{
                    iParamX = params.x
                    iParamY = params.y

                    downActionX = event.rawX
                    downActionY = event.rawY

                    val location = intArrayOf(0, 0)
                    window?.decorView?.getLocationOnScreen(location)
                    initialScreenY = location[1]
                }
                MotionEvent.ACTION_MOVE ->{

                    val dRawX = event.rawX - downActionX
                    val dRawY = (event.rawY - downActionY)

                    if(!directionSet){

                        // Only set direction after a minimum pixel movement
                        if(sqrt(dRawX * dRawX + dRawY * dRawY) < radioThreshold) return@setOnTouchListener false
                        horizontalAxis = abs(dRawX) >= abs(dRawY)
                        directionSet = true
                    }

                    if(horizontalAxis){
                        params.x = iParamX + dRawX.toInt()
                    }else {
                        // Multiple by 1 or -1 because the y axis can be inverted when using Gravity.Bottom param.
                        params.y = iParamY + (dRawY * yAxis).toInt()
                        if(initialScreenY + dRawY.toInt() <= 0) return@setOnTouchListener false
                    }

                    dialog?.window?.attributes = params
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->{

                    if(abs(params.x - iParamX) > minDismissDistance){
                        dismiss()
                    }else {
                        params.x = iParamX
                        params.y = iParamY
                        dialog?.window?.attributes = params
                    }
                    directionSet = false
                }
            }
            true
        }
    }

    private fun setBrightnessTheme(){
        val theme = arguments?.getString(THEME_KEY) ?: return

        if(theme.isNotEmpty()) {

            val id = when (BrightnessTheme.get(theme)) {
                BrightnessTheme.WHITE -> R.style.ProcessTextStyle_White
                BrightnessTheme.BEIGE -> R.style.ProcessTextStyle_Beige
                BrightnessTheme.BLACK -> R.style.ProcessTextStyle_Dark
            }

            context?.theme?.applyStyle(id, true)
        }
    }

    private fun setCenterDialog() {
        window?.let {
            val wlp = it.attributes
            wlp.flags =
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            it.attributes = wlp
        }
    }

    private fun setBottomDialog() {

        window?.let {

            yAxis = -1

            val wlp = it.attributes
            wlp.dimAmount = 0f
            wlp.flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            wlp.y = requireContext().dpToPixel(40)
            wlp.gravity = Gravity.BOTTOM
            it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            it.attributes = wlp
        }
    }

    private fun setSavedWordToolbar() {
        bindingWord.saveIcon.setImageResource(R.drawable.ic_bookmark_black_24dp)
        bindingWord.saveIcon.setOnClickListener { showDeleteDialog(mFoundWords.word) }

        with(bindingWord.editIcon) {
            visibility = View.VISIBLE
            setOnClickListener {
                val dialogFragment = SaveWordDialogFragment.newInstance(dbWord ?: mFoundWords)
                dialogFragment.show(childFragmentManager, TAG_DIALOG_UPDATE_WORD)
            }
        }

        // Hides language from spinner, because language is already predefined.
        bindingWord.spinnerLanguageFrom.visibility = View.INVISIBLE
        bindingWord.textLanguageCode.text = mFoundWords.lang
        bindingWord.textLanguageCode.visibility = View.VISIBLE
    }

    private fun setPlayButton(text: String) {
        playButton = bindingWord.playTtsIcon
        playButton.setOnClickListener { presenter.onClickReproduce(text) }

        playProgressBar = bindingWord.playLoadingIcon
    }

    private fun onPlayButtonClick(text: String) {
        if (isTTSAvailable.value) {
            presenter.onClickReproduce(text)
        } else {
            Toast.makeText(context, "Language not available for TTS", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createViewPager() {
        pager = bindingWord.processViewPager
    }

    private fun createSmallViewPager() {
        pager = bindingWord.processViewPager
        val params = pager?.layoutParams
        params?.height = requireContext().dpToPixel(150)
        pager?.layoutParams = params
    }

    private fun setLanguageFromSpinner(spinner: Spinner) {

        val adapter = DifferentValuesAdapter.createFromResource(
            requireContext(),
            R.array.googleTranslateLangsWithAutoValue,
            R.array.googleTranslateLangsWithAutoArray,
            R.layout.spinner_layout_end
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.setSelection(languageFromIndex, false)
        spinner.post { spinner.onItemSelectedListener = SpinnerListener() }
    }

    override fun onWordSaved(word: Words) {
        setSavedWordToolbar()
    }

    /**
     * Listener for translation fragment when using ViewPager
     */
    private val translationFragListener = object : TranslationFragment.Listener {
        override fun onItemSelected(position: Int) {
            languageToISO = languagesISO[position]
            val editor = preferences.edit()
            editor.putInt(LANGUAGE_PREFERENCE, position)
            editor.apply()
            presenter.onLanguageSpinnerChange(languageFrom, languageToISO)
        }
    }

    /**
     * Listener when layout is for a sentence
     */
    internal inner class SpinnerListener : AdapterView.OnItemSelectedListener {

        override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {

            val editor = preferences.edit()
            when (parent.id) {
                R.id.spinner_language_from -> {
                    languageFrom = if (position == 0)
                        "auto"
                    else
                        languagesISO[position - 1]
                    editor.putString(SettingsFragment.PREF_LANGUAGE_FROM, languageFrom)
                    editor.apply()
                    presenter.onLanguageSpinnerChange(languageFrom, languageToISO)
                }
                else -> {}
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>) {}
    }

    private fun updateLanguageFrom(position: Int) {
        languageFromIndex = position
        languageFrom = if (position == 0)
            "auto"
        else
            languagesISO[position - 1]
        val editor = preferences.edit()
        editor.putString(SettingsFragment.PREF_LANGUAGE_FROM, languageFrom)
        editor.apply()
        presenter.onLanguageSpinnerChange(languageFrom, languageToISO)
    }

    private fun updateLanguageTo(position: Int) {
        languageToISO = languagesISO[position]
        val editor = preferences.edit()
        editor.putInt(LANGUAGE_PREFERENCE, position)
        editor.apply()
        presenter.onLanguageSpinnerChange(languageFrom, languageToISO)
    }

    private fun findSelectedSentence(charIndex: Int) {
        val translation = (presenter as ProcessTextPresenter).currentTranslation ?: return

        var start = 0
        var originStart = 0

        for(sentence in translation.sentences){
            val end = start + sentence.trans.length
            val originalEnd = originStart + sentence.orig.length
            if(charIndex < end) {
                // indicate UI to highlight this sentence
                val spans = SplitPageSpan(Span(originStart, originalEnd), Span(start, end))
                selectedSpans.value = if (selectedSpans.value == spans) null else spans
                return
            }
            start = end
            originStart = originalEnd
        }
    }


    private inner class MyPageAdapter(fragment: Fragment) :
        FragmentStateAdapter(fragment) {

        val fragments = ArrayList<Fragment>()

        fun addFragment(fragment: Fragment) {
            fragments.add(fragment)
        }

        override fun getItemCount() = fragments.size

        override fun createFragment(position: Int) = fragments[position]
    }


    companion object{
        // Bundle keys
        const val TEXT_KEY = "text_key"
        const val WORD_KEY = "word_key"
        const val ACTION_KEY = "extra_key"
        const val THEME_KEY = "theme_key"
        const val LANG_FROM_KEY = "lang_from_key"
        const val WORD_SAVED_KEY = "word_saved_key"

        const val LANGUAGE_PREFERENCE = "ProcessTextLangPreference"
        const val NO_SERVICE = "no_service"

        @JvmStatic
        @JvmOverloads
        fun newInstance(
            text: String,
            action: String?,
            word: Words?,
            theme: String = "",
            languageFrom: String? = null,
            wordIsSaved: Boolean = false,
        ) = TextInfoDialog().apply {
            arguments = Bundle().apply {
                putString(TEXT_KEY, text)
                putString(ACTION_KEY, action)
                putParcelable(WORD_KEY, word)
                putString(THEME_KEY, theme)
                putString(LANG_FROM_KEY, languageFrom)
                putBoolean(WORD_SAVED_KEY, wordIsSaved)
            }
        }

        @JvmStatic
        fun newInstance(
            text: String,
            action: String?,
            word: Words?,
            isSaved: Boolean
        ) = newInstance(text, action, word, wordIsSaved = isSaved)
    }
}