package com.guillermonegrete.tts

import android.app.Application
import android.content.Context
import com.github.tmurakami.dexopener.DexOpener
import androidx.test.runner.AndroidJUnitRunner

/**
 * Enable mocks for kotlin androidTests
 */
class DexOpenerAndroidJUnitRunner : AndroidJUnitRunner() {

    @Throws(ClassNotFoundException::class, IllegalAccessException::class, InstantiationException::class)
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application {
        DexOpener.install(this) // Call me first!
        return super.newApplication(cl, className, context)
    }
}