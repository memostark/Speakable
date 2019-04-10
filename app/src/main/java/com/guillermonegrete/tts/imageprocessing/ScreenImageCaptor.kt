package com.guillermonegrete.tts.imageprocessing

import android.content.Intent
import android.graphics.*
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics

/*
    Based on https://github.com/mtsahakis/MediaProjectionDemo/blob/master/src/com/mtsahakis/mediaprojectiondemo/ScreenCaptureImageActivity.java
 */

class ScreenImageCaptor(
    manager: MediaProjectionManager,
    metrics: DisplayMetrics,
    screenSize: Point,
    resultCode: Int,
    intent: Intent
) {

    private val mediaProjection = manager.getMediaProjection(resultCode, intent)
    private val density = metrics.densityDpi
    private var handler = Handler()

    private val width = screenSize.x
    private val height = screenSize.y
    private val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1)
    private val virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCap", width, height, density,
        VIRTUAL_DISPLAY_FLAGS, imageReader.surface, null, handler)

    private var imagesCaptured = false


    fun getImage(rect: Rect, callback: Callback){

        imageReader.setOnImageAvailableListener(ImageAvailableListener(rect, callback), handler)
        mediaProjection.registerCallback(MediaProjectionStopCallback(), handler)

    }

    private inner class ImageAvailableListener(
        private val rect: Rect,
        private val callback: Callback
    ): ImageReader.OnImageAvailableListener{

        override fun onImageAvailable(reader: ImageReader?) {
            if(imagesCaptured) return

            var image: Image? = null
            var bitmap: Bitmap? = null
            try {
                image = reader?.acquireLatestImage()
                if(image != null){
                    stopProjection()
                    imagesCaptured = true
                    bitmap = createBitmapFromImagePlane(image.planes[0])
                    val statusBarHeight = 0
                    val croppedBitmap = Bitmap.createBitmap(bitmap, rect.left, rect.top + statusBarHeight, rect.width(), rect.height() + statusBarHeight)
                    callback.onImageCaptured(croppedBitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                image?.close()
                bitmap?.recycle()
            }
        }

        private fun createBitmapFromImagePlane(plane: Image.Plane): Bitmap{

            val pixelStride = plane.pixelStride
            val rowPadding = plane.rowStride - pixelStride * width

            return Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888).apply {
                copyPixelsFromBuffer(plane.buffer)
            }
        }

    }

    private inner class MediaProjectionStopCallback: MediaProjection.Callback() {
        override fun onStop() {
            handler.post {
                virtualDisplay.release()
                imageReader.setOnImageAvailableListener(null, null)
                mediaProjection.unregisterCallback(this@MediaProjectionStopCallback)
            }
        }
    }

    private fun stopProjection(){
        handler.post {
            mediaProjection.stop()
        }
    }

    companion object {
        private const val VIRTUAL_DISPLAY_FLAGS =
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
    }

    interface Callback{
        fun onImageCaptured(image: Bitmap)
    }
}