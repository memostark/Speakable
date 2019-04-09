package com.guillermonegrete.tts.imageprocessing

import android.graphics.Bitmap
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage

class FirebaseTextProcessor: ImageProcessingSource{

    override fun onDetectText(bitmap: Bitmap, callback: ImageProcessingSource.Callback) {
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        val detector = FirebaseVision.getInstance().onDeviceTextRecognizer

        val result = detector.processImage(image)
                .addOnSuccessListener { firebaseVisionText ->
                    identifyLanguage(firebaseVisionText.text, callback)
                }
                .addOnFailureListener {
                    callback.onFailure()
                }
    }

    private fun identifyLanguage(text: String, callback: ImageProcessingSource.Callback){
        val languageIdentifier = FirebaseNaturalLanguage.getInstance().languageIdentification
        languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener {
                    callback.onTextDetected(text, it)
                }
                .addOnFailureListener {
                    callback.onFailure()
                }
    }

    companion object {
        @Volatile private var instance: FirebaseTextProcessor? = null

        fun getInstance() =
            instance ?: synchronized(this){
                instance ?: FirebaseTextProcessor().also { instance = it }
            }
    }
}