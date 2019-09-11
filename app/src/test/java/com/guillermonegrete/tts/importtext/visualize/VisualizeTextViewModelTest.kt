package com.guillermonegrete.tts.importtext.visualize

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.android.architecture.blueprints.todoapp.MainCoroutineRule
import com.guillermonegrete.tts.data.source.FakeFileRepository
import com.guillermonegrete.tts.db.BookFile
import com.guillermonegrete.tts.getUnitLiveDataValue
import com.guillermonegrete.tts.importtext.ImportedFileType
import com.guillermonegrete.tts.importtext.epub.Book
import com.guillermonegrete.tts.importtext.epub.NavPoint
import com.guillermonegrete.tts.importtext.epub.TableOfContents
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
    @Mock private lateinit var fileReader: ZipFileReader
    private lateinit var fileRepository: FakeFileRepository

    @Mock private lateinit var pageSplitter: PageSplitter

    private val bookFile = BookFile("empty_uri", "Title", ImportedFileType.EPUB, id = 1)

    @Before
    fun setUp(){
        MockitoAnnotations.initMocks(this)

        fileRepository = FakeFileRepository()
        viewModel = VisualizeTextViewModel(epubParser, fileRepository)
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

        parse_book(DEFAULT_BOOK)
        viewModel.splitToPages()

        verify(pageSplitter).setText(DEFAULT_CHAPTER)
        val resultPages = getUnitLiveDataValue(viewModel.pages)
        assertEquals(pages, resultPages)
    }

    @Test
    fun parse_epub_with_one_page_chapter(){
        val pages = listOf("This shouldn't be here")
        `when`(pageSplitter.getPages()).thenReturn(pages)
        parse_book(DEFAULT_BOOK)

        viewModel.splitToPages()
        verify(pageSplitter).setText(DEFAULT_CHAPTER)

        val resultPages = getUnitLiveDataValue(viewModel.pages)
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
        viewModel.splitToPages()

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
        bookFile.page = initialPage
        fileRepository.addTasks(bookFile)

        splitPages(3)
        parse_book(DEFAULT_BOOK)
        viewModel.fileId = bookFile.id
        viewModel.initPageSplit()

        // Returns initial page in first load
        val page = viewModel.getPage()
        assertEquals(initialPage, page)

        // Second load
        viewModel.swipeChapterLeft()
        splitPages(5)
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
        parse_book(DEFAULT_BOOK)
        viewModel.fileId = bookFile.id
        viewModel.initPageSplit()

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
        viewModel.initPageSplit()

        splitPages(7)

        // Swipe to right three times
        viewModel.swipeChapterRight()
        viewModel.swipeChapterRight()
        viewModel.swipeChapterRight()

        val lastReadDate = Calendar.getInstance()
        viewModel.onFinish(lastReadDate)

        val expectedFile = BookFile(
            uri,
            DEFAULT_BOOK.title,
            ImportedFileType.EPUB,
            chapter = 3,
            lastRead = lastReadDate
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
        parse_book(DEFAULT_BOOK)

        // Initial state
        viewModel.fileUri = bookFile.uri
        viewModel.fileId = bookFile.id
        viewModel.initPageSplit()

        splitPages(7)

        // Swipe to right
        viewModel.swipeChapterRight()
        val lastReadDate = Calendar.getInstance()
        viewModel.onFinish(lastReadDate)

        val expectedFile = BookFile(
            bookFile.uri,
            bookFile.title,
            bookFile.fileType,
            id = bookFile.id,
            chapter = initialChapter + 1,
            lastRead = lastReadDate
        )
        val resultFile = fileRepository.filesServiceData.values.first()
        assertEquals(1, fileRepository.filesServiceData.values.size)
        assertEquals(expectedFile, resultFile)

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
        val pages = Array(pagesSize) {""}.toList()
        `when`(pageSplitter.getPages()).thenReturn(pages)
        viewModel.splitToPages()
    }

    companion object{
        const val DEFAULT_CHAPTER = "Chapter text"
        private val DEFAULT_BOOK = Book(
            "Test title",
            DEFAULT_CHAPTER,
            Array(5){"$it"}.toList(),
            mapOf("1" to "ch1.html", "2" to "ch2.html", "3" to "ch3.html", "4" to "ch4.html", "5" to "ch5.html"),
            TableOfContents(listOf())
        )
        private val TWO_CHAPTER_BOOK = Book(
            "title",
            DEFAULT_CHAPTER,
            listOf("chapter1", "chapter2"),
            mapOf("chapter1" to "ch1.html", "chapter2" to "ch2.html"),
            TableOfContents(listOf())
        )
        private val THREE_CHAPTER_BOOK = Book(
            "title",
            DEFAULT_CHAPTER,
            listOf("chapter1", "chapter2", "chapter3"),
            mapOf("chapter1" to "ch1.html", "chapter2" to "ch2.html", "chapter3" to "ch3.html"),
            TableOfContents(listOf(
                NavPoint("chapter1", "ch1.html"),
                NavPoint("chapter2", "ch2.html"),
                NavPoint("chapter3", "ch3.html")
            ))
        )
    }
}