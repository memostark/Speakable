package com.guillermonegrete.tts.data.remote

import com.guillermonegrete.tts.data.source.WordDataSource
import com.guillermonegrete.tts.data.source.remote.GooglePublicSource
import com.guillermonegrete.tts.db.Words
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch

class GooglePublicRequestTest {

    private lateinit var dataSource: GooglePublicSource
    private val latch = CountDownLatch(1)

    @Before
    fun setDataSource() {dataSource = GooglePublicSource.getInstance()}

    @Test
    fun translatorTest(){
        dataSource.getWordLanguageInfo("prueba", object: WordDataSource.GetWordCallback{
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
}