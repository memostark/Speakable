package com.guillermonegrete.tts.importtext

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Create an epub file in a temporary directory copied from the assets.
 */
fun TemporaryFolder.createEpubFile(): File {
    val context = InstrumentationRegistry.getInstrumentation().context

    val tempFile = newFile("copied_file.epub")
    context.assets.open("test_epub.epub").use { input ->
        tempFile.outputStream().use { output ->
            input.copyTo(output, 1024)
        }
    }

    return tempFile
}
