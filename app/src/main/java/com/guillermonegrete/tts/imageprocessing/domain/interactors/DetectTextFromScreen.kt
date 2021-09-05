package com.guillermonegrete.tts.imageprocessing.domain.interactors

import android.graphics.Bitmap
import android.graphics.Rect
import com.guillermonegrete.tts.AbstractInteractor
import com.guillermonegrete.tts.MainThread
import com.guillermonegrete.tts.imageprocessing.ImageProcessingSource
import com.guillermonegrete.tts.imageprocessing.ScreenImageCaptor
import java.util.concurrent.ExecutorService
import javax.inject.Inject

class DetectTextFromScreen @Inject constructor (
    executor: ExecutorService,
    mainThread: MainThread,
    private val imageProcessor: ImageProcessingSource
): AbstractInteractor(executor, mainThread) {

    private var screenCaptor: ScreenImageCaptor? = null
    private var areaRect = Rect()
    private var callback: Callback? = null

    operator fun invoke(
        screenCaptor: ScreenImageCaptor,
        areaRect: Rect,
        callback: Callback
    ){
        this.screenCaptor = screenCaptor
        this.areaRect = areaRect
        this.callback = callback
        run()
    }

    override fun run() {
        screenCaptor?.getImage(areaRect, object : ScreenImageCaptor.Callback{
            override fun onImageCaptured(image: Bitmap) {
                imageProcessor.detectText(image, imageProcessorCallback)
            }
        })
    }

    private val imageProcessorCallback = object: ImageProcessingSource.Callback{
        override fun onTextDetected(text: String, language: String) {
            mMainThread.post{callback?.onTextDetected(text, language)}
        }

        override fun onFailure(message: String) {
            mMainThread.post{callback?.onError(message)}
        }
    }

    interface Callback{
        fun onTextDetected(text: String, language: String)

        fun onError(message: String)
    }
}