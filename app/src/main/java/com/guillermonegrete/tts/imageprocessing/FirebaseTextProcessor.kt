package com.guillermonegrete.tts.imageprocessing

import android.graphics.Bitmap
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import timber.log.Timber

class FirebaseTextProcessor: ImageProcessingSource{

    override fun detectText(bitmap: Bitmap, callback: ImageProcessingSource.Callback) {
        val bitmapCopy = bitmap.copy(bitmap.config, true)
        val image = InputImage.fromBitmap(bitmapCopy, 0)
        val detector = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        detector.process(image)
                .addOnSuccessListener { firebaseVisionText ->
                    identifyLanguage(firebaseVisionText.text, callback)
                    bitmapCopy.recycle()
                }
                .addOnFailureListener {
                    Timber.e(it, "Error detecting text")
                    callback.onFailure(it)
                    bitmapCopy.recycle()
                }
    }

    private fun identifyLanguage(text: String, callback: ImageProcessingSource.Callback){
        val languageIdentifier = FirebaseNaturalLanguage.getInstance().languageIdentification
        languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener {
                    callback.onTextDetected(text, it)
                }
                .addOnFailureListener {
                    Timber.e(it, "Error identifying language")
                    callback.onFailure(it)
                }
    }
}