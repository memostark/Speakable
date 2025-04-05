package com.guillermonegrete.tts.webreader

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.guillermonegrete.tts.MainCoroutineRule
import com.guillermonegrete.tts.TestThreadExecutor
import com.guillermonegrete.tts.common.models.Span
import com.guillermonegrete.tts.data.DialogState
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.data.Segment
import com.guillermonegrete.tts.data.Translation
import com.guillermonegrete.tts.data.preferences.SettingsRepository
import com.guillermonegrete.tts.data.source.FakeWordRepository
import com.guillermonegrete.tts.data.source.local.FakeExternalLinkSource
import com.guillermonegrete.tts.db.ExternalLink
import com.guillermonegrete.tts.db.FakeWebLinkDAO
import com.guillermonegrete.tts.db.WebLink
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.getOrAwaitValue
import com.guillermonegrete.tts.importtext.visualize.model.SplitPageSpan
import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetExternalLink
import com.guillermonegrete.tts.threading.TestMainThread
import com.guillermonegrete.tts.utils.deleteAllFolder
import com.guillermonegrete.tts.utils.makeDir
import com.guillermonegrete.tts.utils.writeToFile
import com.guillermonegrete.tts.webreader.db.FakeNoteDAO
import com.guillermonegrete.tts.webreader.db.Note
import com.guillermonegrete.tts.webreader.model.ModifiedNote
import com.guillermonegrete.tts.webreader.model.SplitParagraph
import com.guillermonegrete.tts.webreader.model.WordAndLinks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
    private lateinit var externalLinksSource: FakeExternalLinkSource
    private lateinit var webLinkDAO: FakeWebLinkDAO
    private lateinit var notesDAO: FakeNoteDAO

    @Before
    fun setup() {
        wordRepository = FakeWordRepository()
        val getTranslationInteractor =
            GetLangAndTranslation(TestThreadExecutor(), TestMainThread(), wordRepository)

        externalLinksSource = FakeExternalLinkSource()
        val getExternalLink =
            GetExternalLink(TestThreadExecutor(), TestMainThread(), externalLinksSource, mainCoroutineRule.dispatcher)

        webLinkDAO = FakeWebLinkDAO()
        notesDAO = FakeNoteDAO()
        val settings = mockk<SettingsRepository>(relaxed = true)
        every { settings.showSavedWords() } returns flowOf(true)
        val url = "https://example.com"
        viewModel = WebReaderViewModel(
            url,
            getTranslationInteractor,
            getExternalLink,
            wordRepository,
            webLinkDAO,
            notesDAO,
            settings,
            mainCoroutineRule.dispatcher,
            mainCoroutineRule.dispatcher,
        )

        mockkStatic(Jsoup::class)

        mockFileUtils()
    }

    private fun mockFileUtils() {
        mockkStatic(::makeDir, ::deleteAllFolder, ::writeToFile)
        every { makeDir(any()) } returns true
        every { deleteAllFolder(any()) } returns true
        every { writeToFile(any(), any()) } returns Unit
    }

    // region Loading page tests
    @Test
    fun `Given no saved url link, when load page, then load success and new link`() = runTest {
        val url = "https://example.com"
        val link = WebLink(url)
        every { Jsoup.connect(url).get() } returns Jsoup.parse("<body>My document</body>")

        viewModel.loadDoc(url, link)
        advanceUntilIdle()

        val expected = PageInfo("My document", false)
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

        val expected = PageInfo("My document", false)
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
        assertTrue((result as LoadResult.Error).throwable is IOException)
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

        val expected = PageInfo("My document", false)
        assertEquals(LoadResult.Success(expected), viewModel.page.getOrAwaitValue())

        // Finally test loading local page
        viewModel.loadLocalPage()
        advanceUntilIdle()

        assertEquals(
            LoadResult.Success(PageInfo(localContent, true)),
            viewModel.page.getOrAwaitValue()
        )
    }

    @Test
    fun `Given local file, when switch to web, then error`() = runTest {
        loadLocalPage()
        val error = IOException("Error")
        every { Jsoup.connect("https://example.com") } throws error

        viewModel.loadPageFromWeb()
        advanceUntilIdle()

        val result = viewModel.page.getOrAwaitValue() as LoadResult.Error
        assertTrue(result.throwable is IOException)
    }

    @Test
    fun `Given web page, when switch to local, then error`() = runTest {
        loadLocalPage()

        viewModel.loadPageFromWeb()
        advanceUntilIdle()

        every { Jsoup.parse(any<File>(), null) } throws IOException()
        viewModel.loadLocalPage()
        advanceUntilIdle()

        val result = viewModel.page.getOrAwaitValue() as LoadResult.Error
        assertTrue(result.throwable is IOException)
    }
    // endregion

    // region Paragraph tests

    @Test
    fun `Given two texts, when create paragraph, then two split paragraphs`() {
        val result = viewModel.createParagraphs(listOf("First paragraph. Second sentence", "Second paragraph text"))
        val expected = listOf(
            SplitParagraph("First paragraph. Second sentence", listOf(Span(start=0, end=17), Span(start=17, end=32)), listOf("First paragraph. ", "Second sentence")),
            SplitParagraph("Second paragraph text", listOf(Span(start=0, end=21)), listOf("Second paragraph text"))
        )
        assertEquals(expected, result)
    }

    @Test
    fun `Given two paragraphs, when translate paragraph pos 1, then load and success`() = runTest {
        setParagraphs(1)

        viewModel.translateParagraph()

        assertEquals(true, viewModel.paragraphState.value.paragraph?.isLoading)
        advanceUntilIdle()
        assertEquals(secondParagraphTrans, viewModel.paragraphState.value.paragraph?.translation)
    }

    @Test
    fun `Given no translation, when translate paragraph pos 1, then error`() = runTest {
        // setup translation and paragraphs
        val text = "Second paragraph text"
        viewModel.createParagraphs(listOf("First paragraph. Second sentence", text))
        viewModel.paragraphSelected(1)

        viewModel.translateParagraph()

        assertEquals(true, viewModel.paragraphState.value.paragraph?.isLoading)
        advanceUntilIdle()
        val resultError = viewModel.paragraphState.value.paragraph
        assertEquals(null, resultError?.translation)
        assertEquals(false, resultError?.isLoading)
    }

    // endregion

    // region Sentence tests
    @Test
    fun `Given two paragraphs, when translate sentence 1 paragraph 0, then load and success`() = runTest {
        setSentences()

        viewModel.dialogState.test {
            assertEquals(WebReaderViewModel.UiDialogState(), awaitItem())

            viewModel.translateSentence(0, 1)
            assertEquals(true, awaitItem().isLoading)

            val expected = Sentence(sentenceTrans.definition, 0)
            assertEquals(awaitItem().sentence, expected)
        }
    }

    @Test
    fun `Given already translated sentence, when translate again, then load cache and success`() = runTest {
        loadLocalPage() // need the language set for the cache to work
        setSentences()

        viewModel.dialogState.test {
            assertEquals(WebReaderViewModel.UiDialogState(isPageSaved = true), awaitItem())

            viewModel.translateSentence(0, 1)
            assertEquals(true, awaitItem().isLoading)

            val expected = Sentence(sentenceTrans.definition, 0)
            assertEquals(expected, awaitItem().sentence)

            // translate again with cache and expect no events because state didn't change
            viewModel.translateSentence(0, 1)
        }
    }

    @Test
    fun `Given translated paragraph, when find sentence, then return second sentence`() = runTest {
        setParagraphs()

        viewModel.translateParagraph()
        advanceUntilIdle()

        val result = viewModel.findSelectedSentence(0, 18) // index 18 is in the second sentence
        assertEquals(SplitPageSpan(Span(start=17, end=32), Span(start=17, end=35)), result)
    }

    @Test
    fun `When find sentence index out of bound, then return null`() = runTest {
        setParagraphs()

        viewModel.translateParagraph()
        advanceUntilIdle()

        val result = viewModel.findSelectedSentence(0, 36) // index 18 is in the second sentence
        assertEquals(null, result)
    }

    /**
     * Sets up the paragraphs and their translations
     */
    private fun setParagraphs(index: Int = 0) {
        val first = Translation(listOf(Segment("First translation", "First paragraph. "), Segment("Second translation", "Second sentence")), "en")
        wordRepository.addTranslation(first, secondParagraphTrans)
        viewModel.createParagraphs(listOf("First paragraph. Second sentence", "Second paragraph text"))
        runTest {
            viewModel.paragraphState.test {
                assertEquals(WebReaderViewModel.ParagraphUiState(), awaitItem()) // initial value
                viewModel.paragraphSelected(index)
                val expected = WebReaderViewModel.ParagraphUiState(paragraph = SelectedParagraph(index))
                assertEquals(expected, awaitItem())
            }
        }
    }

    private fun setSentences() {
        val expectedTranslation = Translation(listOf(Segment(sentenceTrans.definition, sentenceTrans.word)), sentenceTrans.lang)
        wordRepository.addTranslation(expectedTranslation)
        viewModel.createParagraphs(listOf("First paragraph. Second sentence", "Second paragraph text"))
    }

    // endregion

    // region Translation tests

    @Test
    fun `Given saved word, when translate text, then word dialog`() = runTest {
        val word = Words("Hola", "es", "Hello").apply { id = 3 }
        wordRepository.addWords(word)

        viewModel.dialogState.test {
            assertEquals(WebReaderViewModel.UiDialogState(), awaitItem())

            viewModel.setSavedWord(3, Span(0, 1))
            assertEquals(true, awaitItem().isLoading)
            val wordDialog = awaitItem().dialogState as DialogType.SavedWord
            assertEquals(word, wordDialog.word)
        }
    }

    @Test
    fun `Give no saved words, when translate text, then translation dialog`() = runTest {
        val word = Words("Hola", "es", "Hello")
        val expectedTranslation = Translation(listOf(Segment(word.definition, word.word)), word.lang)
        wordRepository.addTranslation(expectedTranslation)

        viewModel.dialogState.test {
            assertEquals(WebReaderViewModel.UiDialogState(), awaitItem())
            viewModel.translateText("Hola", Span(0, 1), false, false)

            assertEquals(true, awaitItem().isLoading)
            val result = awaitItem().dialogState as DialogType.Translation
            assertEquals(word.definition, result.translation.translation)
        }
    }

    @Test
    fun `Given no saved words and translation, when translate text, then error`() = runTest {
        viewModel.dialogState.test {
            assertEquals(WebReaderViewModel.UiDialogState(), awaitItem())

            viewModel.translateText("Hola", Span(0, 1), false, false)

            assertEquals(true, awaitItem().isLoading)
            val result = awaitItem().error
            assertEquals("Translation not found for: Hola", result)
        }
    }

    @Test
    fun `Given no saved word, when translate word in sentence, then word info updated`() = runTest {
        val translation = Translation("Hola", "es", "Hello")
        wordRepository.addTranslation(translation)
        setSentences()

        viewModel.dialogState.test {
            assertEquals(WebReaderViewModel.UiDialogState(), awaitItem())

            viewModel.translateSentence(0, 1)
            assertEquals(true, awaitItem().isLoading)
            assertNotNull(awaitItem().sentence)

            viewModel.translateWordInSentence("Hola", Span(0, 2))

            assertEquals(true, awaitItem().isWordLoading)
            val state = awaitItem().dialogState as DialogType.Translation
            assertEquals(translation.translatedText, state.translation.translation)
        }
    }

    // endregion

    @Test
    fun `When word clicked, then return external links`() = runTest {
        val links = listOf(
            ExternalLink("Random site", "link", "es"),
            ExternalLink("Random site 2", "link 2", "es"),
        )
        externalLinksSource.addLinks(*links.toTypedArray())
        loadLocalPage()
        viewModel.setLanguage("es")

        viewModel.linksForWord.test {
            assertEquals(DialogState.Empty, awaitItem())
            val word = "hola"
            viewModel.onWordClicked(word, 0)
            val expected = DialogState.Success(WordAndLinks(word, links))
            assertEquals(expected, awaitItem())
        }
    }


    // region Saving link and folder tests

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

    // region Updating notes

    @Test
    fun `When save note, then note updated emitted`() = runTest {
        // Notes only available when local page exists
        loadLocalPage()

        viewModel.updatedNote.test {
            viewModel.saveNote("original text", "new note text", Span(4, 9), 9, "")
            advanceUntilIdle()

            // Verify uuid was deleted
            val newNote = Note("new note text", "original text", 4, 5, "", 10, null, 9)
            val expected = ModifiedNote.Update(newNote)
            assertEquals(expected, awaitItem())
            val note = notesDAO.notes.first()
            assertEquals(newNote, note)
        }
    }

    @Test
    fun `When delete note, then note deleted emitted`() = runTest {
        // Notes only available when local page exists
        loadLocalPage()
        val noteId = 9L
        notesDAO.notes.add(Note("saved text", "original text", 0, 4, "", 10, null, noteId))

        viewModel.updatedNote.test {
            viewModel.deleteNote(noteId)
            advanceUntilIdle()

            val expected = ModifiedNote.Delete(noteId)
            assertEquals(expected, awaitItem())
            val note = notesDAO.notes.firstOrNull()
            assertNull(note)
        }
    }

    // endregion

    private fun loadLocalPage() = runTest {
        val url = "https://example.com"
        val path = UUID.randomUUID()
        val link = WebLink(url, language = "en", id = 10, uuid = path)
        webLinkDAO.addLinks(link)
        val dummyRoot = "test_root"
        viewModel.folderPath = dummyRoot
        val file = File("$dummyRoot\\" + path.toString(), "content.xml")
        every { Jsoup.parse(file, null) } returns Jsoup.parse("<body>My document</body>")

        viewModel.loadDoc(url)
        advanceUntilIdle()

        val expected = PageInfo(localContent, true)
        assertEquals(LoadResult.Success(expected), viewModel.page.getOrAwaitValue())
        assertEquals(link, viewModel.webLink.getOrAwaitValue())
        viewModel.dialogState.test {
            assertEquals(WebReaderViewModel.UiDialogState(isPageSaved = true), awaitItem())
        }
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

        val sentenceTrans = Words("Second sentence", "en", "Imagine this is translated")

        val secondParagraphTrans = Translation(listOf(Segment( "Imagine this is translated", "Second paragraph text")), "EN")
    }
}
