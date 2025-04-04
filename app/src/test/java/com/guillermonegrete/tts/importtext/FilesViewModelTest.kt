package com.guillermonegrete.tts.importtext

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.guillermonegrete.tts.MainCoroutineRule
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.data.source.FakeFileRepository
import com.guillermonegrete.tts.db.BookFile
import com.guillermonegrete.tts.importtext.visualize.io.FakeEpubFileManager
import com.guillermonegrete.tts.utils.deleteAllFolder
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.GregorianCalendar

@ExperimentalCoroutinesApi
class FilesViewModelTest {

    private lateinit var viewModel: FilesViewModel

    private lateinit var fileRepository: FakeFileRepository

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val files = listOf(
        BookFile("fake_uri", "Title 1", ImportedFileType.EPUB, id = 3, lastRead = GregorianCalendar(2013,1,1)),
        BookFile("fake_uri", "Title 2", ImportedFileType.EPUB, id = 4, lastRead = GregorianCalendar(2013,0,1))
    )

    @Before
    fun setUp(){
        fileRepository = FakeFileRepository()
        fileRepository.addFiles(*files.toTypedArray())

        viewModel = FilesViewModel(fileRepository, FakeEpubFileManager())

        mockkStatic(::deleteAllFolder)
    }

    @Test
    fun `When load files, then loading and success`() = runTest {
        viewModel.files.test {
            assertEquals(LoadResult.Loading, awaitItem())
            assertEquals(LoadResult.Success(files), awaitItem())
        }
    }

    @Test
    fun `When exception on loading files, then error result`() = runTest {
        val error = Exception("Error loading recent files")
        fileRepository.returnError = error

        viewModel.files.test {
            assertEquals(LoadResult.Loading, awaitItem())
            assertEquals(error, (awaitItem() as LoadResult.Error).throwable)
        }
    }

    @Test
    fun `Given saved book, then delete`() = runTest {
        viewModel.deleteFile(files.first())

        advanceUntilIdle()

        assertEquals(1, fileRepository.files.size)
    }
}
