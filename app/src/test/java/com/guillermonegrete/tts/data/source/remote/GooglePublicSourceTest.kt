package com.guillermonegrete.tts.data.source.remote

import com.guillermonegrete.tts.db.Words
import org.junit.Assert.assertEquals
import org.junit.Test

class GooglePublicSourceTest {

    private val dataSource: GooglePublicSource = GooglePublicSource()

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

    private fun assertWordEquals(expected: Words, actual: Words){
        assertEquals(expected.word, actual.word)
        assertEquals(expected.lang, actual.lang)
        assertEquals(expected.definition, actual.definition)
    }
}
