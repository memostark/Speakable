package com.guillermonegrete.tts.importtext

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.android.architecture.blueprints.todoapp.MainCoroutineRule
import com.guillermonegrete.tts.data.source.FileRepository
import com.guillermonegrete.tts.db.BookFile
import com.guillermonegrete.tts.getUnitLiveDataValue
import com.guillermonegrete.tts.importtext.epub.Book
import com.guillermonegrete.tts.importtext.epub.NavPoint
import com.guillermonegrete.tts.importtext.epub.TableOfContents
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import java.util.*

class VisualizeTextViewModelTest {

    @get:Rule
    var mInstantTaskExecutorRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var viewModel: VisualizeTextViewModel

    @Mock private lateinit var epubParser: EpubParser
    @Mock private lateinit var fileReader: ZipFileReader
    @Mock private lateinit var fileRepository: FileRepository

    @Mock private lateinit var pageSplitter: PageSplitter

    @Before
    fun setUp(){
        MockitoAnnotations.initMocks(this)

        viewModel = VisualizeTextViewModel(epubParser, fileRepository)
    }

    @Test
    fun parse_epub_with_multiple_pages_chapter(){
        parse_book(DEFAULT_BOOK)

        val pages = listOf("This shouldn't be here", "This should be here")
        `when`(pageSplitter.getPages()).thenReturn(pages)
        viewModel.splitToPages(pageSplitter)
        verify(pageSplitter).setText(DEFAULT_CHAPTER)

        val resultPages = getUnitLiveDataValue(viewModel.pages)
        assertEquals(pages, resultPages)
    }

    @Test
    fun parse_epub_with_one_page_chapter(){
        parse_book(DEFAULT_BOOK)

        val pages = listOf("This shouldn't be here")
        `when`(pageSplitter.getPages()).thenReturn(pages)
        viewModel.splitToPages(pageSplitter)
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
        val path = getUnitLiveDataValue(viewModel.chapterPath)
        assertEquals("ch2.html", path)
    }

    @Test
    fun swipes_to_previous_chapter(){
        parse_book(TWO_CHAPTER_BOOK)

        viewModel.swipeChapterRight()
        assertEquals(1, viewModel.currentChapter)
        val path = getUnitLiveDataValue(viewModel.chapterPath)
        assertEquals("ch2.html", path)

        viewModel.swipeChapterLeft()
        assertEquals(0, viewModel.currentChapter)
        val path2 = getUnitLiveDataValue(viewModel.chapterPath)
        assertEquals("ch1.html", path2)
    }

    @Test
    fun jumps_to_existing_chapter(){
        parse_book(THREE_CHAPTER_BOOK)

        viewModel.jumpToChapter("ch3.html")
        assertEquals(2, viewModel.currentChapter)
        val path2 = getUnitLiveDataValue(viewModel.chapterPath)
        assertEquals("ch3.html", path2)
    }

    @Test
    fun jumps_to_existing_chapter_using_index(){
        parse_book(THREE_CHAPTER_BOOK)

        viewModel.jumpToChapter(2)
        assertEquals(2, viewModel.currentChapter)
        val path2 = getUnitLiveDataValue(viewModel.chapterPath)
        assertEquals("ch3.html", path2)
    }

    @Test
    fun epub_first_load_set_predefined_page(){
        // Set up
        parse_book(DEFAULT_BOOK)
        val pages =  Array(8) {""}.toList()
        `when`(pageSplitter.getPages()).thenReturn(pages)
        viewModel.splitToPages(pageSplitter)

        // Returns initial page in first load
        val initialPage = 4
        viewModel.initialPage = initialPage
        val page = viewModel.getPage()
        assertEquals(initialPage, page)

        // Doesn't return initial page
        val secondLoadPage = viewModel.getPage()
        assertEquals(0, secondLoadPage)

    }

    @Test
    fun when_changed_to_previous_chapter_set_last_page(){
        // Set up
        parse_book(DEFAULT_BOOK)
        val pages = Array(3) {""}.toList()
        `when`(pageSplitter.getPages()).thenReturn(pages)
        viewModel.splitToPages(pageSplitter)

        // Returns initial page in first load
        val initialPage = 2
        viewModel.initialPage = initialPage
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
        parse_book(DEFAULT_BOOK)
        val pages = Array(3) {""}.toList()
        `when`(pageSplitter.getPages()).thenReturn(pages)
        viewModel.splitToPages(pageSplitter)

        // Returns initial page in first load
        val initialPage = 2
        viewModel.initialPage = initialPage
        val page = viewModel.getPage()
        assertEquals(initialPage, page)

        // Second load
        viewModel.swipeChapterRight()
        splitPages(5)
        val secondLoadPage = viewModel.getPage()
        assertEquals(0, secondLoadPage)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun saves_current_file_when_finishing(){
        // Set up
        val uri = "empty_uri"
        parse_book(DEFAULT_BOOK)

        // Initial state
        val initialChapter = 2
        viewModel.fileUri = uri
        viewModel.initialChapter = initialChapter
        viewModel.initPageSplit(pageSplitter)

        val pages = Array(7) {""}.toList()
        `when`(pageSplitter.getPages()).thenReturn(pages)
        viewModel.splitToPages(pageSplitter)

        // Swipe to right
        viewModel.swipeChapterRight()
        val lastReadDate = Calendar.getInstance()
        viewModel.onFinish(lastReadDate)

        val expectedFile = BookFile(
            uri,
            "Title",
            ImportedFileType.EPUB,
            chapter = 3,
            lastRead = lastReadDate
        )
        runBlockingTest { verify(fileRepository).saveFile(expectedFile) }



    }

    private fun parse_book(book: Book){
        `when`(epubParser.parseBook(fileReader)).thenReturn(book)
        viewModel.parseEpub(fileReader)
        viewModel.isEpub = true

        val resultBook = getUnitLiveDataValue(viewModel.book)
        assertEquals(book, resultBook)
    }

    private fun splitPages(pagesSize: Int){
        val pages = Array(pagesSize) {""}.toList()
        `when`(pageSplitter.getPages()).thenReturn(pages)
        viewModel.splitToPages(pageSplitter)
    }

    companion object{
        const val DEFAULT_CHAPTER = "Chapter text"
        private val DEFAULT_BOOK = Book(
            "title",
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