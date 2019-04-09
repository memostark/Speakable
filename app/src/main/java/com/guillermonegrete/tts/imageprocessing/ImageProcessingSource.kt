package com.guillermonegrete.tts.imageprocessing

import android.graphics.Bitmap

interface ImageProcessingSource {

    fun onDetectText(bitmap: Bitmap, callback: Callback)

    interface Callback{
        fun onTextDetected(text: String, language: String)

        fun onFailure()
    }
}