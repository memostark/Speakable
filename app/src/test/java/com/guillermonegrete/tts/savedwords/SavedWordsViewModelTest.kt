package com.guillermonegrete.tts.savedwords

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.android.architecture.blueprints.todoapp.MainCoroutineRule
import com.guillermonegrete.tts.data.source.FakeWordRepository
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.getUnitLiveDataValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
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
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setupViewModel(){
        wordRepository = FakeWordRepository()
        val word1 = Words("gato", "es", "cat").apply { id = 1 }
        val word2 = Words("hund", "de", "dog").apply { id = 2 }
        val word3 = Words("azul", "es", "blue").apply { id = 3 }
        wordRepository.addWords(word1, word2, word3)

        viewModel = SavedWordsViewModel(wordRepository, Dispatchers.Unconfined)
    }

    @Test
    fun load_all_tasks_from_repository(){
        viewModel.getWords()

        val words = getUnitLiveDataValue(viewModel.wordsList)
        assertEquals(3, words.size)
    }

    @Test
    fun load_all_languages_from_repository(){
        runBlockingTest {
            viewModel.getLanguages()

            val langs = getUnitLiveDataValue(viewModel.languagesList)
            assertEquals(2, langs.size)
        }
    }
}