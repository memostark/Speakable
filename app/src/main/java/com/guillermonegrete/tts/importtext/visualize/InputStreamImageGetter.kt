package com.guillermonegrete.tts.importtext.visualize

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Html
import kotlinx.coroutines.runBlocking

class InputStreamImageGetter(
    private val context: Context,
    private val zipFileReader: ZipFileReader
): Html.ImageGetter {

    var basePath = ""

    override fun getDrawable(source: String?): Drawable? {
        return runBlocking {
            val inputStream = source?.let {
                val fullPath = if(basePath.isEmpty()) it else "$basePath/$it"
                println("getDrawable source: $source, full path: $fullPath")
                zipFileReader.getFileStream(fullPath)
            }
            val bitmap = BitmapFactory.decodeStream(inputStream)
            bitmap?.density = Bitmap.DENSITY_NONE
            val drawable = BitmapDrawable(context.resources, bitmap)
            drawable.bounds = Rect(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
            return@runBlocking drawable
        }
    }
}