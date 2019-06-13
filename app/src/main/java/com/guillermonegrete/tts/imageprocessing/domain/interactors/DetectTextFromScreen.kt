package com.guillermonegrete.tts.imageprocessing.domain.interactors

import android.graphics.Bitmap
import android.graphics.Rect
import com.guillermonegrete.tts.AbstractInteractor
import com.guillermonegrete.tts.Executor
import com.guillermonegrete.tts.MainThread
import com.guillermonegrete.tts.imageprocessing.ImageProcessingSource
import com.guillermonegrete.tts.imageprocessing.ScreenImageCaptor

class DetectTextFromScreen (
    executor: Executor,
    mainThread: MainThread,
    private val screenCaptor: ScreenImageCaptor,
    private val imageProcessor: ImageProcessingSource,
    private val areaRect: Rect,
    private val callback: Callback
): AbstractInteractor(executor, mainThread) {

    override fun run() {
        screenCaptor.getImage(areaRect, object : ScreenImageCaptor.Callback{
            override fun onImageCaptured(image: Bitmap) {
                imageProcessor.detectText(image, imageProcessorCallback)
            }
        })
    }

    private val imageProcessorCallback= object: ImageProcessingSource.Callback{
        override fun onTextDetected(text: String, language: String) {
            mMainThread.post{callback.onTextDetected(text, language)}
        }

        override fun onFailure() {}
    }

    interface Callback{
        fun onTextDetected(text: String, language: String)
    }
}