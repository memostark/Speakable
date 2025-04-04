package com.guillermonegrete.tts.importtext.tabs

import app.cash.turbine.test
import com.guillermonegrete.tts.MainCoroutineRule
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.db.FakeWebLinkDAO
import com.guillermonegrete.tts.db.WebLink
import com.guillermonegrete.tts.utils.deleteAllFolder
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.GregorianCalendar
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class WebLinksViewModelTest {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var webLinkDAO: FakeWebLinkDAO

    private lateinit var viewModel: WebLinksViewModel

    private val files = listOf(
        WebLink("fake_url_saved", id = 1, lastRead = GregorianCalendar(2013,0,1), uuid = UUID.randomUUID()),
        WebLink("another_fake_url", id = 2, lastRead = GregorianCalendar(2013,0,1)),
    )

    @Before
    fun setUp(){
        webLinkDAO = FakeWebLinkDAO()
        webLinkDAO.addLinks(*files.toTypedArray())

        viewModel = WebLinksViewModel(webLinkDAO)

        mockkStatic(::deleteAllFolder)
    }

    @Test
    fun `When get links, then loading and success`() = runTest {
        viewModel.uiState.test {
            assertEquals(LoadResult.Loading, awaitItem())
            assertEquals(LoadResult.Success(files), awaitItem())
        }
    }

    @Test
    fun `When exception on loading files, then error result`() = runTest {
        val error = Exception("Error loading recent files")
        webLinkDAO.returnError = error

        viewModel.uiState.test {
            assertEquals(LoadResult.Loading, awaitItem())
            assertEquals(error, (awaitItem() as LoadResult.Error).throwable)
        }
    }

    @Test
    fun `Given saved link, then delete`() = runTest {
        viewModel.delete(files.first(), "")

        advanceUntilIdle()

        assertEquals(1, webLinkDAO.links.size)
    }
}
