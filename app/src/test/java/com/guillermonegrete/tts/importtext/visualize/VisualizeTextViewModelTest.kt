package com.guillermonegrete.tts.importtext.visualize

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.guillermonegrete.tts.MainCoroutineRule
import com.guillermonegrete.tts.TestThreadExecutor
import com.guillermonegrete.tts.common.models.Span
import com.guillermonegrete.tts.data.Segment
import com.guillermonegrete.tts.data.Translation
import com.guillermonegrete.tts.data.preferences.FakeSettingsRepository
import com.guillermonegrete.tts.data.source.FakeFileRepository
import com.guillermonegrete.tts.data.source.FakeWordRepository
import com.guillermonegrete.tts.db.BookFile
import com.guillermonegrete.tts.getOrAwaitValue
import com.guillermonegrete.tts.getUnitLiveDataValue
import com.guillermonegrete.tts.importtext.ImportedFileType
import com.guillermonegrete.tts.importtext.epub.*
import com.guillermonegrete.tts.importtext.visualize.model.SplitPageSpan
import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation
import com.guillermonegrete.tts.threading.TestMainThread
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeoutException

@ExperimentalCoroutinesApi
class VisualizeTextViewModelTest {

    @get:Rule
    var mInstantTaskExecutorRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var viewModel: VisualizeTextViewModel

    @Mock private lateinit var epubParser: EpubParser
    @Mock private lateinit var fileReader: DefaultZipFileReader
    private lateinit var fileRepository: FakeFileRepository
    private lateinit var wordRepository: FakeWordRepository
    private lateinit var settingsRepository: FakeSettingsRepository

    @Mock private lateinit var pageSplitter: PageSplitter

    private val bookFile = BookFile(
        "empty_uri",
        "Title",
        ImportedFileType.EPUB,
        folderPath = "random_path",
        id = 1
    )

    @Before
    fun setUp(){
        MockitoAnnotations.openMocks(this)

        fileRepository = FakeFileRepository()
        wordRepository = FakeWordRepository()
        settingsRepository = FakeSettingsRepository()

        val getTranslationInteractor = GetLangAndTranslation(TestThreadExecutor(), TestMainThread(), wordRepository)

        viewModel = VisualizeTextViewModel(epubParser, settingsRepository, fileRepository, getTranslationInteractor, mainCoroutineRule.dispatcher)
        viewModel.pageSplitter = pageSplitter
        viewModel.fileReader = fileReader

        bookFile.apply {
            chapter = 0
            page = 0
        }
    }

    @Test
    fun parse_book_and_show_loading_icon() = runTest {

        // Epub parsing
        `when`(epubParser.parseBook(fileReader)).thenReturn(DEFAULT_BOOK)
        viewModel.parseEpub()

        // Then progress indicator is shown
        assertTrue(viewModel.dataLoading.getOrAwaitValue())

        advanceUntilIdle()

        // Then progress indicator is hidden
        assertFalse(viewModel.dataLoading.getOrAwaitValue())

        val resultBook = viewModel.book.getOrAwaitValue()
        assertEquals(DEFAULT_BOOK, resultBook)
    }

    @Test
    fun parse_epub_with_multiple_pages_chapter(){
        val pages = listOf("This shouldn't be here", "This should be here")
        `when`(pageSplitter.getPages()).thenReturn(pages)
        runBlocking { `when`(epubParser.getChapterBodyTextFromPath("ch1.html", fileReader)).thenReturn(DEFAULT_CHAPTER)}

        parse_book(DEFAULT_BOOK)

        val resultPages = getUnitLiveDataValue(viewModel.pages).getContentIfNotHandled()
        assertEquals(pages, resultPages)
        assertEquals(2, viewModel.pagesSize)
        assertEquals(5, viewModel.spineSize)
    }

    @Test
    fun parse_epub_with_one_page_chapter(){
        val pages = listOf("This shouldn't be here")
        `when`(pageSplitter.getPages()).thenReturn(pages)
        runTest { `when`(epubParser.getChapterBodyTextFromPath("ch1.html", fileReader)).thenReturn(DEFAULT_CHAPTER)}
        parse_book(DEFAULT_BOOK)

        verify(pageSplitter).setText(DEFAULT_CHAPTER)

        val resultPages = getUnitLiveDataValue(viewModel.pages).getContentIfNotHandled()
        // If it has only one page, then returns two pages (swipe doesn't work with 1 page)
        val expectedPages = listOf("This shouldn't be here", "")
        assertEquals(expectedPages, resultPages)
    }

    @Test(expected = TimeoutException::class)
    fun `When parse epub error, no page update `() = runTest {
        `when`(epubParser.parseBook(fileReader)).thenThrow(IOException())

        viewModel.parseEpub()

        viewModel.pages.getOrAwaitValue() // error because value is not set
    }

    @Test
    fun swipes_to_next_chapter(){
        parse_book(TWO_CHAPTER_BOOK)

        viewModel.swipeChapterRight()
        assertEquals(1, viewModel.currentChapter)
        runTest { advanceUntilIdle(); verify(epubParser).getChapterBodyTextFromPath("ch2.html", fileReader) }
    }

    @Test
    fun swipes_to_previous_chapter() = runTest {
        parse_book(TWO_CHAPTER_BOOK)

        viewModel.swipeChapterRight()
        assertEquals(1, viewModel.currentChapter)
        advanceUntilIdle()
        verify(epubParser).getChapterBodyTextFromPath("ch2.html", fileReader)

        viewModel.swipeChapterLeft()
        assertEquals(0, viewModel.currentChapter)
        // Called twice, when first load and after swiping
        advanceUntilIdle()
        verify(epubParser, times(2)).getChapterBodyTextFromPath("ch1.html", fileReader)
    }

    @Test
    fun `Resets translation when swiping to another chapter`(){
        val pages = listOf("", "", "Página para traducir", "", "")
        splitPages(pages)
        parse_book(TWO_CHAPTER_BOOK)

        val pageIndex = 2
        val expectedTranslation = "Page to translate"
        val trans = Translation(listOf(Segment(expectedTranslation, pages[pageIndex])), "ES")
        wordRepository.addTranslation(trans)

        // Request translation
        viewModel.translatePage(pageIndex)

        viewModel.swipeChapterRight()

        assertTrue(viewModel.translatedPages[pageIndex] == null)
    }

    @Test
    fun jumps_to_existing_chapter() = runTest {
        parse_book(THREE_CHAPTER_BOOK)

        viewModel.jumpToChapter("ch3.html")
        assertEquals(2, viewModel.currentChapter)
        advanceUntilIdle()
        verify(epubParser).getChapterBodyTextFromPath("ch3.html", fileReader)
    }

    @Test
    fun epub_first_load_set_predefined_page(){
        // Set up
        val initialPage = 4
        val initialChar = 41
        bookFile.lastChar = initialChar
        fileRepository.addFiles(bookFile)

        splitPages(8)
        viewModel.fileId = bookFile.id
        parse_book(DEFAULT_BOOK)

        // Returns initial page in first load
        val page = viewModel.getPage()
        assertEquals(initialPage, page)

        // Doesn't return initial page
        val secondLoadPage = viewModel.getPage()
        assertEquals(0, secondLoadPage)

    }

    @Test
    fun `Given out of bound last char, when epub load, then page zero`(){
        // Set up
        val initialPage = 0
        val initialChar = 100
        bookFile.lastChar = initialChar
        fileRepository.addFiles(bookFile)

        splitPages(8)
        viewModel.fileId = bookFile.id
        parse_book(DEFAULT_BOOK)

        // Returns initial page in first load
        val page = viewModel.getPage()
        assertEquals(initialPage, page)
    }

    @Test
    fun when_changed_to_previous_chapter_set_last_page() = runTest {
        // Set up
        val initialPage = 2
        val initialChar = 20
        val initialChapter = 1
        bookFile.lastChar = initialChar
        bookFile.chapter = initialChapter
        fileRepository.addFiles(bookFile)

        splitPages(3)
        viewModel.fileId = bookFile.id
        parse_book(DEFAULT_BOOK)

        // Returns initial page in first load
        val page = viewModel.getPage()
        assertEquals(initialPage, page)

        // Second load, changed to chapter 0
        splitPages(5)
        viewModel.swipeChapterLeft()
        advanceUntilIdle()
        val secondLoadPage = viewModel.getPage()
        assertEquals((5 - 1), secondLoadPage)
    }

    @Test
    fun when_changed_to_next_chapter_set_first_page(){
        // Set up
        val initialChar = 20
        bookFile.lastChar = initialChar
        fileRepository.addFiles(bookFile)

        splitPages(3)
        viewModel.fileId = bookFile.id
        parse_book(DEFAULT_BOOK)

        // Returns initial page in first load
        val page = viewModel.getPage()
        val initialPage = 2
        assertEquals(initialPage, page)

        // Second load
        viewModel.swipeChapterRight()
        splitPages(5)
        val secondLoadPage = viewModel.getPage()
        assertEquals(0, secondLoadPage)
    }

    @Test
    fun `When configuration change keep current chapter and page`(){
        // Set up with saved values
        val initialPage = 2
        val initialChapter = 3
        bookFile.page = initialPage
        bookFile.chapter = initialChapter
        fileRepository.addFiles(bookFile)

        splitPages(5)
        viewModel.fileId = bookFile.id
        parse_book(DEFAULT_BOOK)
        viewModel.getPage()

        // User modifies values
        viewModel.swipeChapterRight()
        viewModel.currentPage = initialPage + 1

        // On config change book is parsed again
        parse_book(DEFAULT_BOOK)

        assertEquals(4, viewModel.currentChapter)
        assertEquals(3, viewModel.getPage())
    }

    @Test
    fun `Saves current file when finishing`() = runTest {
        // Set up
        val uri = "empty_uri"
        parse_book(DEFAULT_BOOK)

        // Initial state
        viewModel.fileUri = uri

        splitPages(7)

        // Swipe to right three times
        viewModel.swipeChapterRight()
        viewModel.swipeChapterRight()
        viewModel.swipeChapterRight()
        advanceUntilIdle()

        val initialPage = 5
        viewModel.currentPage = initialPage

        val lastReadDate = Calendar.getInstance()
        val uuid = "random"
        viewModel.saveBookData(lastReadDate, uuid)

        val sumPreviousChars = sumCharacters(3, initialPage)

        val expectedFile = BookFile(
            uri,
            DEFAULT_BOOK.metadata.title,
            ImportedFileType.EPUB,
            folderPath = uuid,
            lastChar = 50,
            chapter = 3,
            percentageDone = 100 * sumPreviousChars / DEFAULT_BOOK.totalChars,
            lastRead = lastReadDate
        )
        val resultFile = fileRepository.filesServiceData.values.first()
        assertEquals(1, fileRepository.filesServiceData.values.size)
        assertEquals(expectedFile, resultFile)

    }

    @Test
    fun `Does not save file if initial parse isn't complete`(){

        // Stop the coroutine that is parsing the book
        // We do this in order to simulate closing the activity before the parsing finishes
//        mainCoroutineRule.pauseDispatcher()

        // Book parsing
        `when`(epubParser.basePath).thenReturn("")

        runBlocking {
            `when`(epubParser.parseBook(fileReader)).thenReturn(DEFAULT_BOOK)
            viewModel.parseEpub()
        }

        // Initial state
        val uri = "empty_uri"
        viewModel.fileUri = uri

        splitPages(7)

        val lastReadDate = Calendar.getInstance()
        val uuid = "random"
        viewModel.saveBookData(lastReadDate, uuid)

//        mainCoroutineRule.resumeDispatcher()

        assertEquals(0, fileRepository.filesServiceData.values.size)
    }

    @Test
    fun `Updates book files`(){
        // Set up
        val initialChapter = 2
        bookFile.chapter = initialChapter
        fileRepository.addFiles(bookFile)

        // Initial state
        viewModel.fileUri = bookFile.uri
        viewModel.fileId = bookFile.id

        splitPages(4)
        parse_book(DEFAULT_BOOK)
        viewModel.getPage()

        // Swipe to right
        viewModel.swipeChapterRight()
        val lastReadDate = Calendar.getInstance()
        viewModel.saveBookData(lastReadDate)

        val sumPreviousChars = sumCharacters(3, 0)

        val expectedFile = BookFile(
            bookFile.uri,
            bookFile.title,
            bookFile.fileType,
            folderPath = bookFile.folderPath,
            chapter = initialChapter + 1,
            percentageDone = 100 * sumPreviousChars / DEFAULT_BOOK.totalChars,
            lastRead = lastReadDate,
            id = bookFile.id
        )
        val resultFile = fileRepository.filesServiceData.values.first()
        assertEquals(1, fileRepository.filesServiceData.values.size)
        assertEquals(expectedFile, resultFile)

    }

    @Test
    fun `Creates new folder path for book file if empty`(){
        // TODO this test and "Updates book files" test are very similar, try to refactor
        val book = BookFile("default_uri", "Title", ImportedFileType.EPUB, folderPath = "", id = 1)

        fileRepository.addFiles(book)
        viewModel.fileUri = bookFile.uri
        viewModel.fileId = bookFile.id

        splitPages(4)
        parse_book(DEFAULT_BOOK)
        viewModel.getPage()

        val lastReadDate = Calendar.getInstance()
        val uuid = "random"
        viewModel.saveBookData(lastReadDate, uuid)

        val expectedFile = BookFile(
            book.uri,
            book.title,
            book.fileType,
            folderPath = uuid,
            percentageDone = 2, // it considers the first page as completed 10/500 = 2%
            lastRead = lastReadDate,
            id = book.id,
        )

        val resultFile = fileRepository.filesServiceData.values.first()
        assertEquals(1, fileRepository.filesServiceData.values.size)
        assertEquals(expectedFile, resultFile)

    }

    @Test
    fun `Parses book with uri already in db`(){
        // Setup
        fileRepository.addFiles(bookFile)
        viewModel.fileUri = bookFile.uri
        viewModel.fileId = -1

        splitPages(1)
        parse_book(DEFAULT_BOOK)

        val lastReadDate = Calendar.getInstance()
        viewModel.saveBookData(lastReadDate)

        assertEquals(1, fileRepository.filesServiceData.values.size)
    }

    @Test
    fun `Translates page first time`() = runTest {

        // Setup
        val pages = listOf("", "", "Página para traducir", "", "")
        splitPages(pages)
        parse_book(DEFAULT_BOOK)

        val pageIndex = 2
        val pageText = "Page to translate"
        val expectedTranslation = Translation(listOf(Segment(pageText, pages[pageIndex])), "ES")
        wordRepository.addTranslation(expectedTranslation)

        // Request translation
        viewModel.translatePage(pageIndex)
        advanceUntilIdle()

        val resultIndex = viewModel.translatedPageIndex.getOrAwaitValue().getContentIfNotHandled() ?: -1
        assertEquals(expectedTranslation, viewModel.translatedPages[resultIndex])
    }

    @Test
    fun `Translating page error`() = runTest {
        //Set up
        val pages = listOf("", "", "Página para traducir", "", "")
        splitPages(pages)
        parse_book(DEFAULT_BOOK)

        viewModel.translatePage(2)
        advanceUntilIdle()

        val errorMsg = viewModel.translationError.getOrAwaitValue()
        assertEquals("Translation not found for: Página para traducir", errorMsg.peekContent())
    }

    @Test
    fun `Give simple text, when parse, then success`() = runTest {
        val twelveChars = "twelve chars"
        val chunked = twelveChars.chunked(5)
        `when`(pageSplitter.getPages()).thenReturn(chunked)

        viewModel.parseSimpleText(twelveChars)
        advanceUntilIdle()

        val resultPages = viewModel.pages.getOrAwaitValue().getContentIfNotHandled()
        assertEquals(chunked, resultPages)
    }

    @Test
    fun `Given translated paragraph, when find sentence, then return second sentence`() = runTest {
        val twelveChars = "First paragraph. Second sentence. Second paragraph text"
        val chunked = twelveChars.chunked(34)
        `when`(pageSplitter.getPages()).thenReturn(chunked)
        parse_book(TWO_CHAPTER_BOOK)
        advanceUntilIdle()

        val expectedTranslation = Translation(listOf(Segment("First translation", "First paragraph. "), Segment("Second translation", "Second sentence. ")), "en")
        wordRepository.addTranslation(expectedTranslation)
        viewModel.translatePage(0)
        advanceUntilIdle()

        val result = viewModel.findSelectedSentence(0, 18) // index 18 is in the second sentence
        assertEquals(SplitPageSpan(Span(17, 34), Span(17, 35)), result)
    }

    @Test
    fun `Given translated paragraph, when find sentence outside range, then null`() = runTest {
        val twelveChars = "First paragraph. Second sentence. Second paragraph text"
        val chunked = twelveChars.chunked(34)
        `when`(pageSplitter.getPages()).thenReturn(chunked)
        parse_book(TWO_CHAPTER_BOOK)
        advanceUntilIdle()

        val expectedTranslation = Translation(listOf(Segment("First translation", "First paragraph. "), Segment("Second translation", "Second sentence. ")), "en")
        wordRepository.addTranslation(expectedTranslation)
        viewModel.translatePage(0)
        advanceUntilIdle()

        val result = viewModel.findSelectedSentence(0, 35) // index 18 is in the second sentence
        assertEquals(null, result)
    }

    @Test
    fun `Change and query language from`() = runTest {
        viewModel.languageFrom = "es"

        assertEquals("es", settingsRepository.getLanguageFrom())
    }

    @Test
    fun `Change and query language to`() = runTest {
        viewModel.languageTo = "de"

        assertEquals("de", settingsRepository.getLanguageTo())
        assertEquals(emptyList<Translation>(), viewModel.translatedPages)
    }

    @Test
    fun `Check default UI settings, saved during configuration changes`() = runTest {
        assertFalse(viewModel.hasBottomSheet)
        assertFalse(viewModel.isSheetExpanded)
        assertFalse(viewModel.fullScreen)
    }

    private fun parse_book(book: Book){
        `when`(epubParser.basePath).thenReturn("")

        runTest {
            `when`(epubParser.parseBook(fileReader)).thenReturn(book)
            viewModel.parseEpub()
        }

        val resultBook = getUnitLiveDataValue(viewModel.book)
        assertEquals(book, resultBook)
    }

    private fun splitPages(pagesSize: Int){
        val pages = Array(pagesSize) { "n".repeat(10) }.toList()
        `when`(pageSplitter.getPages()).thenReturn(pages)
    }

    private fun splitPages(pages: List<String>){
        `when`(pageSplitter.getPages()).thenReturn(pages)
    }

    private fun sumCharacters(chapterIndex: Int, pageIndex: Int): Int{
        var sumPreviousChars = 0
        // Sum of previous chapters (previous items in the spine)
        for (i in 0 until chapterIndex){
            sumPreviousChars += DEFAULT_BOOK.spine[i].charCount
        }

        // Sum of previous pages
        for(i in 0..pageIndex){
            sumPreviousChars += 10 // Every page only has one character
        }

        println("Sum previous pages $sumPreviousChars")
        return sumPreviousChars
    }

    companion object{
        const val DEFAULT_CHAPTER = "Chapter text"

        private val defaultMetadata = EPUBMetadata("title", "", "", "coverId")
        private val DEFAULT_BOOK = Book(
            EPUBMetadata("Test title", "", "", "coverId"),
            DEFAULT_CHAPTER,
            Array(5){SpineItem("$it", "ch${it + 1}.html", 100)}.toList(),
            mapOf("0" to "ch1.html", "1" to "ch2.html", "2" to "ch3.html", "3" to "ch4.html", "4" to "ch5.html", "coverId" to "cover-jpg"),
            TableOfContents(listOf())
        )
        private val TWO_CHAPTER_BOOK = Book(
            defaultMetadata,
            DEFAULT_CHAPTER,
            listOf(
                SpineItem("chapter1", "ch1.html", 0),
                SpineItem("chapter2", "ch2.html", 0)
            ),
            mapOf("chapter1" to "ch1.html", "chapter2" to "ch2.html"),
            TableOfContents(listOf())
        )
        private val THREE_CHAPTER_BOOK = Book(
            defaultMetadata,
            DEFAULT_CHAPTER,
            listOf(
                SpineItem("chapter1", "", 0),
                SpineItem("chapter2", "", 0),
                SpineItem("chapter3", "", 0)
            ),
            mapOf("chapter1" to "ch1.html", "chapter2" to "ch2.html", "chapter3" to "ch3.html", "coverId" to "cover-jpg"),
            TableOfContents(listOf(
                NavPoint("chapter1", "ch1.html"),
                NavPoint("chapter2", "ch2.html"),
                NavPoint("chapter3", "ch3.html")
            ))
        )
    }
}