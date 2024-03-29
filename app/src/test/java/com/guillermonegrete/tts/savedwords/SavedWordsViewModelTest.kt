package com.guillermonegrete.tts.savedwords

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.guillermonegrete.tts.MainCoroutineRule
import com.guillermonegrete.tts.data.source.FakeWordRepository
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SavedWordsViewModelTest {

    private lateinit var viewModel: SavedWordsViewModel

    private lateinit var wordRepository: FakeWordRepository

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule(UnconfinedTestDispatcher())

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val words = listOf(
        Words("gato", "es", "cat").apply { id = 1 },
        Words("hund", "de", "dog").apply { id = 2 },
        Words("azul", "es", "blue").apply { id = 3 }
    )

    @Before
    fun setupViewModel(){
        wordRepository = FakeWordRepository()
        wordRepository.addWords(*words.toTypedArray())

        viewModel = SavedWordsViewModel(wordRepository, UnconfinedTestDispatcher())
    }

    @Test
    fun load_all_words_from_repository() = runTest {
        val words = viewModel.wordsList.getOrAwaitValue()
        assertEquals(3, words.size)
    }

    @Test
    fun load_all_languages_from_repository() = runTest {
        val langs = viewModel.languagesList.getOrAwaitValue()
        assertEquals(2, langs.size)
    }

    @Test
    fun `Given saved word, then delete`() = runTest {
        viewModel.delete(words.first())
        assertEquals(listOf(words[1], words[2]), wordRepository.words)
    }
}