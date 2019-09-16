package com.guillermonegrete.tts.imageprocessing

import android.graphics.Bitmap

interface ImageProcessingSource {

    fun detectText(bitmap: Bitmap, callback: Callback)

    interface Callback{
        fun onTextDetected(text: String, language: String)

        fun onFailure(message: String)
    }
}