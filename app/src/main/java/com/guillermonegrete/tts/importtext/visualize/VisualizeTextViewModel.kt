package com.guillermonegrete.tts.importtext.visualize

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guillermonegrete.tts.Event
import com.guillermonegrete.tts.common.models.Span
import com.guillermonegrete.tts.data.Result
import com.guillermonegrete.tts.data.Translation
import com.guillermonegrete.tts.data.preferences.SettingsRepository
import com.guillermonegrete.tts.data.source.FileRepository
import com.guillermonegrete.tts.db.BookFile
import com.guillermonegrete.tts.db.ExternalLink
import com.guillermonegrete.tts.db.WordsDAO
import com.guillermonegrete.tts.importtext.ImportedFileType
import com.guillermonegrete.tts.importtext.epub.Book
import com.guillermonegrete.tts.importtext.visualize.model.BookChapter
import com.guillermonegrete.tts.importtext.visualize.model.SplitPageSpan
import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetExternalLink
import com.guillermonegrete.tts.utils.wrapEspressoIdlingResource
import com.guillermonegrete.tts.webreader.AddNoteResult
import com.guillermonegrete.tts.webreader.db.Note
import com.guillermonegrete.tts.webreader.db.NoteDAO
import com.guillermonegrete.tts.webreader.model.ModifiedNote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import timber.log.Timber
import java.io.File
import java.util.*
import javax.inject.Inject

@HiltViewModel
class VisualizeTextViewModel @Inject constructor(
    private val epubParser: EpubParser,
    private val settings: SettingsRepository,
    private val fileRepository: FileRepository,
    private val noteDAO: NoteDAO,
    private val wordDAO: WordsDAO,
    private val getTranslationInteractor: GetLangAndTranslation,
    private val getExternalLinksInteractor: GetExternalLink,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
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
    private val _bookChapter = MutableLiveData<Event<BookChapter>>()
    /**
     * Called every time pages have been processed, called when visualizer starts and
     * when switching between chapters.
     */
    val bookChapter: LiveData<Event<BookChapter>>
        get() = _bookChapter

    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> = _dataLoading

    private var _translatedPages = mutableListOf<Translation?>()
    val translatedPages: List<Translation?>
        get() = _translatedPages

    private val _translatedPageIndex = MutableLiveData<Event<Int>>()
    val translatedPageIndex: LiveData<Event<Int>> = _translatedPageIndex

    private val _translationLoading = MutableLiveData<Boolean>()
    val translationLoading: LiveData<Boolean> = _translationLoading

    private val _translationError = MutableLiveData<Event<String>>()
    val translationError: LiveData<Event<String>> = _translationError

    private val _updatedNote = MutableLiveData<ModifiedNote>()
    val updatedNote: LiveData<ModifiedNote> = _updatedNote

    private val _linksForWord = MutableLiveData<List<ExternalLink>>()
    val linksForWord: LiveData<List<ExternalLink>> = _linksForWord

    // Settings
    var hasBottomSheet = false
    var isSheetExpanded = false
    var fullScreen = false

    var languagesISO  = arrayOf<String>()

    var languageFrom: String = ""
        get() = databaseBookFile?.language ?: "auto"
        set(value) {
            field = value
            val bookFile = databaseBookFile ?: return
            val lang = if (value == "auto") null else value
            databaseBookFile = bookFile.copy(language = lang)
        }
    var languageTo = settings.getLanguageTo()
        set(value) {
            if(field != value) {
                field = value
                settings.setLanguageTo(value)
                _translatedPages = arrayOfNulls<Translation>(pagesSize).toMutableList()
            }
        }

    private val pageWords = MutableStateFlow(emptyList<String>())
    @OptIn(ExperimentalCoroutinesApi::class)
    val pageSavedWords = pageWords.flatMapLatest { words ->
        wordDAO.findWords(words)
    }


    fun parseEpub() {
        firstLoad = true
        val reader = fileReader ?: return

        _dataLoading.value = true
        viewModelScope.launch {
            val parsedBook: Book
            try {
                parsedBook = epubParser.parseBook(reader)
            } catch (e: Exception){
                Timber.e("Error parsing book", e)
                return@launch
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
            val lastChar = databaseBookFile?.lastChar ?: 0
            val initialPage = if(currentPage == -1) getPageIndex(lastChar) else currentPage
            if (initialPage >= pagesSize) pagesSize - 1 else initialPage
        } else if(leftSwipe) pagesSize - 1 else 0

        return currentPage
    }

    private suspend fun initPageSplit(isEpub: Boolean = false) {
        if(isEpub){
            databaseBookFile = getBookFile()
            if (databaseBookFile == null) databaseBookFile = createNewBook()
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
            val folderPath = it.folderPath.ifBlank { uuid }

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
            val result = withContext(ioDispatcher) { getTranslationInteractor(text, languageFrom, languageTo) }

            when(result){
                is Result.Success -> {
                    _translatedPages[index] = result.data
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

        currentChapter = index
        changeEpubChapter(chapterPath)
        splitToPages()
    }

    private suspend fun splitToPages() {
        val splitter = pageSplitter ?: return

        splitter.setText(text)
        splitter.split()
        val mutablePages = splitter.getPages().toMutableList()
        if (mutablePages.size == 1 && _book.value != null) mutablePages.add("")
        pagesSize = mutablePages.size

        currentPages = mutablePages
        _translatedPages = arrayOfNulls<Translation>(pagesSize).toMutableList()

        var notes = emptyList<Note>()
        databaseBookFile?.let { book ->

            // Shift left 24 because the chapter is encoded in the last 8 bits of a 32 bit int.
            val start = currentChapter shl 24
            notes = noteDAO.getFileNotes(book.id, start)
        }
        _bookChapter.value = Event(BookChapter(mutablePages, notes))
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

            val charPos = getCharPos()
            databaseBookFile?.apply {
                lastChar = charPos
                chapter = currentChapter
                lastRead = date
                percentageDone = calculateProgress()
                if(folderPath.isBlank()) folderPath = path
            }

            val title = book.metadata.title
            val bookFile = databaseBookFile ?: BookFile(
                uri,
                title,
                fileType,
                folderPath = path,
                lastChar = charPos,
                chapter = currentChapter,
                percentageDone =  calculateProgress(),
                lastRead =  date
            )

            fileRepository.saveFile(bookFile)
        }
    }

    private suspend fun createNewBook(): BookFile? {
        val uri = fileUri ?: return null
        val book = currentBook ?: return null

        val charPos = getCharPos()
        val title = book.metadata.title
        val bookFile = BookFile(
            uri,
            title,
            fileType,
            folderPath = uuid,
            lastChar = charPos,
            percentageDone =  calculateProgress(),
            lastRead =  Calendar.getInstance()
        )
        val id = fileRepository.saveFile(bookFile)
        bookFile.id = id.toInt()
        return bookFile
    }

    private fun calculateProgress(): Int {

        val book = currentBook ?: return 0
        var sumPreviousChars = 0

        // Sum of previous spine items (chapters)
        for (i in 0 until currentChapter) {
            sumPreviousChars += book.spine[i].charCount
        }

        // Sum of previous and current pages
        for (i in 0..currentPage) {
            sumPreviousChars += currentPages[i].length
        }

        return 100 * sumPreviousChars / book.totalChars
    }

    /**
     * Gets the page that contains [charPos]
     * For example, the third page ranges from 20 to 35, a char with position 23 would be inside that page.
     *
     * @return The page number that contains the character. Returns zero if none contains it.
     */
    private fun getPageIndex(charPos: Int): Int{
        var acc = 0

        currentPages.forEachIndexed { index, page ->
            acc += page.length
            if(charPos < acc) return index
        }

        return 0
    }

    /**
     * Returns the character position of the first element of the current page.
     * For example, the third page ranges from 20 to 35, it returns 20.
     */
    fun getCharPos(): Int {
        var sum = 0
        for (i in 0 until currentPage){
            sum += currentPages[i].length
        }
        return sum
    }

    fun findSelectedSentence(page: Int, charIndex: Int): SplitPageSpan? {
        val translation = translatedPages[page] ?: return null

        var start = 0
        var originStart = 0

        for(sentence in translation.sentences){
            val end = start + sentence.trans.length
            val originalEnd = originStart + sentence.orig.length
            if(charIndex < end){
                // indicate UI to highlight this sentence
                return SplitPageSpan(Span(originStart, originalEnd), Span(start, end))
            }
            start = end
            originStart = originalEnd
        }

        return null
    }

    fun saveNote(newNote: AddNoteResult, originalText: String, position: Int, length: Int, id: Long) {
        val bookId = databaseBookFile?.id ?: return

        viewModelScope.launch {
            wrapEspressoIdlingResource {
                val chapter = currentChapter
                // java int is 32 bits
                val chapterAndPage = (chapter shl 24) or (position and 0x00ffffff)
                val newDbNote = Note(newNote.text, originalText, chapterAndPage, length, newNote.colorHex, null, bookId, id)
                val resultId = noteDAO.upsert(newDbNote)
                // Upsert returns -1 when the operation was an update, use the parameter ID.
                val finalId = if(resultId == -1L) id else resultId
                val result = ModifiedNote.Update(newDbNote.copy(id = finalId))
                _updatedNote.value = result
            }
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            wrapEspressoIdlingResource {
                noteDAO.delete(Note("", "", 0, 0, "", 0, null, id)) // only the id is necessary
                _updatedNote.value = ModifiedNote.Delete(id)
            }
        }
    }

    fun getExternalLinks(word: String) {
        viewModelScope.launch {
            val links = getExternalLinksInteractor(languageFrom, word)
            _linksForWord.value = links
        }
    }

    fun loadLocalWords(words: List<String>) {
        pageWords.value = words
    }

}
