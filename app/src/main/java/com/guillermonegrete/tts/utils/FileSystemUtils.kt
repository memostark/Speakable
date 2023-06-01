package com.guillermonegrete.tts.utils

import java.io.File

fun makeDir(folder: File) = folder.mkdir()

/**
 * Writes the text [content] to the [file]
 */
fun writeToFile(file: File, content: String) {
    file.bufferedWriter().use { it.write(content) }
}

/**
 * Deletes folder and all its sub-folders.
 *
 * @return true if and only if the file or directory is successfully deleted
 */
fun deleteAllFolder(folder: File): Boolean {

    val files = folder.listFiles()
    if (files != null) {
        for (child: File in files) {
            child.delete()
        }
    }

    return folder.delete()
}
