package com.guillermonegrete.tts.ui.webview


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent

class ChromeCustomTabsActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra(KEY_URL)

        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(this, Uri.parse(url))

        finish()
    }

    companion object{
        const val KEY_URL = "url"
        fun intent(context: Context, url: String) =
            Intent(context, ChromeCustomTabsActivity::class.java).apply { putExtra(KEY_URL, url) }
    }
}