package com.guillermonegrete.tts.imageprocessing

import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.firebase.ml.vision.text.RecognizedLanguage
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.*


class FirebaseCloudTextProcessorTest {

    @Test
    fun parseHebrewLanguageAndText(){
        val hebrewText = createHebrewFirebaseVisionText()
        val word = FirebaseCloudTextProcessor.Parser().getLanguageAndText(hebrewText)
        assertEquals("he", word.lang)
        assertEquals("מה עדיף לך , הכסף שחסכת או שלוש", word.word)
    }

    @Test
    fun parseEnglishLanguageAndText(){
        val englishText = createEnglishFirebaseVisionText()
        val word = FirebaseCloudTextProcessor.Parser().getLanguageAndText(englishText)
        assertEquals("en", word.lang)
        assertEquals("Like a rolling stone", word.word)
    }

    private fun createHebrewFirebaseVisionText() : FirebaseVisionText{
        val recognizedLanguage = mock(RecognizedLanguage::class.java)
        `when`(recognizedLanguage.languageCode).thenReturn("iw") // Hebrew old language code
        val languages = arrayListOf(recognizedLanguage)

        val elements = listOf(
            FirebaseVisionText.Element("שלוש", null, languages, null),
            FirebaseVisionText.Element("או", null, languages, null),
            FirebaseVisionText.Element("שחסכת", null, languages, null),
            FirebaseVisionText.Element("הכסף", null, languages, null),
            FirebaseVisionText.Element(",", null, languages, null),
            FirebaseVisionText.Element("לך", null, languages, null),
            FirebaseVisionText.Element("עדיף", null, languages, null),
            FirebaseVisionText.Element("מה", null, languages, null)
        )

        val line = FirebaseVisionText.Line("שלושאו שחסכת הכסף , לך עדיף מה", null, languages, elements, null)
        val lines = listOf(line)
        val block = FirebaseVisionText.TextBlock("שלושאו שחסכת הכסף , לך עדיף מה", null, languages, lines, null)
        val blocks = listOf(block)

        return FirebaseVisionText("שלושאו שחסכת הכסף , לך עדיף מה", blocks)
    }

    private fun createEnglishFirebaseVisionText(): FirebaseVisionText{
        val recognizedLanguage = mock(RecognizedLanguage::class.java)
        `when`(recognizedLanguage.languageCode).thenReturn("en") // Hebrew old language code
        val languages = arrayListOf(recognizedLanguage)

        val block = FirebaseVisionText.TextBlock("Like a rolling stone", null, languages, ArrayList<FirebaseVisionText.Line>(), null)
        val blocks = listOf(block)

        return FirebaseVisionText("Like a rolling stone", blocks)
    }

}