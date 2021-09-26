package com.guillermonegrete.tts.data.source

import com.guillermonegrete.tts.MainCoroutineRule
import com.guillermonegrete.tts.data.Result
import com.guillermonegrete.tts.data.Segment
import com.guillermonegrete.tts.data.Translation
import com.guillermonegrete.tts.data.source.remote.FakeWordDataSource
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

    private lateinit var fakeTranslator: FakeTranslatorSource

    @Before
    fun setUp(){

        fakeRemoteSource = FakeWordDataSource()
        fakeLocalSource = FakeWordDataSource()

        fakeTranslator = FakeTranslatorSource()

        repository = WordRepository(fakeLocalSource, fakeTranslator)
    }

    @Test
    fun `Get translation from remote source`(){
        val translation = Translation(listOf(Segment("remote", "Hola")), "en")
        fakeTranslator.addTranslation(translation)

        val result = repository.getTranslation("Hola", "auto", "en")

        assertTrue(result is Result.Success)
        val word = (result as Result.Success).data
        assertEquals(translation, word)
    }
}