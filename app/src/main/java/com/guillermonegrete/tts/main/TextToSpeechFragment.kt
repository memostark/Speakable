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
import android.util.Patterns
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import tourguide.tourguide.TourGuide

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.databinding.FragmentMainTtsBinding
import com.guillermonegrete.tts.services.ScreenTextService

import com.guillermonegrete.tts.services.ScreenTextService.NORMAL_SERVICE
import com.guillermonegrete.tts.services.ScreenTextService.NO_FLOATING_ICON_SERVICE
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class TextToSpeechFragment: Fragment(R.layout.fragment_main_tts), MainTTSContract.View, MenuProvider {

    @Inject lateinit var presenter: MainTTSPresenter

    private  var _binding: FragmentMainTtsBinding? = null
    private val binding get() = _binding!!

    private lateinit var requestOverlayPermission: ActivityResultLauncher<Intent>
    private lateinit var requestScreenCapture: ActivityResultLauncher<Intent>

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

        requestOverlayPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // If overlay drawing permission was granted ask for the screen capture one
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(context)) getScreenCaptureIntent()
            }
        }

        requestScreenCapture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data ?: return@registerForActivityResult
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val intent = Intent(activity, ScreenTextService::class.java)
                intent.action = NORMAL_SERVICE
                intent.putExtra(ScreenTextService.EXTRA_RESULT_CODE, result.resultCode)
                intent.putExtras(data)
                activity?.startService(intent)
                activity?.finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.setView(this)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_tts_fragment, menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.how_to_menu_item -> {
                playTutorial()
                true
            }
            R.id.info_item -> {
                findNavController().navigate(R.id.action_textToSpeechFragment_to_appInfoDest)
                true
            }
            else -> false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        _binding = FragmentMainTtsBinding.bind(view)

        val bottomSheetBehavior = BottomSheetBehavior.from(binding.bottom.root)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        with(binding.main){

            browseBtn.setOnClickListener {
                hideKeyboard()
                val text = ttsEditText.text.toString()
                presenter.onClickShowBrowser(text)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }

            pasteBtn.setOnClickListener { presenter.onClickPaste(clipText) }

            ttsEditText.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }

            ttsEditText.doAfterTextChanged {
                webReaderBtn.isVisible = Patterns.WEB_URL.matcher(it.toString()).matches()
            }

            startBubbleBtn.setOnClickListener { presenter.onStartOverlayMode() }

            clipboardBtn.setOnClickListener { presenter.onStartClipboardMode() }

            val autoText = getString(R.string.auto_detect)
            pickLanguage.text = autoText
            pickLanguage.setOnClickListener { findNavController().navigate(R.id.action_textToSpeech_to_pickLanguageFragment) }

            playBtn.setOnClickListener {
                val text = ttsEditText.text.toString()

                val selectedLang = pickLanguage.text.toString()
                val lang = if(selectedLang == autoText) null else selectedLang
                presenter.onClickReproduce(text, lang)
            }

            webReaderBtn.setOnClickListener {
                val url = ttsEditText.text.toString()
                val action = TextToSpeechFragmentDirections.toWebReaderFragmentAction(url)
                findNavController().navigate(action)
            }
        }

        val webview = binding.bottom.webviewWiktionary
        webview.setOnTouchListener { webView, _ ->
            webView.parent.requestDisallowInterceptTouchEvent(true)
            false
        }
        webview.webViewClient = HelloWebViewClient()

        setFragmentResultListener("requestKey") { _, bundle ->
            binding.main.pickLanguage.text = bundle.getString("lang")
        }

        setupOptionsMenu()
    }

    private fun setupOptionsMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        presenter.start()
    }

    override fun onStop() {
        super.onStop()
        presenter.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.destroy()
    }

    override fun setDictionaryWebPage(word: String) {
        binding.bottom.webviewWiktionary.loadUrl("https://en.m.wiktionary.org/wiki/$word")
    }

    override fun setEditText(text: String) {
        binding.main.ttsEditText.setText(text)
    }

    override fun startClipboardService() {
        val intent = Intent(activity, ScreenTextService::class.java)
        intent.action = NO_FLOATING_ICON_SERVICE
        activity?.startService(intent)
    }

    override fun startOverlayService() {
        // For versions of android older than Android M (sdk 23), only it was necessary to request the screen capture permission
        // For newer versions it's necessary to first ask for the permission to draw overlays, then ask for the screen capture one
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context?.packageName))
            requestOverlayPermission.launch(intent)
        } else {
            getScreenCaptureIntent()
        }
    }

    private fun getScreenCaptureIntent(){
        val manager = activity?.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val permissionIntent = manager.createScreenCaptureIntent()
        requestScreenCapture.launch(permissionIntent)
    }

    override fun showDetectedLanguage(language: String?) {
        binding.main.textLanguageCode.text = language
    }

    override fun showLanguageNotAvailable() {
        Toast.makeText(context, "Detected language not available for Text to Speech", Toast.LENGTH_SHORT).show()
        setPlayIcons(true)
    }

    override fun showLoadingTTS() {
        setPlayIcons(false)
    }

    override fun showPlayIcon() {
        setIcon(R.drawable.ic_volume_up_black_24dp)
    }

    override fun showStopIcon() {
        setIcon(R.drawable.ic_stop_black_24dp)
        setPlayIcons(true)
    }

    private fun setPlayIcons(visible: Boolean){
        with(binding.main){
            playLoadingIcon.visibility = if(visible) View.INVISIBLE else View.VISIBLE
            playBtn.visibility = if(visible) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun setIcon(@DrawableRes icon: Int){
        binding.main.playBtn.setCompoundDrawablesWithIntrinsicBounds(0, icon, 0, 0)
    }

    private fun hideKeyboard() {
        val context = activity ?: return
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val focusedView = context.currentFocus ?: return
        inputMethodManager.hideSoftInputFromWindow(focusedView.windowToken, 0)
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
                    setOnClickListener { this@create.cleanUp() }
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
                    setOnClickListener {
                        this@create.cleanUp()
                        guideOverlay.playOn(binding.main.startBubbleBtn)
                    }
                }
            }.playOn(binding.main.clipboardBtn)
        }
    }

    private inner class HelloWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            view.loadUrl(url)
            return true
        }
    }
}
