package com.guillermonegrete.tts.textprocessing

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.SpeakableApplication
import com.guillermonegrete.tts.customviews.ButtonsPreference
import com.guillermonegrete.tts.db.ExternalLink
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.main.SettingsFragment
import com.guillermonegrete.tts.savedwords.SaveWordDialogFragment
import com.guillermonegrete.tts.savedwords.SaveWordDialogFragment.TAG_DIALOG_UPDATE_WORD
import com.guillermonegrete.tts.services.ScreenTextService
import com.guillermonegrete.tts.services.ScreenTextService.NO_FLOATING_ICON_SERVICE
import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem
import com.guillermonegrete.tts.ui.DifferentValuesAdapter
import java.util.*
import javax.inject.Inject

class TextInfoDialog private constructor(): DialogFragment(), ProcessTextContract.View, SaveWordDialogFragment.Callback {

    private var window: Window? = null

    private var dictionaryAdapter: WiktionaryAdapter? = null

    private var inputText: String? = null

    private lateinit var mFoundWords: Words

    @Inject
    internal lateinit var presenter: ProcessTextContract.Presenter

    private var pager: ViewPager2? = null
    private var pagerAdapter: MyPageAdapter? = null

    private var playButton: ImageButton? = null
    private var playProgressBar: ProgressBar? = null

    private var playIconsContainer: View? = null

    @Inject
    internal lateinit var preferences: SharedPreferences

    private var languagesISO: Array<String> = arrayOf()
    private var languageFromIndex: Int = -1
    private var languagePreferenceIndex: Int = 0
    private var languageFrom: String? = null
    private var languageToISO: String? = null

    private var ttsReady = false

    override fun onAttach(context: Context) {
        (context.applicationContext as SpeakableApplication).appComponent.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        inputText = arguments?.getString(TEXT_KEY)

        languageToISO = getPreferenceISO()
        languageFrom = getLanguageFromPreference()

        presenter.setView(this)
        val extraWord: Words? = arguments?.getParcelable(WORD_KEY)
        if(extraWord != null){
            presenter.start(extraWord)
        }else{
            val action = arguments?.getString(ACTION_KEY)
            if(NO_SERVICE == action){
                presenter.getLayout(inputText, languageFrom, languageToISO)
            }else{
                presenter.startWithService(inputText, languageFrom, languageToISO)
            }
        }
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
    ): View? {
        keepImmersiveMode()

        return inflater.inflate(R.layout.placeholder_layout, container, false)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        val parent = activity
        if(parent is DialogInterface.OnDismissListener) parent.onDismiss(dialog)
    }

    private fun getPreferenceISO(): String {
        val englishIndex = 15
        languagePreferenceIndex = preferences.getInt(LANGUAGE_PREFERENCE, englishIndex)
        languagesISO = resources.getStringArray(R.array.googleTranslateLanguagesValue)
        return languagesISO[languagePreferenceIndex]
    }

    private fun getLanguageFromPreference(): String {
        val preference = preferences.getString(SettingsFragment.PREF_LANGUAGE_FROM, "auto") ?: "auto"
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
        setContentView(R.layout.activity_processtext)
        val textString = word.word

        val mTextTTS = view?.findViewById<TextView>(R.id.text_tts)
        mTextTTS?.text = textString

        val textViewLanguage = view?.findViewById<TextView>(R.id.text_language_code)
        textViewLanguage?.visibility = if(languageFrom == "auto") View.VISIBLE else View.GONE
        textViewLanguage?.text = word.lang

        setPlayButton(textString)
        setLanguageFromSpinner()

        view?.findViewById<View>(R.id.save_icon)?.setOnClickListener { presenter.onClickBookmark() }
    }

    override fun setWiktionaryLayout(word: Words, items: List<WikiItem>) {
        val isLargeWindow = preferences.getBoolean(
            SettingsFragment.PREF_WINDOW_SIZE,
            ButtonsPreference.DEFAULT_VALUE
        )
        if (isLargeWindow) setCenterDialog() else setBottomDialog()
        setWordLayout(word)
        dictionaryAdapter = WiktionaryAdapter(requireContext(), items)

        mFoundWords = word

        if (isLargeWindow) createViewPager() else createSmallViewPager()
    }


    override fun setSavedWordLayout(word: Words) {
        setBottomDialog()
        mFoundWords = word

        setWordLayout(word)
        createSmallViewPager()

        setSavedWordToolbar(word)
        languagePreferenceIndex = -1 // Indicates spinner not visible

    }

    override fun setDictWithSaveWordLayout(word: Words, items: List<WikiItem>) {
        setWiktionaryLayout(word, items)
        setSavedWordToolbar(word)
        languagePreferenceIndex = -1 // Indicates spinner not visible

        // Hides language from spinner, because language is already predefined.
        val spinner = view?.findViewById<Spinner>(R.id.spinner_language_from_code)
        spinner?.visibility = View.INVISIBLE

        val textViewLanguage = view?.findViewById<TextView>(R.id.text_language_code)
        textViewLanguage?.visibility = View.VISIBLE

    }

    override fun setTranslationLayout(word: Words) {
        setBottomDialog()
        mFoundWords = word
        setWordLayout(word)
        createSmallViewPager()
    }

    override fun setSentenceLayout(word: Words) {
        setBottomDialog()
        val text = word.word
        setContentView(R.layout.activity_process_sentence)
        val mTextTTS = view?.findViewById<TextView>(R.id.text_tts)
        mTextTTS?.text = text
        val mTextTranslation = view?.findViewById<TextView>(R.id.text_translation)
        mTextTranslation?.text = word.definition
        val textLanguage = view?.findViewById<TextView>(R.id.text_language_code)
        textLanguage?.visibility = if (languageFrom == "auto") View.VISIBLE else View.GONE
        textLanguage?.text = word.lang
        setPlayButton(text)

        setLanguageFromSpinner()
        setSpinner()
    }

    override fun setExternalDictionary(links: List<ExternalLink>) {
        pagerAdapter = MyPageAdapter(this)
        if (dictionaryAdapter != null) pagerAdapter?.addFragment(
            DefinitionFragment.newInstance(dictionaryAdapter)
        )
        val translationFragment = TranslationFragment.newInstance(mFoundWords, languagePreferenceIndex)
        translationFragment.setListener(translationFragListener)
        pagerAdapter?.addFragment(translationFragment)
        pagerAdapter?.addFragment(
            ExternalLinksFragment.newInstance(inputText, links as ArrayList<ExternalLink>)
        )
        pager?.adapter = pagerAdapter

        val tabLayout = view?.findViewById<TabLayout>(R.id.pager_menu_dots)
        if(tabLayout != null){
            pager?.let {
                TabLayoutMediator(tabLayout, it, true) { _, _ -> }.attach() // Tab without text
            }
        }
    }

    override fun setTranslationErrorMessage() {
        // If pager is not null, means we are using activity_processtext layout,
        // otherwise is sentence layout
        pagerAdapter?.let {
            // Check if the adapter has dictionary fragment
            val index: Int = if (dictionaryAdapter != null && it.itemCount == 3) 1 else 0

            val fragment = it.fragments[index]
            if (fragment is TranslationFragment) fragment.setErrorLayout()
        }
    }

    override fun showSaveDialog(word: Words) {
        val dialogFragment: DialogFragment = SaveWordDialogFragment.newInstance(word)
        dialogFragment.show(childFragmentManager, "New word process")
    }

    override fun showDeleteDialog(word: String) {

        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage("Do you want to delete this word?")
            .setPositiveButton("Yes") { dialog, _ ->
                presenter.onClickDeleteWord(word)
                dialog.dismiss()

            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }

        builder.create().show()
    }

    override fun showWordDeleted() {
        val saveIcon = view?.findViewById<ImageButton>(R.id.save_icon)
        saveIcon?.setImageResource(R.drawable.ic_bookmark_border_black_24dp)

        val editIcon = view?.findViewById<ImageButton>(R.id.edit_icon)
        editIcon?.visibility = View.GONE
    }

    override fun startService() {
        val intentService = Intent(context, ScreenTextService::class.java)
        intentService.action = NO_FLOATING_ICON_SERVICE
        context?.startService(intentService)
    }

    override fun showLanguageNotAvailable() {
        if (playIconsContainer != null) { // Why do you need to check this?
            playIconsContainer?.visibility = View.GONE
            Toast.makeText(context, "Language not available for TTS", Toast.LENGTH_SHORT).show()
        }
        ttsReady = false
    }

    override fun showLoadingTTS() {
        playProgressBar?.visibility = View.VISIBLE
        playButton?.visibility = View.GONE
        ttsReady = false
    }

    override fun showPlayIcon() {
        playButton?.setImageResource(R.drawable.ic_volume_up_black_24dp)
        playProgressBar?.visibility = View.GONE
        playButton?.visibility = View.VISIBLE
        ttsReady = true
    }

    override fun showStopIcon() {
        playButton?.setImageResource(R.drawable.ic_stop_black_24dp)
        playProgressBar?.visibility = View.GONE
        playButton?.visibility = View.VISIBLE
        ttsReady = true
    }

    override fun updateTranslation(translation: String) {
        val textLanguage = view?.findViewById<TextView>(R.id.text_language_code)
        textLanguage?.visibility = if (languageFrom == "auto") View.VISIBLE else  View.GONE

        // If pager is not null, means we are using activity_processtext layout,
        // otherwise is sentence layout
        if (pager != null) {
            val fragIndex = pager?.currentItem
            if(fragIndex != null) {
                val fragment = pagerAdapter?.fragments?.get(fragIndex)
                if (fragment is TranslationFragment) fragment.updateTranslation(translation)
            }
        } else {
            val translationTextView = view?.findViewById<TextView>(R.id.text_translation)
            translationTextView?.text = translation
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

    private fun setContentView(@LayoutRes id: Int){
        val inflater = activity?.layoutInflater
        val newView = inflater?.inflate(id, null)
        val rootView = view as ViewGroup
        rootView.removeAllViews()
        rootView.addView(newView)
    }

    private fun setCenterDialog() {
        window?.let {
            val wlp = it.attributes
            wlp.flags =
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            it.attributes = wlp
        }
    }

    private fun setBottomDialog() {

        window?.let {
            val wlp = it.attributes
            wlp.dimAmount = 0f
            wlp.flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            wlp.y = 50 * resources.displayMetrics.density.toInt() // dp to px
            wlp.gravity = Gravity.BOTTOM
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            it.attributes = wlp
        }
    }

    private fun setSavedWordToolbar(word: Words) {
        val saveIcon = view?.findViewById<ImageButton>(R.id.save_icon)
        saveIcon?.setImageResource(R.drawable.ic_bookmark_black_24dp)

        val editIcon = view?.findViewById<ImageButton>(R.id.edit_icon)
        editIcon?.visibility = View.VISIBLE
        editIcon?.setOnClickListener {
            val dialogFragment = SaveWordDialogFragment.newInstance(word)
            dialogFragment.show(childFragmentManager, TAG_DIALOG_UPDATE_WORD)
        }

    }

    private fun setPlayButton(text: String) {
        playButton = view?.findViewById(R.id.play_tts_icon)
        playButton?.setOnClickListener { presenter.onClickReproduce(text) }

        playProgressBar = view?.findViewById(R.id.play_loading_icon)
        playIconsContainer = view?.findViewById(R.id.play_icons_container)

        if(ttsReady){
            playProgressBar?.visibility = View.GONE
            playButton?.visibility = View.VISIBLE
        }
    }

    private fun createViewPager() {
        pager = view?.findViewById(R.id.process_view_pager)
    }

    private fun createSmallViewPager() {
        pager = view?.findViewById(R.id.process_view_pager)
        val params = pager?.layoutParams
        params?.height = 250
        pager?.layoutParams = params
    }

    private fun setLanguageFromSpinner() {
        val spinner: Spinner? = view?.findViewById(R.id.spinner_language_from_code)
        val adapter = DifferentValuesAdapter.createFromResource(
            requireContext(),
            R.array.googleTranslateLangsWithAutoValue,
            R.array.googleTranslateLangsWithAutoArray,
            R.layout.spinner_layout_end
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner?.adapter = adapter

        spinner?.setSelection(languageFromIndex, false)
        spinner?.onItemSelectedListener = SpinnerListener()
    }

    private fun setSpinner() {
        val spinner = view?.findViewById<Spinner>(R.id.translate_to_spinner)
        val adapter = DifferentValuesAdapter.createFromResource(
            requireContext(),
            R.array.googleTranslateLanguagesValue,
            R.array.googleTranslateLanguagesArray,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner?.adapter = adapter

        spinner?.setSelection(languagePreferenceIndex, false)
        spinner?.onItemSelectedListener = SpinnerListener()
    }

    override fun onWordSaved(word: Words) {
        presenter.onClickSaveWord(word)
        val saveIcon = view?.findViewById<ImageButton>(R.id.save_icon)
        saveIcon?.setImageResource(R.drawable.ic_bookmark_black_24dp)

        val editIcon = view?.findViewById<ImageButton>(R.id.edit_icon)
        editIcon?.visibility = View.VISIBLE
        editIcon?.setOnClickListener {
            val dialogFragment = SaveWordDialogFragment.newInstance(word)
            dialogFragment.show(childFragmentManager, TAG_DIALOG_UPDATE_WORD)
        }
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
                R.id.spinner_language_from_code -> {
                    languageFrom = if (position == 0)
                        "auto"
                    else
                        languagesISO[position - 1]
                    editor.putString(SettingsFragment.PREF_LANGUAGE_FROM, languageFrom)
                    editor.apply()
                    presenter.onLanguageSpinnerChange(languageFrom, languageToISO)
                }
                R.id.translate_to_spinner -> {
                    languageToISO = languagesISO[position]
                    editor.putInt(LANGUAGE_PREFERENCE, position)
                    editor.apply()
                    presenter.onLanguageSpinnerChange(languageFrom, languageToISO)
                }
                else -> {
                }
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>) {}
    }


    private inner class MyPageAdapter internal constructor(fragment: Fragment) :
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

        private const val LANGUAGE_PREFERENCE = "ProcessTextLangPreference"
        const val NO_SERVICE = "no_service"

        @JvmStatic
        fun newInstance(text: String, action: String?, word: Words?) = TextInfoDialog().apply {
            arguments = Bundle().apply {
                putString(TEXT_KEY, text)
                putString(ACTION_KEY, action)
                putParcelable(WORD_KEY, word)
            }
        }
    }
}