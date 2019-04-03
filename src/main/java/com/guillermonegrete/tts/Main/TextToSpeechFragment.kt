package com.guillermonegrete.tts.Main


import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import tourguide.tourguide.TourGuide

import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.guillermonegrete.tts.CustomTTS.CustomTTS
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.Services.ScreenTextService
import com.guillermonegrete.tts.ThreadExecutor
import com.guillermonegrete.tts.data.source.remote.MSTranslatorSource
import com.guillermonegrete.tts.threading.MainThreadImpl

import java.util.Objects

import com.guillermonegrete.tts.Services.ScreenTextService.NORMAL_SERVICE
import com.guillermonegrete.tts.Services.ScreenTextService.NO_FLOATING_ICON_SERVICE


class TextToSpeechFragment : Fragment(), MainTTSContract.View {

    private var presenter: MainTTSPresenter? = null

    private lateinit var editText: EditText
    private lateinit var webview: WebView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

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
                MSTranslatorSource.getInstance(),
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

        val playBtn = fragment_layout.findViewById<ImageButton>(R.id.play_btn)
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


        playBtn.setOnClickListener {
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



        fragment_layout.findViewById<View>(R.id.startBubble_btn).setOnClickListener { presenter!!.onStartOverlayMode() }

        fragment_layout.findViewById<View>(R.id.clipboard_btn).setOnClickListener { presenter!!.onStartClipboardMode() }

        return fragment_layout
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                val intent = Intent(activity, ScreenTextService::class.java)
                intent.action = NORMAL_SERVICE
                intent.putExtra(ScreenTextService.EXTRA_RESULT_CODE, resultCode)
                intent.putExtras(data!!)
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
