
package com.guillermonegrete.tts.main


import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.os.Bundle
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

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.guillermonegrete.tts.CustomTTS.CustomTTS
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.Services.ScreenTextService
import com.guillermonegrete.tts.ThreadExecutor
import com.guillermonegrete.tts.threading.MainThreadImpl

import com.guillermonegrete.tts.Services.ScreenTextService.NORMAL_SERVICE
import com.guillermonegrete.tts.Services.ScreenTextService.NO_FLOATING_ICON_SERVICE
import com.guillermonegrete.tts.data.source.WordRepository
import com.guillermonegrete.tts.data.source.local.WordLocalDataSource
import com.guillermonegrete.tts.data.source.remote.GooglePublicSource
import com.guillermonegrete.tts.db.WordsDatabase


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

    private val clipText: String
        get() {
            val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            val clip = clipboard.primaryClip ?: return ""

            if (clip.itemCount <= 0) return ""

            val pasteData = clip.getItemAt(0).text
            return pasteData?.toString() ?: ""
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        presenter = MainTTSPresenter(
                ThreadExecutor.getInstance(),
                MainThreadImpl.getInstance(),
                this,
                WordRepository.getInstance(GooglePublicSource.getInstance(), WordLocalDataSource.getInstance(WordsDatabase.getDatabase(activity?.applicationContext).wordsDAO())),
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
        val fragment_layout = inflater.inflate(R.layout.fragment_main_tts, container, false)

        val bottomSheet = fragment_layout.findViewById<LinearLayout>(R.id.bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        languageTextView = fragment_layout.findViewById(R.id.text_language_code)

        with(fragment_layout){
            playButton = findViewById(R.id.play_btn)
            ttsProgressBar = findViewById(R.id.play_loading_icon)
        }

        val browseBtn = fragment_layout.findViewById<ImageButton>(R.id.browse_btn)
        val pasteBtn = fragment_layout.findViewById<ImageButton>(R.id.paste_btn)

        editText = fragment_layout.findViewById(R.id.tts_ev)
        editText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus -> if (hasFocus) bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN }

        webview = fragment_layout.findViewById(R.id.webview_wiktionary)
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



        overlayButton = fragment_layout.findViewById<Button>(R.id.startBubble_btn).apply {
            setOnClickListener { presenter?.onStartOverlayMode() }
        }

        clipboardButton = fragment_layout.findViewById<Button>(R.id.clipboard_btn).apply {
            setOnClickListener { presenter?.onStartClipboardMode() }
        }

        return fragment_layout
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                val intent = Intent(activity, ScreenTextService::class.java)
                intent.action = NORMAL_SERVICE
                intent.putExtra(ScreenTextService.EXTRA_RESULT_CODE, resultCode)
                intent.putExtras(data)
                activity?.startService(intent)
                activity?.finish()
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
        activity!!.startService(intent)
    }

    override fun startOverlayService() {
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

            val guide: TourGuide? = TourGuide.create(it){
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
    }
}