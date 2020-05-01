package com.guillermonegrete.tts.importtext.visualize

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guillermonegrete.tts.data.source.FileRepository
import com.guillermonegrete.tts.db.BookFile
import com.guillermonegrete.tts.importtext.ImportedFileType
import com.guillermonegrete.tts.importtext.epub.Book
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject

class VisualizeTextViewModel @Inject constructor(
    private val epubParser: EpubParser,
    private val fileRepository: FileRepository
): ViewModel() {

    var pageSplitter: PageSplitter? = null
    var fileReader: ZipFileReader? = null

    private var isEpub = false
    private var firstLoad = true
    private var leftSwipe = false

    private var text = ""
    var currentPage = 0
    var currentChapter = 0
        private set

    var spineSize = 0
        private set

    var pagesSize = 0
        private set

    var fileUri: String? = null
    var fileId: Int = -1
    private var databaseBookFile: BookFile? = null

    private var currentBook: Book? = null
    private var fileType = ImportedFileType.TXT

    private val _book = MutableLiveData<Book>()
    val book: LiveData<Book>
        get() = _book

    private val _pages = MutableLiveData<List<CharSequence>>()
    val pages: LiveData<List<CharSequence>>
        get() = _pages

    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> = _dataLoading


    fun parseEpub() {
        fileReader?.let {
            _dataLoading.value = true
            viewModelScope.launch {
                val parsedBook = epubParser.parseBook(it)
                val imageGetter = pageSplitter?.imageGetter
                if(imageGetter != null && imageGetter is InputStreamImageGetter){
                    imageGetter.basePath = epubParser.basePath
                }
                text = parsedBook.currentChapter
                spineSize = parsedBook.spine.size
                currentBook = parsedBook
                fileType = ImportedFileType.EPUB
                _book.value = parsedBook
                isEpub = true
                initPageSplit()
                _dataLoading.value = false
            }
        }
    }

    private suspend fun changeEpubChapter(path: String){
        text = fileReader?.let { epubParser.getChapterBodyTextFromPath(path, it) } ?: ""
    }

    fun parseSimpleText(text: String){
        viewModelScope.launch {
            fileType = ImportedFileType.TXT
            this@VisualizeTextViewModel.text = text
            initPageSplit()
        }
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
        currentBook?.let{
            val spineSize = it.spine.size
            if (position in 0 until spineSize) {
                val spineItem = it.spine[position]
                val newChapterPath = it.manifest[spineItem.idRef]
                if (newChapterPath != null) {
                    currentChapter = position
                    _dataLoading.value = true
                    viewModelScope.launch {
                        changeEpubChapter(newChapterPath)
                        splitToPages()
                        _dataLoading.value = false
                    }
                }
            }
        }
    }

    fun getPage(): Int{
        currentPage = if(firstLoad) {
            firstLoad = false
            val initialPage = databaseBookFile?.page ?: 0
            if (initialPage >= pagesSize) pagesSize - 1 else initialPage
        } else if(leftSwipe) pagesSize - 1 else 0
        return currentPage
    }

    private suspend fun initPageSplit() {
        if(isEpub){
            databaseBookFile = getBookFile()
            val initialChapter = databaseBookFile?.chapter ?: 0
            jumpToChapter(initialChapter)
        }else{
            splitToPages()
        }
    }

    private suspend fun getBookFile(): BookFile?{
        return if(fileId == -1){
            fileUri?.let { fileRepository.getFile(it) }
        } else {
            fileRepository.getFile(fileId)
        }
    }

    /**
     * Used when you have the file path to the chapter e.g. changing chapters with the table of contents
     */
    fun jumpToChapter(path: String){
        _book.value?.let{
            val key = it.manifest.filterValues { value -> value == path }.keys.first()
            val index = it.spine.indexOfFirst { item -> item.idRef == key }
            if(index != -1) {
                currentChapter = index
                _dataLoading.value = true
                viewModelScope.launch {
                    changeEpubChapter(path)
                    splitToPages()
                    _dataLoading.value = false
                }
            }
        }
    }

    /**
     * Used when you have the index, using the order defined in the spine. E.g. index saved from recent files list
     */
    private suspend fun jumpToChapter(index: Int){
        currentBook?.let {book ->
            val spine = book.spine
            val manifest = book.manifest

            println("Manifest: $manifest")
            println("Spine: $spine")

            val spineItem = spine[index]
            val chapterPath = manifest[spineItem.idRef]

            println(" Index: $index, Path: $chapterPath")

            if(index != -1 && chapterPath != null) {
                currentChapter = index
                changeEpubChapter(chapterPath)
                splitToPages()
            }
        }
    }

    private suspend fun splitToPages() {
        pageSplitter?.let {
            it.setText(text)
            it.split()
            val mutablePages = it.getPages().toMutableList()
            if (mutablePages.size == 1 && _book.value != null) mutablePages.add("")
            pagesSize = mutablePages.size
            _pages.value = mutablePages
        }
    }

    /**
     * Called when view or application is about to be destroyed.
     * The date is injected only when testing, function should be
     * called without parameters.
     */
    fun onFinish(date: Calendar = Calendar.getInstance()){
        saveBookFileData(date)
    }

    private fun saveBookFileData(date: Calendar){
        fileUri?.let {
            // This operation is intended to be synchronous
            runBlocking{
                databaseBookFile?.apply {
                    page = currentPage
                    chapter = currentChapter
                    lastRead = date
                }
                val title = currentBook?.title ?: ""
                val bookFile = databaseBookFile ?: BookFile(it, title, fileType, "und", currentPage, currentChapter, date)
                fileRepository.saveFile(bookFile)
            }
        }
    }

}