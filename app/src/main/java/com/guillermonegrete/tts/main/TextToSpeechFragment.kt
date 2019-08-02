package com.guillermonegrete.tts.main


import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import tourguide.tourguide.TourGuide

import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.preference.PreferenceManager

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.guillermonegrete.tts.BuildConfig
import com.guillermonegrete.tts.customtts.CustomTTS
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.services.ScreenTextService
import com.guillermonegrete.tts.ThreadExecutor
import com.guillermonegrete.tts.threading.MainThreadImpl

import com.guillermonegrete.tts.services.ScreenTextService.NORMAL_SERVICE
import com.guillermonegrete.tts.services.ScreenTextService.NO_FLOATING_ICON_SERVICE
import com.guillermonegrete.tts.data.source.WordDataSource
import com.guillermonegrete.tts.data.source.WordRepository
import com.guillermonegrete.tts.data.source.remote.GooglePublicSource
import com.guillermonegrete.tts.data.source.remote.MSTranslatorSource
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject


class TextToSpeechFragment : Fragment(), MainTTSContract.View {

    private var presenter: MainTTSPresenter? = null

    private lateinit var editText: EditText
    private lateinit var webview: WebView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    private lateinit var playButton: ImageButton
    private lateinit var ttsProgressBar: ProgressBar

    private lateinit var clipboardButton: Button
    private lateinit var overlayButton: Button

    private lateinit var languageTextView: TextView

    @Inject
    lateinit var wordRepository: WordRepository

    private val clipText: String
        get() {
            val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            val clip = clipboard.primaryClip ?: return ""

            if (clip.itemCount <= 0) return ""

            val pasteData = clip.getItemAt(0).text
            return pasteData?.toString() ?: ""
        }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        presenter = MainTTSPresenter(
                ThreadExecutor.getInstance(),
                MainThreadImpl.getInstance(),
                this,
//                WordRepository.getInstance(getTranslatorSource(), WordLocalDataSource.getInstance(WordsDatabase.getDatabase(activity?.applicationContext).wordsDAO())),
            wordRepository,
            CustomTTS.getInstance(activity?.applicationContext)
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_tts_fragment, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.how_to_menu_item -> {
                playTutorial()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentLayout = inflater.inflate(R.layout.fragment_main_tts, container, false)

        val bottomSheet = fragmentLayout.findViewById<LinearLayout>(R.id.bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        languageTextView = fragmentLayout.findViewById(R.id.text_language_code)

        with(fragmentLayout){
            playButton = findViewById(R.id.play_btn)
            ttsProgressBar = findViewById(R.id.play_loading_icon)
        }

        val browseBtn = fragmentLayout.findViewById<ImageButton>(R.id.browse_btn)
        val pasteBtn = fragmentLayout.findViewById<ImageButton>(R.id.paste_btn)

        editText = fragmentLayout.findViewById(R.id.tts_ev)
        editText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus -> if (hasFocus) bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN }

        webview = fragmentLayout.findViewById(R.id.webview_wiktionary)
        webview.setOnTouchListener { view, _ ->
            view.parent.requestDisallowInterceptTouchEvent(true)
            false
        }
        webview.webViewClient = HelloWebViewClient()


        playButton.setOnClickListener {
            val text = editText.text.toString()
            presenter?.onClickReproduce(text)
        }

        browseBtn.setOnClickListener {
            hideKeyboard()
            val text = editText.text.toString()
            presenter?.onClickShowBrowser(text)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        pasteBtn.setOnClickListener { presenter!!.onClickPaste(clipText) }



        overlayButton = fragmentLayout.findViewById<Button>(R.id.startBubble_btn).apply {
            setOnClickListener { presenter?.onStartOverlayMode() }
        }

        clipboardButton = fragmentLayout.findViewById<Button>(R.id.clipboard_btn).apply {
            setOnClickListener { presenter?.onStartClipboardMode() }
        }

        return fragmentLayout
    }

    override fun onStart() {
        super.onStart()
        presenter?.start()
    }

    override fun onStop() {
        super.onStop()
        presenter?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter?.destroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == AppCompatActivity.RESULT_OK && data != null) {
            when(requestCode) {
                REQUEST_CODE_SCREEN_CAPTURE -> {
                    val intent = Intent(activity, ScreenTextService::class.java)
                    intent.action = NORMAL_SERVICE
                    intent.putExtra(ScreenTextService.EXTRA_RESULT_CODE, resultCode)
                    intent.putExtras(data)
                    activity?.startService(intent)
                    activity?.finish()
                }
                REQUEST_CODE_DRAW_OVERLAY -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Settings.canDrawOverlays(context)) getScreenCaptureIntent()
                    }
                }
            }
        }
    }

    override fun setDictionaryWebPage(word: String) {
        webview.loadUrl("https://en.m.wiktionary.org/wiki/$word")
    }

    override fun setEditText(text: String) {
        editText.setText(text)
    }

    override fun startClipboardService() {
        val intent = Intent(activity, ScreenTextService::class.java)
        intent.action = NO_FLOATING_ICON_SERVICE
        activity?.startService(intent)
    }

    override fun startOverlayService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context?.packageName))
            startActivityForResult(intent, REQUEST_CODE_DRAW_OVERLAY)
        } else {
            getScreenCaptureIntent()
        }
    }

    private fun getScreenCaptureIntent(){
        val manager = activity?.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val permissionIntent = manager.createScreenCaptureIntent()
        startActivityForResult(permissionIntent, REQUEST_CODE_SCREEN_CAPTURE)
    }

    override fun showDetectedLanguage(language: String?) {
        languageTextView.text = language
    }

    override fun showLanguageNotAvailable() {
        Toast.makeText(context, "Detected language not available for Text to Speech", Toast.LENGTH_SHORT).show()
        ttsProgressBar.visibility = View.GONE
        playButton.visibility = View.VISIBLE
    }

    override fun showLoadingTTS() {
        ttsProgressBar.visibility = View.VISIBLE
        playButton.visibility = View.GONE
    }

    override fun showPlayIcon() {
        playButton.setImageResource(R.drawable.ic_volume_up_black_24dp)
    }

    override fun showStopIcon() {
        playButton.setImageResource(R.drawable.ic_stop_black_24dp)
        ttsProgressBar.visibility = View.GONE
        playButton.visibility = View.VISIBLE
    }


    private fun getTranslatorSource(): WordDataSource{
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val translatorPreference = preferences.getString(TranslatorType.PREFERENCE_KEY, "")
        val translatorType = if(translatorPreference.isNullOrEmpty()) TranslatorType.GOOGLE_PUBLIC.value else translatorPreference.toInt()

        return when(TranslatorType.valueOf(translatorType)){
            TranslatorType.GOOGLE_PUBLIC -> GooglePublicSource.getInstance()
            TranslatorType.MICROSOFT -> MSTranslatorSource.getInstance(BuildConfig.TranslatorApiKey)
        }
    }

    private fun hideKeyboard() {
        val context = activity
        if (context != null) {
            val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val focusedView = context.currentFocus
            if (focusedView != null) {
                inputMethodManager.hideSoftInputFromWindow(focusedView.windowToken, 0)
            }
        }
    }

    private fun playTutorial(){
        activity?.let {

            val guideOverlay: TourGuide = TourGuide.create(it){
                toolTip {
                    title{"Enable overlay mode"}
                    description { "Shows floating icon that allows you to select text to reproduce" }
                }
                overlay {
                    backgroundColor { Color.parseColor("#66FF0000") }
                    disableClickThroughHole(true)
                    setOnClickListener(View.OnClickListener { this@create.cleanUp() })
                }
            }

            TourGuide.create(it){
                toolTip {
                    title{"Enable clipboard mode"}
                    description { "Shows dialog with translation whenever text is copied to clipboard" }
                }
                overlay {
                    backgroundColor { Color.parseColor("#66FF0000") }
                    disableClickThroughHole(true)
                    setOnClickListener(View.OnClickListener {
                        this@create.cleanUp()
                        guideOverlay.playOn(overlayButton)
                    })
                }
            }.playOn(clipboardButton)

        }
    }

    private inner class HelloWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            view.loadUrl(url)
            return true
        }
    }

    companion object {

        const val REQUEST_CODE_SCREEN_CAPTURE = 100
        const val REQUEST_CODE_DRAW_OVERLAY = 300
    }
}