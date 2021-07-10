package com.guillermonegrete.tts.importtext.visualize

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guillermonegrete.tts.Event
import com.guillermonegrete.tts.data.Result
import com.guillermonegrete.tts.data.preferences.SettingsRepository
import com.guillermonegrete.tts.data.source.FileRepository
import com.guillermonegrete.tts.db.BookFile
import com.guillermonegrete.tts.importtext.ImportedFileType
import com.guillermonegrete.tts.importtext.epub.Book
import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation
import kotlinx.coroutines.*
import java.io.File
import java.util.*

class VisualizeTextViewModel @ViewModelInject constructor(
    private val epubParser: EpubParser,
    private val settings: SettingsRepository,
    private val fileRepository: FileRepository,
    private val getTranslationInteractor: GetLangAndTranslation
): ViewModel() {

    var pageSplitter: PageSplitter? = null
    var fileReader: ZipFileReader? = null

    private var firstLoad = true
    private var leftSwipe = false

    private var text = ""
    var currentPage = -1
    var currentChapter = -1
        private set

    var spineSize = 0
        private set

    var pagesSize = 0
        private set

    var fileUri: String? = null
    private var uuid: String = UUID.randomUUID().toString()
    var fileId: Int = -1
    private var databaseBookFile: BookFile? = null

    private var currentBook: Book? = null
    private var fileType = ImportedFileType.TXT

    private val _book = MutableLiveData<Book>()
    val book: LiveData<Book>
        get() = _book

    private var currentPages = listOf<CharSequence>()
    private val _pages = MutableLiveData<Event<List<CharSequence>>>()
    val pages: LiveData<Event<List<CharSequence>>>
        get() = _pages

    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> = _dataLoading

    private var _translatedPages = mutableListOf<CharSequence?>()
    val translatedPages: List<CharSequence?>
        get() = _translatedPages

    private val _translatedPageIndex = MutableLiveData<Event<Int>>()
    val translatedPageIndex: LiveData<Event<Int>> = _translatedPageIndex

    private val _translationLoading = MutableLiveData<Boolean>()
    val translationLoading: LiveData<Boolean> = _translationLoading

    private val _translationError= MutableLiveData<Event<String>>()
    val translationError: LiveData<Event<String>> = _translationError

    // Settings
    var hasBottomSheet = false
    var isSheetExpanded = false
    var fullScreen = false

    var languagesISO  = arrayOf<String>()

    var languageFrom = settings.getLanguageFrom()
        set(value) {
            field = value
            settings.setLanguageFrom(value)
        }
    var languageTo = settings.getLanguageTo()
        set(value) {
            if(field != value) {
                field = value
                settings.setLanguageTo(value)
                _translatedPages = arrayOfNulls<CharSequence>(pagesSize).toMutableList()
            }
        }


    fun parseEpub() {
        firstLoad = true
        val reader = fileReader ?: return

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

            initPageSplit(true)
            _dataLoading.value = false
        }
    }

    private suspend fun changeEpubChapter(path: String){
        text = fileReader?.let { epubParser.getChapterBodyTextFromPath(path, it) } ?: ""
    }

    fun parseSimpleText(text: String){
        firstLoad = true
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
        val tempBook = _book.value ?: return

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
            val initialPage = if(currentPage == -1) databaseBookFile?.page ?: 0 else currentPage
            if (initialPage >= pagesSize) pagesSize - 1 else initialPage
        } else if(leftSwipe) pagesSize - 1 else 0

        return currentPage
    }

    private suspend fun initPageSplit(isEpub: Boolean = false) {
        if(isEpub){
            databaseBookFile = getBookFile()
            val initialChapter = if(currentChapter == -1) databaseBookFile?.chapter ?: 0 else currentChapter

            // Create files folder and save image cover
            createFolderForBook()

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

    private suspend fun createFolderForBook(){
        databaseBookFile?.let {

            // Verify folderPath is not empty
            val folderPath = if (it.folderPath.isBlank()) uuid else it.folderPath

            fileReader?.createFileFolder(folderPath)

            val book = currentBook ?: return@let

            val coverId = book.metadata.cover
            val coverPath = book.manifest[coverId]


            coverPath?.let { path ->
                val absCoverPath =
                    File(epubParser.basePath, path).absolutePath.trimStart('/')
                fileReader?.saveCoverBitmap(absCoverPath, folderPath)
            }
        }
    }

    /**
     * Used when you have the file path to the chapter e.g. changing chapters with the table of contents
     */
    fun jumpToChapter(path: String){
        val tempBook = _book.value ?: return

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
        val text = currentPages[index].toString()

        if(translatedPages[index] != null) return
        _translationLoading.value = true

        viewModelScope.launch{
            val result = withContext(Dispatchers.IO) { getTranslationInteractor(text, languageFrom, languageTo) }

            when(result){
                is Result.Success -> {
                    _translatedPages[index] = result.data.definition // In this case is a translation
                    _translatedPageIndex.value = Event(index)
                }
                is Result.Error -> {
                    val error = result.exception
                    error.printStackTrace()
                    _translationError.value = Event(error.message ?: "Unknown error")
                }
            }

            _translationLoading.value = false
        }
    }

    /**
     * Used when you have the index, using the order defined in the spine. E.g. index saved from recent files list
     */
    private suspend fun jumpToChapter(index: Int){
        val tempBook = currentBook ?: return

        val chapterPath = tempBook.spine[index].href

        if(index != -1) {
            currentChapter = index
            changeEpubChapter(chapterPath)
            splitToPages()
        }
    }

    private suspend fun splitToPages() {
        val splitter = pageSplitter ?: return

        splitter.setText(text)
        splitter.split()
        val mutablePages = splitter.getPages().toMutableList()
        if (mutablePages.size == 1 && _book.value != null) mutablePages.add("")
        pagesSize = mutablePages.size

        currentPages = mutablePages
        _translatedPages = arrayOfNulls<CharSequence>(pagesSize).toMutableList()

        _pages.value = Event(mutablePages)
    }

    /**
     * Persists the book data when the user stops interacting with the app (should be called when onPaused())
     */
    fun saveBookData(date: Calendar = Calendar.getInstance(), folderPath: String = uuid) {
        saveBookFileData(date, folderPath)
    }

    private fun saveBookFileData(date: Calendar, path: String){
        val uri = fileUri ?: return
        val book = currentBook ?: return

        // This operation is intended to be synchronous
        // TODO: Change to async, with context call or work manager.
        runBlocking{
            val progress = calculateProgress()

            databaseBookFile?.apply {
                page = currentPage
                chapter = currentChapter
                lastRead = date
                percentageDone = progress
                if(folderPath.isBlank()) folderPath = path
            }

            val title = book.metadata.title
            val bookFile = databaseBookFile ?: BookFile(
                uri,
                title,
                fileType,
                folderPath = path,
                page = currentPage,
                chapter = currentChapter,
                percentageDone =  calculateProgress(),
                lastRead =  date
            )

            fileRepository.saveFile(bookFile)
        }
    }

    private fun calculateProgress(): Int{
        var percentage = 0

        val book = currentBook ?: return percentage
        var sumPreviousChars = 0

        // Sum of previous spine items (chapters)
        for (i in 0 until currentChapter){
            sumPreviousChars += book.spine[i].charCount
        }

        // Sum of previous and current pages
        for(i in 0..currentPage){
            sumPreviousChars += currentPages[i].length
        }

        percentage = 100 * sumPreviousChars / book.totalChars

        return percentage
    }

}