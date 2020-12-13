package com.guillermonegrete.tts.importtext.visualize.io

import android.content.Context
import android.os.Environment
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class DefaultEpubFileManager @Inject constructor(@ApplicationContext context: Context) : EpubFileManager {

    override val rootDir: File

    override val filesDir: File

    init {
        val state = Environment.getExternalStorageState()
        rootDir = if (Environment.MEDIA_MOUNTED == state) {

            val dirs = ContextCompat.getExternalFilesDirs(context, null)
            // Dirs[0] ==> (emulated)
            // Dirs[1] ==> (SD card)
            val dir = if(dirs.size == 2) dirs[1] else dirs[0]
            dir
        } else {
            context.filesDir
        }
        filesDir = File(rootDir.absolutePath + File.separator + volumes_folder)
    }

    companion object{
        const val volumes_folder = "volumes"
    }
}