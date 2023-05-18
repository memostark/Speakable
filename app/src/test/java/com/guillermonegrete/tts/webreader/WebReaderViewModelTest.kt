package com.guillermonegrete.tts.webreader

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.guillermonegrete.tts.MainCoroutineRule
import com.guillermonegrete.tts.TestThreadExecutor
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.data.source.ExternalLinksDataSource
import com.guillermonegrete.tts.data.source.FakeWordRepository
import com.guillermonegrete.tts.data.source.local.FakeExternalLinkSource
import com.guillermonegrete.tts.db.FakeWebLinkDAO
import com.guillermonegrete.tts.db.WebLink
import com.guillermonegrete.tts.getOrAwaitValue
import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetExternalLink
import com.guillermonegrete.tts.threading.TestMainThread
import com.guillermonegrete.tts.webreader.db.FakeNoteDAO
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.Calendar
import java.util.UUID

@ExperimentalCoroutinesApi
class WebReaderViewModelTest {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: WebReaderViewModel

    private lateinit var wordRepository: FakeWordRepository
    private lateinit var externalLinksSource: ExternalLinksDataSource
    private lateinit var webLinkDAO: FakeWebLinkDAO
    private lateinit var notesDAO: FakeNoteDAO

    @Before
    fun setup() {
        wordRepository = FakeWordRepository()
        val getTranslationInteractor =
            GetLangAndTranslation(TestThreadExecutor(), TestMainThread(), wordRepository)

        externalLinksSource = FakeExternalLinkSource()
        val getExternalLink =
            GetExternalLink(TestThreadExecutor(), TestMainThread(), externalLinksSource)

        webLinkDAO = FakeWebLinkDAO()
        notesDAO = FakeNoteDAO()
        viewModel = WebReaderViewModel(
            getTranslationInteractor,
            getExternalLink,
            wordRepository,
            webLinkDAO,
            notesDAO,
            mainCoroutineRule.dispatcher
        )

        mockkStatic(Jsoup::class)
    }

    // region Loading page tests
    @Test
    fun `Given no saved url link, when load page, then load success and new link`() = runTest {
        val url = "https://example.com"
        val link = WebLink(url)
        every { Jsoup.connect(url).get() } returns Jsoup.parse("<body>My document</body>")

        viewModel.loadDoc(url, link)
        advanceUntilIdle()

        val expected = PageInfo("My document", emptyList(), false)
        assertEquals(LoadResult.Success(expected), viewModel.page.getOrAwaitValue())
        assertEquals(link, viewModel.webLink.getOrAwaitValue())
    }

    @Test
    fun `Given saved url link, when load page, then load success and saved link`() = runTest {
        val url = "https://example.com"
        val link = WebLink(url, "en", id = 10)
        webLinkDAO.links.add(link)
        every { Jsoup.connect(url).get() } returns Jsoup.parse("<body>My document</body>")

        viewModel.loadDoc(url)
        advanceUntilIdle()

        val expected = PageInfo("My document", emptyList(), false)
        assertEquals(LoadResult.Success(expected), viewModel.page.getOrAwaitValue())
        assertEquals(link, viewModel.webLink.getOrAwaitValue())
    }

    @Test
    fun `Given connection error, when load page, then load error`() = runTest {
        val url = "https://example.com"
        val error = IOException()
        every { Jsoup.connect(url).get() } throws error

        viewModel.loadDoc(url)
        advanceUntilIdle()

        val result = viewModel.page.getOrAwaitValue()
        assertTrue(result is LoadResult.Error)
        assertTrue((result as LoadResult.Error).exception is IOException)
    }

    @Test
    fun `Given local file, when load page, then load success and saved link`() = runTest {
        loadLocalPage()
    }

    @Test
    fun `Given local file, when load from web and local again, then load success`() = runTest {
        // First load a local page
        loadLocalPage()

        // Then switch to web version
        every {
            Jsoup.connect("https://example.com").get()
        } returns Jsoup.parse("<body>My document</body>")
        viewModel.loadPageFromWeb()
        advanceUntilIdle()

        val expected = PageInfo("My document", emptyList(), false)
        assertEquals(LoadResult.Success(expected), viewModel.page.getOrAwaitValue())

        // Finally test loading local page
        viewModel.loadLocalPage()
        advanceUntilIdle()

        assertEquals(
            LoadResult.Success(PageInfo(localContent, emptyList(), true)),
            viewModel.page.getOrAwaitValue()
        )
    }

    // endregion

    // region Loading page tests

    @Test
    fun `Given no saved url link, when save link, then db link created`() = runTest {
        val url = "https://example.com"
        every { Jsoup.connect(url).get() } returns Jsoup.parse("<body>My document</body>")
        viewModel.loadDoc(url)
        advanceUntilIdle()

        val time = Calendar.getInstance()
        mockkStatic(Calendar::class)
        every { Calendar.getInstance() } returns time // so link's lastRead is saved with this time
        viewModel.saveWebLink()
        advanceUntilIdle()

        val savedLink = webLinkDAO.links.find { it.url == url }
        assertNotNull(savedLink)
        assertEquals(WebLink(url, "", lastRead = time), savedLink)
    }

    @Test
    fun `When save link folder, then db link updated`() = runTest {
        // Initial page load
        val url = "https://example.com"
        every { Jsoup.connect(url).get() } returns Jsoup.parse("<body>My document</body>")
        viewModel.loadDoc(url)
        advanceUntilIdle()

        // Create a folder
        val time = Calendar.getInstance()
        mockkStatic(Calendar::class)
        every { Calendar.getInstance() } returns time // so link's lastRead is saved with this time
        val uuid = UUID.randomUUID()
        viewModel.saveWebLinkFolder("", uuid, "text")
        advanceUntilIdle()

        // Verify link saved and uuid created
        val savedLink = webLinkDAO.links.find { it.url == url }
        assertNotNull(savedLink)
        assertEquals(WebLink(url, "", uuid = uuid, lastRead = time), savedLink)
    }

    @Test
    fun `When delete link folder, then uuid null`() = runTest {
        // Load local page because it has a folder and UUID
        loadLocalPage()

        // Delete folder
        viewModel.deleteLinkFolder("")
        advanceUntilIdle()

        // Verify uuid was deleted
        val updatedLink = webLinkDAO.links.find { it.url == "https://example.com" }
        assertNotNull(updatedLink)
        assertNull(updatedLink?.uuid)
    }

    // endregion

    private fun loadLocalPage() = runTest {
        val url = "https://example.com"
        val path = UUID.randomUUID()
        val link = WebLink(url, "en", id = 10, uuid = path)
        webLinkDAO.upsert(link)
        val dummyRoot = "test_root"
        viewModel.folderPath = dummyRoot
        val file = File("$dummyRoot\\" + path.toString(), "content.xml")
        every { Jsoup.parse(file, null) } returns Jsoup.parse("<body>My document</body>")

        viewModel.loadDoc(url)
        advanceUntilIdle()

        val expected = PageInfo(localContent, emptyList(), true)
        assertEquals(LoadResult.Success(expected), viewModel.page.getOrAwaitValue())
        assertEquals(link, viewModel.webLink.getOrAwaitValue())
    }

    companion object {
        val localContent = """
                            <html>
                             <head></head>
                             <body>
                              My document
                             </body>
                            </html>
                           """.trimIndent()
    }
}
