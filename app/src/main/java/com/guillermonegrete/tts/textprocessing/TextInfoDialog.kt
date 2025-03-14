package com.guillermonegrete.tts.textprocessing

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.BundleCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.common.compose.ExternalLinkList
import com.guillermonegrete.tts.common.compose.ExternalLinksDialog
import com.guillermonegrete.tts.common.compose.LanguagesList
import com.guillermonegrete.tts.common.compose.StringList
import com.guillermonegrete.tts.common.compose.YesNoDialog
import com.guillermonegrete.tts.common.models.Span
import com.guillermonegrete.tts.common.models.WordUI
import com.guillermonegrete.tts.common.models.toUI
import com.guillermonegrete.tts.customviews.ButtonsPreference
import com.guillermonegrete.tts.data.Translation
import com.guillermonegrete.tts.data.WordResult
import com.guillermonegrete.tts.databinding.DialogFragmentWordBinding
import com.guillermonegrete.tts.db.ExternalLink
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.importtext.visualize.model.SplitPageSpan
import com.guillermonegrete.tts.main.SettingsFragment
import com.guillermonegrete.tts.savedwords.ResultType
import com.guillermonegrete.tts.savedwords.SaveWordDialogViewModel
import com.guillermonegrete.tts.services.ScreenTextService
import com.guillermonegrete.tts.services.ScreenTextService.NO_FLOATING_ICON_SERVICE
import com.guillermonegrete.tts.textprocessing.domain.model.GetLayoutResult
import com.guillermonegrete.tts.textprocessing.domain.model.StatusTTS
import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem
import com.guillermonegrete.tts.ui.BrightnessTheme
import com.guillermonegrete.tts.ui.DifferentValuesAdapter
import com.guillermonegrete.tts.ui.theme.VisualizerTheme
import com.guillermonegrete.tts.utils.dpToPixel
import com.guillermonegrete.tts.utils.findWord
import com.guillermonegrete.tts.utils.isNightMode
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sqrt

@AndroidEntryPoint
class TextInfoDialog: DialogFragment(), ProcessTextContract.View {

    private var window: Window? = null

    private var dictionaryAdapter: WiktionaryAdapter? = null

    private var inputText: String? = null

    private var mFoundWords: Words? = null
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
    private val playIconState = mutableStateOf(PlayIconState())
    private val detectedLanguage = mutableIntStateOf(-1)
    private val selectedSpans = mutableStateOf<SplitPageSpan?>(null)
    private val wordState = mutableStateOf<WordState?>(null)

    private val wordLinks = mutableStateOf(ExternalLinkList(emptyList()))
    private var selectedLink = 0

    private val editDialogShown = mutableStateOf(false)
    private val deleteDialogShown = mutableStateOf(false)
    private var linksDialogShown = mutableStateOf(false)

    private var selectedWordSpan = Span(0, 0)

    @Inject
    internal lateinit var preferences: SharedPreferences
    private lateinit var brightnessTheme: BrightnessTheme

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
        val back = ColorDrawable(Color.TRANSPARENT)
        val inset = InsetDrawable(back, requireContext().dpToPixel(20))
        window?.setBackgroundDrawable(inset)
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
            val languagesFrom = StringList(resources.getStringArray(R.array.googleTranslateLangsWithAutoArray).toList())
            languages = resources.getStringArray(R.array.googleTranslateLanguagesArray).toList()
            val languagesToStable = StringList(languages)
            return ComposeView(requireContext()).apply {
                setContent {

                    VisualizerTheme(brightnessTheme) {
                        SentenceDialog(
                            isVisible = isVisible,
                            text = text,
                            translation = translatedText,
                            languagesFrom = languagesFrom,
                            languagesTo = languagesToStable,
                            targetLangIndex = languagePreferenceIndex,
                            playIconState = playIconState,
                            sourceLangIndex = languageFromIndex,
                            detectedLanguageState = detectedLanguage,
                            highlightedSpanState = selectedSpans,
                            wordState = wordState,
                            onPlayButtonClick = { onPlayButtonClick(text) },
                            onTopTextClick = { findWord(it) },
                            onBottomTextClick = { findSelectedSentence(it) },
                            onBookmarkClicked = { editDialogShown.value = true },
                            onMoreInfoClicked = { onMoreInfoClicked() },
                            onSourceLangChanged = { updateLanguageFrom(it) },
                            onTargetLangChanged = { updateLanguageTo(it) },
                            onDismiss = { dismiss() },
                        )

                        Dialogs()

                        LocalExternalLinksDialog()
                    }
                }
            }
        }

        _bindingWord = DialogFragmentWordBinding.inflate(inflater, container, false)

        bindingWord.textTts.text = text

        setLanguageFromSpinner()
        setPlayButton(text)
        bindingWord.textLanguageCode.visibility = if (languageFrom == "auto") View.VISIBLE else View.GONE

        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        return bindingWord.root
    }

    private fun onMoreInfoClicked() {
        val wordUI = wordState.value?.word ?: return
        (presenter as ProcessTextPresenter).getExternalLinks(wordUI.toWord())
    }

    private fun findWord(offset: Int) {
        // If true the selected word was tapped, unselect
        if (selectedWordSpan.inside(offset)) {
            selectedWordSpan = Span(0, 0)
            wordState.value = null
            return
        }

        val text = inputText ?: return

        val span = text.findWord(offset)
        selectedWordSpan = span
        val word = text.substring(span.start, span.end)
        val detectedIndex = detectedLanguage.intValue
        val language = if (languageFromIndex == 0 && detectedIndex != -1) {
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
            playIconState.value = playIconState.value.copy(isLoading = false, isTTSAvailable = available)
        }

        val extraWord = BundleCompat.getParcelable(requireArguments(), WORD_KEY, Words::class.java)
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

        presenterImp.wordInfo().observe(this) {result ->
            when(result) {
                is WordResult.Local -> wordState.value = WordState(result.word.toUI(), result.word.id, selectedWordSpan)
                is WordResult.Remote -> wordState.value = WordState(WordUI(result.translation.originalText, result.translation.src, result.translation.translatedText), span = selectedWordSpan)
                is WordResult.Error -> {
                    Toast.makeText(context, "Couldn't load selected word", Toast.LENGTH_SHORT).show()
                    Timber.e(result.exception, "Couldn't load selected word info")
                }
            }
        }

        presenterImp.wordLinks.observe(this) { links ->
            wordLinks.value = ExternalLinkList(links.map(ExternalLink::toUI))
            // If out of index, default to the first item
            if(selectedLink >= links.size) selectedLink = 0
            linksDialogShown.value = true
        }

        saveWordViewModel.update.observe(this) {result ->
            when(result) {
                is ResultType.Insert -> {
                    editDialogShown.value = false
                    if (_bindingWord != null) {
                        setSavedWordToolbar(result.word)
                    }
                    updateDatabaseWord(result.word.id)
                    wordState.value = WordState(result.word.toUI(), result.word.id, wordState.value?.span)
                    dbWord = result.word
                }
                is ResultType.Update -> {
                    wordState.value = WordState(result.word.toUI(), result.word.id, wordState.value?.span)
                    dbWord = result.word
                    editDialogShown.value = false
                }
                is ResultType.Delete -> {}
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
        if (parent is DialogInterface.OnDismissListener) parent.onDismiss(dialog)
        val fragment = parentFragment
        if (fragment is DialogInterface.OnDismissListener) fragment.onDismiss(dialog)
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

        bindingWord.saveIcon.setOnClickListener { editDialogShown.value = true }
        bindingWord.composeRoot.setContent {
            VisualizerTheme(theme = brightnessTheme) {
                languages = resources.getStringArray(R.array.googleTranslateLanguagesArray).toList()
                wordState.value = WordState(word.toUI(), dbId = word.id)
                Dialogs()
            }
        }
    }

    private fun setDictRemoteWordLayout(word: Words, items: List<WikiItem>) {
        mFoundWords = word
        setWiktionaryLayout(word, items)
    }

    private fun setWiktionaryLayout(word: Words, items: List<WikiItem>) {
        val isLargeWindow = preferences.getBoolean(SettingsFragment.PREF_WINDOW_SIZE, ButtonsPreference.DEFAULT_VALUE)
        if (isLargeWindow) setCenterDialog() else setBottomDialog()
        dictionaryAdapter = WiktionaryAdapter(items)

        setWordLayout(word)

        if (isLargeWindow) createViewPager() else createSmallViewPager()
    }


    override fun setSavedWordLayout(word: Words) {
        setBottomDialog()
        dbWord = word
        createSmallViewPager()

        setSavedWordToolbar(word)
    }

    override fun setDictWithSaveWordLayout(word: Words, items: List<WikiItem>) {
        dbWord = word
        setWiktionaryLayout(word, items)
        setSavedWordToolbar(word)
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
            if (index != -1) detectedLanguage.intValue = index
        }
    }

    override fun setExternalDictionary(links: List<ExternalLink>) {
        if(!isAdded) return

        val pagerAdapter = MyPageAdapter(this)
        if (dictionaryAdapter != null) pagerAdapter.addFragment(
            DefinitionFragment.newInstance(dictionaryAdapter)
        )
        val word = dbWord ?: mFoundWords
        if (word != null) {
            val translationFragment = TranslationFragment.newInstance(word, languagePreferenceIndex)
            translationFragment.setListener(translationFragListener)
            pagerAdapter.addFragment(translationFragment)
        }
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
                if(requireArguments().getBoolean(WORD_SAVED_KEY)) setDictWithSaveWordLayout(result.word, result.items) else setDictRemoteWordLayout(result.word, result.items)
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

    override fun showWordDeleted() {
        if (_bindingWord != null) {
            bindingWord.saveIcon.setImageResource(R.drawable.ic_bookmark_border_black_24dp)
        }
        dbWord = null
        val oldWord = mFoundWords
        if (oldWord != null) {
            val word = WordUI(oldWord.word, oldWord.lang, oldWord.definition) // Deleted word has same values but no id and notes
            wordState.value = WordState(word, NOT_SAVED_ID, wordState.value?.span)
        } else {
            presenter.onLanguageSpinnerChange(languageFrom, languageToISO)
        }
        updateDatabaseWord(NOT_SAVED_ID)
        deleteDialogShown.value = false
        editDialogShown.value = false
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
            playIconState.value = playIconState.value.copy(isLoading = true)
        }
    }

    override fun showPlayIcon() {
        if (_bindingWord != null) {
            playButton.setImageResource(R.drawable.ic_volume_up_black_24dp)
            playProgressBar.visibility = View.INVISIBLE
            playButton.visibility = View.VISIBLE
        } else {
            playIconState.value = playIconState.value.copy(isLoading = false, isPlaying = false)
        }
    }

    override fun showStopIcon() {
        if (_bindingWord != null) {
            playButton.setImageResource(R.drawable.ic_stop_black_24dp)
            playProgressBar.visibility = View.INVISIBLE
            playButton.visibility = View.VISIBLE
        } else {
            playIconState.value = playIconState.value.copy(isLoading = false, isPlaying = true)
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
            if (fragment is TranslationFragment) fragment.updateTranslation(word, languagePreferenceIndex)
            mFoundWords = word
            wordState.value = WordState(word.toUI(), word.id, wordState.value?.span)
        } else {
            if(selectedSpans.value != null) selectedSpans.value = null
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
        val theme = arguments?.getString(THEME_KEY)

        brightnessTheme = if(!theme.isNullOrEmpty()) {
            BrightnessTheme.get(theme)
        } else {
            BrightnessTheme.get(isNightMode(requireContext()))
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

    private fun setSavedWordToolbar(word: Words) {
        bindingWord.saveIcon.setImageResource(R.drawable.ic_bookmark_black_24dp)
        bindingWord.saveIcon.setOnClickListener { editDialogShown.value = true }

        // Hides language from spinner, because language is already predefined.
        bindingWord.spinnerLanguageFrom.visibility = View.INVISIBLE
        bindingWord.textLanguageCode.text = word.lang
        bindingWord.textLanguageCode.visibility = View.VISIBLE

        wordState.value = WordState(word.toUI(), word.id, wordState.value?.span)
        bindingWord.composeRoot.setContent {
            VisualizerTheme(theme = brightnessTheme) {
                languages = resources.getStringArray(R.array.googleTranslateLanguagesArray).toList()
                Dialogs()
            }
        }
    }

    private fun setPlayButton(text: String) {
        playButton = bindingWord.playTtsIcon
        playButton.setOnClickListener { presenter.onClickReproduce(text) }

        playProgressBar = bindingWord.playLoadingIcon
    }

    private fun onPlayButtonClick(text: String) {
        if (playIconState.value.isTTSAvailable) {
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

    private fun setLanguageFromSpinner() {
        val spinner = bindingWord.spinnerLanguageFrom
        val adapter = DifferentValuesAdapter.createFromResource(
            requireContext(),
            R.array.googleTranslateLangsWithAutoValue,
            R.array.googleTranslateLangsWithAutoArray,
            android.R.layout.simple_spinner_dropdown_item
        )
        spinner.setAdapter(adapter)
        val item = spinner.adapter.getItem(languageFromIndex)
        if (item != null) spinner.setText(item.toString(), false)
        spinner.setOnItemClickListener { _, _, position, _ -> updateLanguageFrom(position) }
        spinner.setOnClickListener { spinner.showDropDown() }
        spinner.post { spinner.dropDownVerticalOffset = -spinner.height }
    }

    /**
     * Listener for translation fragment when using ViewPager
     */
    private val translationFragListener = object : TranslationFragment.Listener {
        override fun onItemSelected(position: Int) {
            languageToISO = languagesISO[position]
            languagePreferenceIndex = position
            val editor = preferences.edit()
            editor.putInt(LANGUAGE_PREFERENCE, position)
            editor.apply()
            presenter.onLanguageSpinnerChange(languageFrom, languageToISO)
        }
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
        languagePreferenceIndex = position
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

    private fun updateDatabaseWord(id: Int) {
        val fragment = childFragmentManager.fragments.find { it is TranslationFragment}
        if (fragment != null && fragment is TranslationFragment) {
            fragment.setWordId(id)
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

    @Composable
    fun Dialogs() {
        val state = wordState.value ?: return
        val word = state.word

        EditDeleteWordDialogs(
            state,
            editDialogShown,
            deleteDialogShown,
            LanguagesList(languages, languagesISO),
            onSave = {
                val resultWord = it.toWord()
                resultWord.id = state.dbId
                if(state.isSaved) saveWordViewModel.update(resultWord) else saveWordViewModel.save(resultWord)
            },
            onDelete = { presenter.onClickDeleteWord(word.word) },
        )
    }

    @Composable
    fun LocalExternalLinksDialog() {

        ExternalLinksDialog(
            isShown = linksDialogShown.value,
            links = wordLinks.value,
            selection = selectedLink,
            onItemClick = { selectedLink = it},
            onDismiss = { linksDialogShown.value = false },
        )
    }
}

@Composable
fun EditDeleteWordDialogs(
    wordState: WordState,
    editDialogShown: MutableState<Boolean>,
    deleteDialogShown: MutableState<Boolean>,
    languages: LanguagesList,
    onSave: (word: WordUI) -> Unit = { _ -> },
    onDelete: () -> Unit = {},
) {
    val word = wordState.word

    var editShown by remember { editDialogShown }
    var deleteShown by remember { deleteDialogShown }

    EditWordDialog(
        isShown = editShown,
        word = word.word,
        language = word.lang,
        translation = word.definition,
        notes = word.notes,
        languages = languages,
        isSaved = wordState.isSaved,
        onSave = onSave,
        onDelete = { deleteDialogShown.value = true },
        onDismiss = { editShown = false },
    )

    if (deleteDialogShown.value) {
        YesNoDialog(
            onDismissRequest = { deleteShown = false },
            onConfirmation = onDelete,
            dialogTitle = LocalContext.current.getString(R.string.delete_word_message),
            dialogText = LocalContext.current.getString(R.string.delete_word_message),
        )
    }
}

data class EditDeleteDialogUI(
    val word: WordState,
    val isDeleteShown: Boolean,
    val languages: LanguagesList
)

@Composable
fun EditDeleteWordDialogs(
    state: EditDeleteDialogUI?,
    onSave: (word: WordUI) -> Unit = { _ -> },
    onDismiss: () -> Unit = {},
    deleteDialogChange: (Boolean) -> Unit = {},
    onDelete: () -> Unit = {},
) {
    if (state == null) return
    val wordState = state.word
    val word = wordState.word

    var deleteShown by remember { mutableStateOf(state.isDeleteShown) }

    EditWordDialog(
        isShown = true,
        word = word.word,
        language = word.lang,
        translation = word.definition,
        notes = word.notes,
        languages = state.languages,
        isSaved = wordState.isSaved,
        onSave = onSave,
        onDelete = {
            deleteDialogChange(true)
            deleteShown = true
        },
        onDismiss = onDismiss,
    )

    if (deleteShown) {
        YesNoDialog(
            onDismissRequest = {
                deleteDialogChange(false)
                deleteShown = false
            },
            onConfirmation = onDelete,
            dialogTitle = LocalContext.current.getString(R.string.delete_word_message),
            dialogText = LocalContext.current.getString(R.string.delete_word_message),
        )
    }
}
