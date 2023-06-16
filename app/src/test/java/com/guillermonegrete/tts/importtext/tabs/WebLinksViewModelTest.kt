package com.guillermonegrete.tts.importtext.tabs

import com.guillermonegrete.tts.MainCoroutineRule
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.db.FakeWebLinkDAO
import com.guillermonegrete.tts.db.WebLink
import com.guillermonegrete.tts.utils.deleteAllFolder
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
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
        viewModel.getRecentLinks()
        Assert.assertEquals(LoadResult.Loading, viewModel.uiState.value)

        advanceUntilIdle()

        Assert.assertEquals(LoadResult.Success(files), viewModel.uiState.value)
    }

    @Test
    fun `Given saved link, then delete`() = runTest {
        viewModel.delete(files.first(), "")

        advanceUntilIdle()

        Assert.assertEquals(1, webLinkDAO.links.size)
    }
}
