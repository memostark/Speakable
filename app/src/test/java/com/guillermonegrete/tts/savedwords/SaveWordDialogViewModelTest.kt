package com.guillermonegrete.tts.savedwords

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.guillermonegrete.tts.MainCoroutineRule
import com.guillermonegrete.tts.data.source.remote.FakeWordDataSource
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
class SaveWordDialogViewModelTest {

    private lateinit var viewModel: SaveWordDialogViewModel

    private val savedWord = Words("casa", "es", "house").apply { id = 1 }

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule(UnconfinedTestDispatcher())

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        val wordSource = FakeWordDataSource()
        wordSource.addTranslation(savedWord)

        viewModel = SaveWordDialogViewModel(wordSource)
    }

    @Test
    fun `When save, then insert emitted`() = runTest {
        val word = Words("nueva", "es", "new")
        viewModel.save(word)

        assertEquals(ResultType.Insert(word), viewModel.update.getOrAwaitValue())
    }

    @Test
    fun `When update, then updated emitted`() = runTest {
        val word = Words("casa", "es", "house new").apply { id = 1 }
        viewModel.update(word)

        assertEquals(ResultType.Update, viewModel.update.getOrAwaitValue())
    }
}
