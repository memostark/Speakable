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
import java.io.IOException

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
    fun setup(){
        wordRepository = FakeWordRepository()
        val getTranslationInteractor = GetLangAndTranslation(TestThreadExecutor(), TestMainThread(), wordRepository)

        externalLinksSource = FakeExternalLinkSource()
        val getExternalLink = GetExternalLink(TestThreadExecutor(), TestMainThread(), externalLinksSource)

        webLinkDAO = FakeWebLinkDAO()
        notesDAO = FakeNoteDAO()
        viewModel = WebReaderViewModel(getTranslationInteractor, getExternalLink, wordRepository, webLinkDAO, notesDAO, mainCoroutineRule.dispatcher)

        mockkStatic(Jsoup::class)
    }

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
        val link = WebLink(url, "en", id=10)
        webLinkDAO.upsert(link)
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
}
