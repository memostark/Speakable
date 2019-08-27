package com.guillermonegrete.tts.importtext

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.guillermonegrete.tts.importtext.epub.Book
import javax.inject.Inject

class VisualizeTextViewModel @Inject constructor(private val epubParser: EpubParser): ViewModel() {

    var isEpub = false
    private var firstLoad = true
    private var leftSwipe = false

    /**
     * Initial index values
     */
    var initialChapter = 0

    var initialPage = 0

    private var text = ""
    var currentChapter = 0
        private set

    var spineSize = 0
        private set

    var basePath = ""
        private set

    var pagesSize = 0
        private set

    private var currentBook: Book? = null

    private val _book = MutableLiveData<Book>()
    val book: LiveData<Book>
        get() = _book

    private val _pages = MutableLiveData<List<CharSequence>>()
    val pages: LiveData<List<CharSequence>>
        get() = _pages

    private val _chapterPath = MutableLiveData<String>()
    val chapterPath: LiveData<String>
        get() = _chapterPath


    fun parseEpub(fileReader: ZipFileReader){
        val parsedBook = epubParser.parseBook(fileReader)
        basePath = epubParser.basePath
        text = parsedBook.currentChapter
        spineSize = parsedBook.spine.size
        currentBook = parsedBook
        _book.value = parsedBook
    }

    fun changeEpubChapter(path: String, fileReader: ZipFileReader){
        text = epubParser.getChapterBodyTextFromPath(path, fileReader)
    }

    fun parseSimpleText(text: String){
        this.text = text
    }

    fun swipeChapterRight(){
        leftSwipe = false
        swipeChapter(currentChapter + 1)
    }

    fun swipeChapterLeft(){
        leftSwipe = true
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

    fun getPage(): Int{
        return if(firstLoad) {
            firstLoad = false
            if (initialPage >= pagesSize) pagesSize - 1 else initialPage
        } else if(leftSwipe) pagesSize - 1 else 0
    }

    fun setChapter(chapterIndex: Int, pageSplitter: PageSplitter){
        if(isEpub){
            jumpToChapter(chapterIndex)
        }else{
            splitToPages(pageSplitter)
        }
    }

    /**
     * Used when you have the file path to the chapter e.g. changing chapters with the table of contents
     */
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

    /**
     * Used when you have the index, using the order defined in the spine. E.g. index saved from recent files list
     */
    fun jumpToChapter(index: Int){
        currentBook?.let {book ->
            val spine = book.spine
            val manifest = book.manifest

            println("Manifest: $manifest")
            println("Spine: $spine")

            val spineItem = spine[index]
            val chapterPath = manifest[spineItem]

            println(" Index: $index, Path: $chapterPath")

            if(index != -1) {
                currentChapter = index
                _chapterPath.value = chapterPath
            }
        }
    }

    fun splitToPages(pageSplitter: PageSplitter){
        pageSplitter.setText(text)
        pageSplitter.split()
        val mutablePages = pageSplitter.getPages().toMutableList()
        if (mutablePages.size == 1 && _book.value != null) mutablePages.add("")
        pagesSize = mutablePages.size
        _pages.value = mutablePages
    }

}