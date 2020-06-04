package com.guillermonegrete.tts.data.source

import com.guillermonegrete.tts.MainCoroutineRule
import com.guillermonegrete.tts.data.Result
import com.guillermonegrete.tts.data.source.remote.FakeWordDataSource
import com.guillermonegrete.tts.db.Words
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WordRepositoryTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var repository: WordRepository
    private lateinit var fakeRemoteSource: FakeWordDataSource
    private lateinit var fakeLocalSource: FakeWordDataSource

    @Before
    fun setUp(){

        fakeRemoteSource = FakeWordDataSource()
        fakeLocalSource = FakeWordDataSource()

        repository = WordRepository(fakeRemoteSource, fakeLocalSource)
    }

    @Test
    fun `Get translation from remote source`(){
        val remoteWord = Words("Hola", "en", "remote")
        fakeRemoteSource.addTranslation(remoteWord)

        val result = repository.getLanguageAndTranslation("Hola", "auto", "en")

        assertTrue(result is Result.Success)
        val word = (result as Result.Success).data
        assertEquals(remoteWord, word)
    }
}