package com.guillermonegrete.tts.data.db

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.db.WordsDAO
import com.guillermonegrete.tts.db.WordsDatabase
import com.guillermonegrete.tts.getLiveDataValue
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WordsDAOTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: WordsDatabase
    private lateinit var dao: WordsDAO

    @Before
    fun setUp(){
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().context,
            WordsDatabase::class.java).build()

        dao = database.wordsDAO()
    }

    @After
    fun close_db(){
        database.close()
    }

    @Test
    fun get_unique_languages_iso(){
        dao.insert(*initialWords.toTypedArray())

        val languages = getLiveDataValue(dao.languagesISO)

        val expectedLanguages = listOf("es", "de", "ca")
        println("expected: $expectedLanguages, actual: $languages")
        assertEquals(expectedLanguages, languages)
    }

    companion object{
        val initialWords = listOf(
            Words("Perro", "es", "dog"),
            Words("Gato", "es", "cat"),
            Words("Hund", "de", "dog"),
            Words("Gos", "ca", "dog"),
            Words("Katze", "de", "cat")
        )
    }
}