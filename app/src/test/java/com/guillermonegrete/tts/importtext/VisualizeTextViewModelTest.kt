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
        parse_default_book()

        val pages = listOf("This shouldn't be here", "This should be here")
        `when`(pageSplitter.getPages()).thenReturn(pages)
        viewModel.splitToPages("This shouldn't be here", pageSplitter, TextPaint())

        val resultPages = getUnitLiveDataValue(viewModel.pages)
        assertEquals(pages, resultPages)
    }

    @Test
    fun parse_epub_with_one_page_chapter(){
        parse_default_book()

        val pages = listOf("This shouldn't be here")
        `when`(pageSplitter.getPages()).thenReturn(pages)
        viewModel.splitToPages("This shouldn't be here", pageSplitter, TextPaint())

        val resultPages = getUnitLiveDataValue(viewModel.pages)
        val expectedPages = listOf("This shouldn't be here", "")
        assertEquals(expectedPages, resultPages)
    }

    private fun parse_default_book(){
        val book = Book(
            "title",
            "Chapter text",
            listOf(),
            mapOf(),
            TableOfContents(listOf())
        )
        `when`(epubParser.parseBook(parser, fileReader)).thenReturn(book)
        viewModel.parseEpub(parser, fileReader)

        val resultBook = getUnitLiveDataValue(viewModel.book)
        assertEquals(book, resultBook)
    }
}