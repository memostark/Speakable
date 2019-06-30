package com.guillermonegrete.tts.importtext

import android.text.TextPaint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.guillermonegrete.tts.importtext.epub.Book
import org.xmlpull.v1.XmlPullParser

class VisualizeTextViewModel(private val epubParser: EpubParser): ViewModel() {

    private val _book = MutableLiveData<Book>()
    val book: LiveData<Book>
        get() = _book

    private val _pages = MutableLiveData<List<CharSequence>>()
    val pages: LiveData<List<CharSequence>>
        get() = _pages

    fun parseEpub(parser: XmlPullParser, fileReader: ZipFileReader){
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        _book.value = epubParser.parseBook(parser, fileReader)
    }

    fun splitToPages(text: String, pageSplitter: PageSplitter, textPaint: TextPaint){
        pageSplitter.append(text)
        pageSplitter.split(textPaint)
        val mutablePages = pageSplitter.getPages().toMutableList()
        if (mutablePages.size == 1 && _book.value != null) mutablePages.add("")
        _pages.value = mutablePages
    }

}