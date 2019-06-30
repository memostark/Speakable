package com.guillermonegrete.tts.importtext

import android.text.TextPaint
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.guillermonegrete.tts.getUnitLiveDataValue
import com.guillermonegrete.tts.importtext.epub.Book
import com.guillermonegrete.tts.importtext.epub.TableOfContents
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.xmlpull.v1.XmlPullParser

class VisualizeTextViewModelTest {

    @get:Rule
    var mInstantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: VisualizeTextViewModel

    @Mock private lateinit var epubParser: EpubParser
    @Mock private lateinit var fileReader: ZipFileReader
    @Mock private lateinit var parser: XmlPullParser

    @Mock private lateinit var pageSplitter: PageSplitter

    @Before
    fun setUp(){
        MockitoAnnotations.initMocks(this)

        viewModel = VisualizeTextViewModel(epubParser)
    }

    @Test
    fun parse_epub_with_multiple_pages_chapter(){
        parse_book(DEFAULT_BOOK)

        val pages = listOf("This shouldn't be here", "This should be here")
        `when`(pageSplitter.getPages()).thenReturn(pages)
        viewModel.splitToPages(pageSplitter, TextPaint())
        verify(pageSplitter).append(DEFAULT_CHAPTER)

        val resultPages = getUnitLiveDataValue(viewModel.pages)
        assertEquals(pages, resultPages)
    }

    @Test
    fun parse_epub_with_one_page_chapter(){
        parse_book(DEFAULT_BOOK)

        val pages = listOf("This shouldn't be here")
        `when`(pageSplitter.getPages()).thenReturn(pages)
        viewModel.splitToPages(pageSplitter, TextPaint())
        verify(pageSplitter).append(DEFAULT_CHAPTER)

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

    private fun parse_book(book: Book){
        `when`(epubParser.parseBook(parser, fileReader)).thenReturn(book)
        viewModel.parseEpub(parser, fileReader)

        val resultBook = getUnitLiveDataValue(viewModel.book)
        assertEquals(book, resultBook)
    }

    companion object{
        const val DEFAULT_CHAPTER = "Chapter text"
        private val DEFAULT_BOOK = Book(
            "title",
            DEFAULT_CHAPTER,
            listOf(),
            mapOf(),
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
            TableOfContents(listOf())
        )
    }
}