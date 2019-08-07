package com.guillermonegrete.tts.imageprocessing

import android.graphics.Bitmap
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer
import com.guillermonegrete.tts.db.Words

/*
*   Uses Google Firebase Cloud service for text and language recognition. More info here: https://firebase.google.com/docs/ml-kit/android/recognize-text
*   So far only works with the first block(paragraph) of text detected, support for more than one block may be added later.
*
* */

class FirebaseCloudTextProcessor(private val detector: FirebaseVisionTextRecognizer) : ImageProcessingSource {

    override fun detectText(bitmap: Bitmap, callback: ImageProcessingSource.Callback) {
        val bitmapCopy = bitmap.copy(bitmap.config, true)
        val image = FirebaseVisionImage.fromBitmap(bitmapCopy)
        val result = detector.processImage(image)
            .addOnSuccessListener { firebaseVisionText ->

                // List of languages: https://firebase.google.com/docs/ml-kit/langid-support?hl=es-419
                val words = Parser().getLanguageAndText(firebaseVisionText)
                callback.onTextDetected(words.word, words.lang)
                bitmapCopy.recycle()
            }
            .addOnFailureListener {
                callback.onFailure()
                bitmapCopy.recycle()
            }
    }


    class Parser{

        fun getLanguageAndText(firebaseVisionText: FirebaseVisionText): Words {
            val rawText = firebaseVisionText.text
            val blocks = firebaseVisionText.textBlocks
            if(blocks.isNotEmpty()){
                val languages = blocks.first().recognizedLanguages
                if(languages.isNotEmpty()){
                    val rawLanguage = languages.first().languageCode
                    if (rawLanguage != null) {
                        val language = getISOLanguage(rawLanguage)
                        val text = if(isRTLLanguage(language)) reverseWordOrder(blocks.first()) else rawText
                        return Words(text, language, "")
                    }
                }
            }
            return Words("", "", "")
        }

        private fun getISOLanguage(raw: String) = if(raw == "iw") "he" else raw

        /*
        *   Support for right to left languages. Because Firebase returns LTR (left to right) we have to reverse the
        *   word order.
        *
        * */

        private fun isRTLLanguage(lang: String) =
            when(lang){
                "he" -> true
                else -> false
            }

        private fun reverseWordOrder(block: FirebaseVisionText.TextBlock) : String{
            val reversedWords = arrayListOf<String>()
            for(line in block.lines){
                for(word in line.elements.reversed()){
                    reversedWords.add(word.text)
                }
            }
            return reversedWords.joinToString(separator = " ")
        }
    }


    companion object {
        @Volatile private var instance: FirebaseCloudTextProcessor? = null

        fun getInstance(detector: FirebaseVisionTextRecognizer) =
            instance ?: synchronized(this){
                instance ?: FirebaseCloudTextProcessor(detector).also { instance = it }
            }
    }
}