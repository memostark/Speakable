package com.guillermonegrete.tts.importtext.visualize

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guillermonegrete.tts.data.source.FileRepository
import com.guillermonegrete.tts.db.BookFile
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.importtext.ImportedFileType
import com.guillermonegrete.tts.importtext.epub.Book
import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject

class VisualizeTextViewModel @Inject constructor(
    private val epubParser: EpubParser,
    private val fileRepository: FileRepository,
    private val getTranslationInteractor: GetLangAndTranslation
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

    private var currentPages = listOf<CharSequence>()
    private val _pages = MutableLiveData<List<CharSequence>>()
    val pages: LiveData<List<CharSequence>>
        get() = _pages

    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> = _dataLoading

    private val _pageTranslation = MutableLiveData<Words>()
    val pageTranslation: LiveData<Words> = _pageTranslation


    fun parseEpub() {
        val reader = fileReader
        reader ?: return

        _dataLoading.value = true
        viewModelScope.launch {
            val parsedBook = epubParser.parseBook(reader)
            val imageGetter = pageSplitter?.imageGetter
            if(imageGetter is InputStreamImageGetter){
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
        val tempBook = _book.value
        tempBook ?: return

        val spineSize = tempBook.spine.size
        if (position in 0 until spineSize) {

            val newChapterPath = tempBook.spine[position].href

            currentChapter = position
            _dataLoading.value = true
            viewModelScope.launch {
                changeEpubChapter(newChapterPath)
                splitToPages()
                _dataLoading.value = false
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
        val tempBook = _book.value
        tempBook ?: return

        val key = tempBook.manifest.filterValues { value -> value == path }.keys.first()
        val index = tempBook.spine.indexOfFirst { item -> item.idRef == key }
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

    fun translatePage(index: Int){
        val text = currentPages[index]
        getTranslationInteractor(
            text.toString(),
            object : GetLangAndTranslation.Callback {
                override fun onTranslationAndLanguage(word: Words) {
                    _pageTranslation.value = word
                }

                override fun onDataNotAvailable() {}
            })
    }

    /**
     * Used when you have the index, using the order defined in the spine. E.g. index saved from recent files list
     */
    private suspend fun jumpToChapter(index: Int){
        val tempBook = currentBook
        tempBook ?: return

        val chapterPath = tempBook.spine[index].href

        if(index != -1) {
            currentChapter = index
            changeEpubChapter(chapterPath)
            splitToPages()
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
            currentPages = mutablePages
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
        val uri = fileUri
        uri ?: return

        // This operation is intended to be synchronous
        runBlocking{
            val progress = calculateProgress()

            databaseBookFile?.apply {
                page = currentPage
                chapter = currentChapter
                lastRead = date
                percentageDone = progress
            }
            val title = currentBook?.title ?: ""
            val bookFile = databaseBookFile ?: BookFile(uri, title, fileType, "und", currentPage, currentChapter, calculateProgress(), date)
            fileRepository.saveFile(bookFile)
        }
    }

    private fun calculateProgress(): Int{
        var percentage = 0

        currentBook?.let {
            var sumPreviousChars = 0

            // Sum of previous spine items (chapters)
            for (i in 0 until currentChapter){
                sumPreviousChars += it.spine[i].charCount
            }

            // Sum of previous and current pages
            for(i in 0..currentPage){
                sumPreviousChars += currentPages[i].length
            }

            percentage = 100 * sumPreviousChars / it.totalChars
        }
        return percentage
    }

}