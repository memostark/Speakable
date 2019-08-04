package com.guillermonegrete.tts.importtext

import android.text.TextPaint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.guillermonegrete.tts.importtext.epub.Book
import org.xmlpull.v1.XmlPullParser
import javax.inject.Inject

class VisualizeTextViewModel @Inject constructor(private val epubParser: EpubParser): ViewModel() {

    private var text = ""
    var currentChapter = 0
        private set

    var spineSize = 0
        private set

    private val _book = MutableLiveData<Book>()
    val book: LiveData<Book>
        get() = _book

    private val _pages = MutableLiveData<List<CharSequence>>()
    val pages: LiveData<List<CharSequence>>
        get() = _pages

    private val _chapterPath = MutableLiveData<String>()
    val chapterPath: LiveData<String>
        get() = _chapterPath


    fun parseEpub(parser: XmlPullParser, fileReader: ZipFileReader){
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        val parsedBook = epubParser.parseBook(parser, fileReader)
        text = parsedBook.currentChapter
        spineSize = parsedBook.spine.size
        _book.value = parsedBook
    }

    fun changeEpubChapter(path: String, parser: XmlPullParser, fileReader: ZipFileReader){
        text = epubParser.getChapterBodyTextFromPath(path, parser, fileReader)
    }

    fun parseSimpleText(text: String){
        this.text = text
    }

    fun swipeChapterRight(){
        swipeChapter(currentChapter + 1)
    }

    fun swipeChapterLeft(){
        swipeChapter(currentChapter - 1)
    }

    private fun swipeChapter(position: Int){
        _book.value?.let{
            val spineSize = it.spine.size
            if (position in 0 until spineSize) {
                val spineItem = it.spine[position]
                val newChapterPath = it.manifest[spineItem]
                if (newChapterPath != null) {
                    currentChapter = position
                    _chapterPath.value = newChapterPath
                }
            }
        }
    }

    fun jumpToChapter(path: String){
        _book.value?.let{
            val key = it.manifest.filterValues { value -> value == path }.keys.first()
            val index = it.spine.indexOf(key)
            if(index != -1) {
                currentChapter = index
                _chapterPath.value = path
            }
        }
    }

    fun splitToPages(pageSplitter: PageSplitter, textPaint: TextPaint){
        pageSplitter.append(text)
        pageSplitter.split(textPaint)
        val mutablePages = pageSplitter.getPages().toMutableList()
        if (mutablePages.size == 1 && _book.value != null) mutablePages.add("")
        _pages.value = mutablePages
    }

}