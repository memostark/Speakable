package com.guillermonegrete.tts.importtext

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.guillermonegrete.tts.MainCoroutineRule
import com.guillermonegrete.tts.data.source.FakeFileRepository
import com.guillermonegrete.tts.db.BookFile
import com.guillermonegrete.tts.getOrAwaitValue
import com.guillermonegrete.tts.importtext.visualize.io.FakeEpubFileManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@FlowPreview
@ExperimentalCoroutinesApi
class ImportTextViewModelTest {

    private lateinit var viewModel: ImportTextViewModel

    private lateinit var fileRepository: FakeFileRepository

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule(UnconfinedTestDispatcher())

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp(){
        fileRepository = FakeFileRepository()
        val files1= BookFile("fake_uri", "Title 1", ImportedFileType.EPUB, id = 3)
        val files2= BookFile("fake_uri", "Title 1", ImportedFileType.EPUB, id = 4)

        fileRepository.addTasks(files1, files2)

        viewModel = ImportTextViewModel(fileRepository, FakeEpubFileManager())
    }

    @Test
    fun load_files() = runTest {
        // TODO find how to test the status of the loading icon as well

        val files = viewModel.files

        assertEquals(2, files.getOrAwaitValue().size)
    }
}
