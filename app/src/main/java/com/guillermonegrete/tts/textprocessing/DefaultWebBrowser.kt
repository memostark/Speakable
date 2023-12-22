package com.guillermonegrete.tts.textprocessing

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.guillermonegrete.tts.ui.webview.ChromeCustomTabsActivity

enum class DefaultWebBrowser(val type: String) {
    CUSTOM_TABS("custom_tabs") {
        override fun intentForUrl(context: Context, url: String) =
            ChromeCustomTabsActivity.intent(context, url)
    },
    DEVICE_DEFAULT("default") {
        override fun intentForUrl(context: Context, url: String) =
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
    };

    abstract fun intentForUrl(context: Context, url: String): Intent

    companion object{
        const val PREFERENCE_KEY = "default_browser_pref_key"

        fun get(type: String): DefaultWebBrowser {
                return values().find { it.type == type } ?: CUSTOM_TABS
        }
    }
}