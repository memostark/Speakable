package com.guillermonegrete.tts.importtext.visualize

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.guillermonegrete.tts.MainCoroutineRule
import com.guillermonegrete.tts.TestThreadExecutor
import com.guillermonegrete.tts.data.preferences.FakeSettingsRepository
import com.guillermonegrete.tts.data.source.FakeFileRepository
import com.guillermonegrete.tts.data.source.FakeWordRepository
import com.guillermonegrete.tts.db.BookFile
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.getUnitLiveDataValue
import com.guillermonegrete.tts.importtext.ImportedFileType
import com.guillermonegrete.tts.importtext.epub.Book
import com.guillermonegrete.tts.importtext.epub.NavPoint
import com.guillermonegrete.tts.importtext.epub.SpineItem
import com.guillermonegrete.tts.importtext.epub.TableOfContents
import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation
import com.guillermonegrete.tts.threading.TestMainThread
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.util.*

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

    private val bookFile = BookFile("empty_uri", "Title", ImportedFileType.EPUB, id = 1)

    @Before
    fun setUp(){
        MockitoAnnotations.initMocks(this)

        fileRepository = FakeFileRepository()
        wordRepository = FakeWordRepository()
        settingsRepository = FakeSettingsRepository()

        val getTranslationInteractor = GetLangAndTranslation(TestThreadExecutor(), TestMainThread(), wordRepository)

        viewModel = VisualizeTextViewModel(epubParser, settingsRepository, fileRepository, getTranslationInteractor)
        viewModel.pageSplitter = pageSplitter
        viewModel.fileReader = fileReader

        bookFile.apply {
            chapter = 0
            page = 0
        }
    }

    @Test
    fun parse_book_and_show_loading_icon(){
        // Pause dispatcher so we can verify initial values
        mainCoroutineRule.pauseDispatcher()

        // Epub parsing
        runBlockingTest {
            `when`(epubParser.parseBook(fileReader)).thenReturn(DEFAULT_BOOK)
            viewModel.parseEpub()
        }

        // Then progress indicator is shown
        Assert.assertTrue(getUnitLiveDataValue(viewModel.dataLoading))

        mainCoroutineRule.resumeDispatcher()

        // Then progress indicator is hidden
        Assert.assertFalse(getUnitLiveDataValue(viewModel.dataLoading))

        val resultBook = getUnitLiveDataValue(viewModel.book)
        assertEquals(DEFAULT_BOOK, resultBook)
    }

    @Test
    fun parse_epub_with_multiple_pages_chapter(){
        val pages = listOf("This shouldn't be here", "This should be here")
        `when`(pageSplitter.getPages()).thenReturn(pages)
        runBlockingTest { `when`(epubParser.getChapterBodyTextFromPath("ch1.html", fileReader)).thenReturn(DEFAULT_CHAPTER)}

        parse_book(DEFAULT_BOOK)

        val resultPages = getUnitLiveDataValue(viewModel.pages)
        assertEquals(pages, resultPages)
    }

    @Test
    fun parse_epub_with_one_page_chapter(){
        val pages = listOf("This shouldn't be here")
        `when`(pageSplitter.getPages()).thenReturn(pages)
        runBlockingTest { `when`(epubParser.getChapterBodyTextFromPath("ch1.html", fileReader)).thenReturn(DEFAULT_CHAPTER)}
        parse_book(DEFAULT_BOOK)

        verify(pageSplitter).setText(DEFAULT_CHAPTER)

        val resultPages = getUnitLiveDataValue(viewModel.pages)
        // If it has only one page, then returns two pages (swipe doesn't work with 1 page)
        val expectedPages = listOf("This shouldn't be here", "")
        assertEquals(expectedPages, resultPages)
    }

    @Test
    fun swipes_to_next_chapter(){
        parse_book(TWO_CHAPTER_BOOK)

        viewModel.swipeChapterRight()
        assertEquals(1, viewModel.currentChapter)
        runBlockingTest { verify(epubParser).getChapterBodyTextFromPath("ch2.html", fileReader) }
    }

    @Test
    fun swipes_to_previous_chapter(){
        parse_book(TWO_CHAPTER_BOOK)

        viewModel.swipeChapterRight()
        assertEquals(1, viewModel.currentChapter)
        runBlockingTest { verify(epubParser).getChapterBodyTextFromPath("ch2.html", fileReader) }

        viewModel.swipeChapterLeft()
        assertEquals(0, viewModel.currentChapter)
        // Called twice, when first load and after swiping
        runBlockingTest { verify(epubParser, times(2)).getChapterBodyTextFromPath("ch1.html", fileReader) }
    }

    @Test
    fun jumps_to_existing_chapter(){
        parse_book(THREE_CHAPTER_BOOK)

        viewModel.jumpToChapter("ch3.html")
        assertEquals(2, viewModel.currentChapter)
        runBlockingTest { verify(epubParser).getChapterBodyTextFromPath("ch3.html", fileReader) }
    }

    @Test
    fun epub_first_load_set_predefined_page(){
        // Set up
        val initialPage = 4
        bookFile.page = initialPage
        fileRepository.addTasks(bookFile)

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
    fun when_changed_to_previous_chapter_set_last_page(){
        // Set up
        val initialPage = 2
        val initialChapter = 1
        bookFile.page = initialPage
        bookFile.chapter = initialChapter
        fileRepository.addTasks(bookFile)

        splitPages(3)
        viewModel.fileId = bookFile.id
        parse_book(DEFAULT_BOOK)

        // Returns initial page in first load
        val page = viewModel.getPage()
        assertEquals(initialPage, page)

        // Second load, changed to chapter 0
        splitPages(5)
        viewModel.swipeChapterLeft()
        val secondLoadPage = viewModel.getPage()
        assertEquals(5 - 1, secondLoadPage)
    }

    @Test
    fun when_changed_to_next_chapter_set_first_page(){
        // Set up
        val initialPage = 2
        bookFile.page = initialPage
        fileRepository.addTasks(bookFile)

        splitPages(3)
        viewModel.fileId = bookFile.id
        parse_book(DEFAULT_BOOK)

        // Returns initial page in first load
        val page = viewModel.getPage()
        assertEquals(initialPage, page)

        // Second load
        viewModel.swipeChapterRight()
        splitPages(5)
        val secondLoadPage = viewModel.getPage()
        assertEquals(0, secondLoadPage)
    }

    @Test
    fun saves_current_file_when_finishing(){
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

        val initialPage = 5
        viewModel.currentPage = initialPage

        val lastReadDate = Calendar.getInstance()
        viewModel.onFinish(lastReadDate)

        val sumPreviousChars = sumCharacters(3, initialPage)
        println("Total chars ${DEFAULT_BOOK.totalChars} ")

        val expectedFile = BookFile(
            uri,
            DEFAULT_BOOK.title,
            ImportedFileType.EPUB,
            page = initialPage,
            chapter = 3,
            lastRead = lastReadDate,
            percentageDone = 100 * sumPreviousChars / DEFAULT_BOOK.totalChars
        )
        val resultFile = fileRepository.filesServiceData.values.first()
        assertEquals(1, fileRepository.filesServiceData.values.size)
        assertEquals(expectedFile, resultFile)

    }

    @Test
    fun updates_book_file(){
        // Set up
        val initialChapter = 2
        bookFile.chapter = initialChapter
        fileRepository.addTasks(bookFile)

        // Initial state
        viewModel.fileUri = bookFile.uri
        viewModel.fileId = bookFile.id
        parse_book(DEFAULT_BOOK)


        splitPages(7)

        // Swipe to right
        viewModel.swipeChapterRight()
        val lastReadDate = Calendar.getInstance()
        viewModel.onFinish(lastReadDate)

        val sumPreviousChars = sumCharacters(3, 0)

        val expectedFile = BookFile(
            bookFile.uri,
            bookFile.title,
            bookFile.fileType,
            id = bookFile.id,
            chapter = initialChapter + 1,
            lastRead = lastReadDate,
            percentageDone = 100 * sumPreviousChars / DEFAULT_BOOK.totalChars
        )
        val resultFile = fileRepository.filesServiceData.values.first()
        assertEquals(1, fileRepository.filesServiceData.values.size)
        assertEquals(expectedFile, resultFile)

    }

    @Test
    fun parses_book_from_picker_with_uri_already_in_db(){
        // Setup
        fileRepository.addTasks(bookFile)
        viewModel.fileUri = bookFile.uri
        viewModel.fileId = -1

        splitPages(1)
        parse_book(DEFAULT_BOOK)

        val lastReadDate = Calendar.getInstance()
        viewModel.onFinish(lastReadDate)

        assertEquals(1, fileRepository.filesServiceData.values.size)


    }

    @Test
    fun `Translates page first time`(){

        // Setup
        val pages = listOf("", "", "Página para traducir", "", "")
        splitPages(pages)
        parse_book(DEFAULT_BOOK)

        val pageIndex = 2
        val expectedTranslation = "Page to translate"
        wordRepository.addTranslation(Words(pages[pageIndex], "ES", expectedTranslation))

        // Request translation
        viewModel.translatePage(pageIndex)

        val resultIndex = getUnitLiveDataValue(viewModel.translatedPageIndex)
        assertEquals(expectedTranslation, viewModel.translatedPages[resultIndex])
    }

    @Test
    fun `Translating page error`(){
        //Set up
        val pages = listOf("", "", "Página para traducir", "", "")
        splitPages(pages)
        parse_book(DEFAULT_BOOK)

        viewModel.translatePage(2)

        val errorMsg = getUnitLiveDataValue(viewModel.translationError)
        assertEquals("Translation not found", errorMsg)
    }

    private fun parse_book(book: Book){
        runBlockingTest {
            `when`(epubParser.parseBook(fileReader)).thenReturn(book)
            viewModel.parseEpub()
        }

        val resultBook = getUnitLiveDataValue(viewModel.book)
        assertEquals(book, resultBook)
    }

    private fun splitPages(pagesSize: Int){
        val pages = Array(pagesSize) {"n"}.toList()
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
            sumPreviousChars += 1 // Every page only has one character
        }

        println("Sum previous pages $sumPreviousChars")
        return sumPreviousChars
    }

    companion object{
        const val DEFAULT_CHAPTER = "Chapter text"
        private val DEFAULT_BOOK = Book(
            "Test title",
            DEFAULT_CHAPTER,
            Array(5){SpineItem("$it", "ch${it + 1}.html", it + 100)}.toList(),
            mapOf("0" to "ch1.html", "1" to "ch2.html", "2" to "ch3.html", "3" to "ch4.html", "4" to "ch5.html"),
            TableOfContents(listOf())
        )
        private val TWO_CHAPTER_BOOK = Book(
            "title",
            DEFAULT_CHAPTER,
            listOf(
                SpineItem("chapter1", "ch1.html", 0),
                SpineItem("chapter2", "ch2.html", 0)
            ),
            mapOf("chapter1" to "ch1.html", "chapter2" to "ch2.html"),
            TableOfContents(listOf())
        )
        private val THREE_CHAPTER_BOOK = Book(
            "title",
            DEFAULT_CHAPTER,
            listOf(
                SpineItem("chapter1", "", 0),
                SpineItem("chapter2", "", 0),
                SpineItem("chapter3", "", 0)
            ),
            mapOf("chapter1" to "ch1.html", "chapter2" to "ch2.html", "chapter3" to "ch3.html"),
            TableOfContents(listOf(
                NavPoint("chapter1", "ch1.html"),
                NavPoint("chapter2", "ch2.html"),
                NavPoint("chapter3", "ch3.html")
            ))
        )
    }
}