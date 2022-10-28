package com.guillermonegrete.tts.webreader

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.guillermonegrete.tts.MainCoroutineRule
import com.guillermonegrete.tts.MainDispatcherRule
import com.guillermonegrete.tts.TestThreadExecutor
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.data.source.ExternalLinksDataSource
import com.guillermonegrete.tts.data.source.FakeWordRepository
import com.guillermonegrete.tts.data.source.local.FakeExternalLinkSource
import com.guillermonegrete.tts.db.FakeWebLinkDAO
import com.guillermonegrete.tts.getOrAwaitValue
import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetExternalLink
import com.guillermonegrete.tts.threading.TestMainThread
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WebReaderViewModelTest {

    /*@ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()*/
    @ExperimentalCoroutinesApi
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: WebReaderViewModel

    private lateinit var wordRepository: FakeWordRepository
    private lateinit var externalLinksSource: ExternalLinksDataSource
    private lateinit var webLinkDAO: FakeWebLinkDAO

    @Before
    fun setup(){
        wordRepository = FakeWordRepository()
        val getTranslationInteractor = GetLangAndTranslation(TestThreadExecutor(), TestMainThread(), wordRepository)

        externalLinksSource = FakeExternalLinkSource()
        val getExternalLink = GetExternalLink(TestThreadExecutor(), TestMainThread(), externalLinksSource)

        webLinkDAO = FakeWebLinkDAO()
        viewModel = WebReaderViewModel(getTranslationInteractor, getExternalLink, wordRepository, webLinkDAO)

        mockkStatic(Jsoup::class)
    }

    @Test
    fun `Given valid url, when load page, then load success`() = runTest{
        val url = "https://example.com"
        every { Jsoup.connect(url).get() } returns Document(url)
        viewModel.loadDoc(url)

        assertEquals(LoadResult.Success("Page content"), viewModel.page.getOrAwaitValue())

    }
}
