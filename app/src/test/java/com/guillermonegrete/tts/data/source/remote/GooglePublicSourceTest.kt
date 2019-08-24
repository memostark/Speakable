package com.guillermonegrete.tts.data.source.remote

import com.guillermonegrete.tts.data.source.WordDataSource
import com.guillermonegrete.tts.db.Words
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.CountDownLatch

class GooglePublicSourceTest {

    private val dataSource: GooglePublicSource = GooglePublicSource()
    private val latch = CountDownLatch(1)

    fun translatorTest(){
        dataSource.getWordLanguageInfo("prueba", "auto", "en", object: WordDataSource.GetWordCallback{
            override fun onWordLoaded(word: Words?) {
                latch.countDown()
            }

            override fun onDataNotAvailable() {
                latch.countDown()
            }

        })

        try {
            latch.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }

    @Test
    fun process_request_with_auto_detect(){
        val selectedText = "decepción de ese pobre"
        val responseBodyText = "[[[\"disappointment of that poor\",\"decepción de ese pobre\",null,null,3]],null,\"es\",null,null,null,0.97732538,null,[[\"es\"],null,[0.97732538],[\"es\"]]]"

        val resultWord = dataSource.processText(responseBodyText, selectedText)
        val expectedWord = Words(selectedText, "es", "disappointment of that poor")

        assertWordEquals(expectedWord, resultWord)
    }

    @Test
    fun process_request_with_set_language(){
        val selectedText = "decepción de ese pobre"
        val responseBodyText = "[[[\"disappointment of that poor\",\"decepción de ese pobre\",null,null,3]],null,\"es\"]"

        val resultWord = dataSource.processText(responseBodyText, selectedText)
        val expectedWord = Words(selectedText, "es", "disappointment of that poor")

        assertWordEquals(expectedWord, resultWord)
    }

    private fun assertWordEquals(expected: Words, actual: Words){
        assertEquals(expected.word, actual.word)
        assertEquals(expected.lang, actual.lang)
        assertEquals(expected.definition, actual.definition)
    }
}