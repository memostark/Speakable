package com.guillermonegrete.tts.data.source.remote

import com.guillermonegrete.tts.data.Segment
import com.guillermonegrete.tts.data.Translation
import com.guillermonegrete.tts.db.Words
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class GooglePublicSourceTest {

    @MockK
    private lateinit var googleApi: GooglePublicAPI
    private lateinit var dataSource: GooglePublicSource

    @Before
    fun setUp(){
        MockKAnnotations.init(this)
        dataSource = GooglePublicSource(googleApi)
    }

    @Test
    fun `Given a response with one sentence, response is processed, then word result`(){
        val selectedText = "decepción de ese pobre"
        val response = GoogleTranslateResponse(listOf(Sentence( "disappointment of that poor", "decepción de ese pobre")), "es")

        val resultWord = dataSource.processText(response, selectedText)
        val expectedWord = Words(selectedText, "es", "disappointment of that poor")

        assertWordEquals(expectedWord, resultWord)
    }

    @Test
    fun `Given a response with multiple sentences, response is processed, then word result`(){
        val selectedText = "decepción de ese pobre"
        val response = GoogleTranslateResponse(listOf(
            Sentence("First sentence ", "Primera Oración"),
            Sentence("Second sentence", "Segunda oración")
        ), "es")

        val resultWord = dataSource.processText(response, selectedText)
        val expectedWord = Words(selectedText, "es", "First sentence Second sentence")

        assertWordEquals(expectedWord, resultWord)
    }

    @Test
    fun `Given multi sentence text, when getTranslation(), then return translation`(){
        // The google translate api removes the space betwwen the sentences in the original (unless there is a line break)
        // This test is for making sure that space is added again.
        val selectedText = "La primer oración del texto. Y la segunda oración del texto.\nY la tercer oración."
        val response = GoogleTranslateResponse(
            listOf(
                Sentence( "The first sentence of the text. ", "La primer oración del texto."),
                Sentence( "And the second sentence of the text.\n", "Y la segunda oración del texto.\n"),
                Sentence( "And the third sentence.", "Y la tercer oración."),
            ), "es")

        every { googleApi.getWord(selectedText, "es", "en").execute() } returns Response.success(response)
        val result = dataSource.getTranslation(selectedText, "es", "en")
        val expectedTranslation = Translation(
            listOf(
                Segment("The first sentence of the text. ", "La primer oración del texto. "),
                Segment( "And the second sentence of the text.\n", "Y la segunda oración del texto.\n"),
                Segment( "And the third sentence.", "Y la tercer oración."),
            ), "es")

        assertEquals(expectedTranslation, result)
    }

    private fun assertWordEquals(expected: Words, actual: Words){
        assertEquals(expected.word, actual.word)
        assertEquals(expected.lang, actual.lang)
        assertEquals(expected.definition, actual.definition)
    }
}
