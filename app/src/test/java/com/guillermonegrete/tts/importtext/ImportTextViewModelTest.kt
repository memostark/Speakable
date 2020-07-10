package com.guillermonegrete.tts.importtext

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.guillermonegrete.tts.MainCoroutineRule
import com.guillermonegrete.tts.data.source.FakeFileRepository
import com.guillermonegrete.tts.db.BookFile
import com.guillermonegrete.tts.getUnitLiveDataValue
import com.guillermonegrete.tts.importtext.visualize.io.FakeEpubFileManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ImportTextViewModelTest {

    private lateinit var viewModel: ImportTextViewModel

    private lateinit var fileRepository: FakeFileRepository

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

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
    fun load_files_and_show_loading_icon(){
        // Pause dispatcher so we can verify initial values
        mainCoroutineRule.pauseDispatcher()

        viewModel.loadRecentFiles()

        // Then progress indicator is shown
        assertTrue(getUnitLiveDataValue(viewModel.dataLoading))

        mainCoroutineRule.resumeDispatcher()

        // Then progress indicator is hidden
        assertFalse(getUnitLiveDataValue(viewModel.dataLoading))

        assertEquals(2, getUnitLiveDataValue(viewModel.files).size)

    }
}