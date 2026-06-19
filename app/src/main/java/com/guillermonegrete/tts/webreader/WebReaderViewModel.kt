package com.guillermonegrete.tts.webreader

import android.os.Parcelable
import androidx.lifecycle.*
import com.guillermonegrete.tts.common.models.Span
import com.guillermonegrete.tts.common.models.hasInside
import com.guillermonegrete.tts.common.models.toUI
import com.guillermonegrete.tts.data.DialogState
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.data.Result
import com.guillermonegrete.tts.data.Translation
import com.guillermonegrete.tts.data.preferences.SettingsRepository
import com.guillermonegrete.tts.data.source.WordRepositorySource
import com.guillermonegrete.tts.data.source.WordRepositorySource.GetWordsCallback
import com.guillermonegrete.tts.db.WebLink
import com.guillermonegrete.tts.db.WebLinkDAO
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.di.DefaultDispatcher
import com.guillermonegrete.tts.di.IoDispatcher
import com.guillermonegrete.tts.importtext.visualize.model.SplitPageSpan
import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation
import com.guillermonegrete.tts.savedwords.ResultType
import com.guillermonegrete.tts.textprocessing.WordState
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetExternalLink
import com.guillermonegrete.tts.utils.deleteAllFolder
import com.guillermonegrete.tts.utils.isWord
import com.guillermonegrete.tts.utils.makeDir
import com.guillermonegrete.tts.utils.wrapEspressoIdlingResource
import com.guillermonegrete.tts.utils.writeToFile
import com.guillermonegrete.tts.webreader.db.Note
import com.guillermonegrete.tts.webreader.db.NoteDAO
import com.guillermonegrete.tts.webreader.db.span
import com.guillermonegrete.tts.webreader.model.ModifiedNote
import com.guillermonegrete.tts.webreader.model.SplitParagraph
import com.guillermonegrete.tts.webreader.model.WordAndLinks
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import net.dankito.readability4j.Readability4J
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.BreakIterator
import java.util.*

@HiltViewModel(assistedFactory = WebReaderViewModel.Factory::class)
class WebReaderViewModel @AssistedInject constructor(
    @Assisted private val url: String,
    private val getTranslationInteractor: GetLangAndTranslation,
    private val getExternalLinksInteractor: GetExternalLink,
    private val wordRepository: WordRepositorySource,
    private val webLinkDAO: WebLinkDAO,
    private val noteDAO: NoteDAO,
    private val settings: SettingsRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
): ViewModel() {

    private val _page = MutableLiveData<LoadResult<PageInfo>>()
    val page: LiveData<LoadResult<PageInfo>>
        get() = _page

    private var cachedParagraphs: List<CachedParagraph>? = null
    private var cachedSplitParagraphs: List<SplitParagraph>? = null

    private var _translatedParagraphs = mutableListOf<Translation?>()
    val translatedParagraphs: List<Translation?>
        get() = _translatedParagraphs

    private val _paragraphState = savedStateHandle.getMutableStateFlow("paragraphState", ParagraphUiState())
    val paragraphState: StateFlow<ParagraphUiState> = _paragraphState

    private val _dialogState = savedStateHandle.getMutableStateFlow("dialogState", UiDialogState())
    val dialogState: StateFlow<UiDialogState> = _dialogState

    private val _editDialogs = savedStateHandle.getMutableStateFlow("editDialogs", UiEditDialogsState())
    val editDialogs: StateFlow<UiEditDialogsState> = _editDialogs

    private val _linksForWord = savedStateHandle.getMutableStateFlow<DialogState<WordAndLinks>>("linksForWord", DialogState.Empty)
    val linksForWord: StateFlow<DialogState<WordAndLinks>> = _linksForWord

    private val _linksSheetExpanded = savedStateHandle.getMutableStateFlow<Boolean?>("linksSheetExpanded", null)
    val linksSheetExpanded: StateFlow<Boolean?> = _linksSheetExpanded

    private val _selectedLink = savedStateHandle.getMutableStateFlow("selectedLink", 0)
    val selectedLink: StateFlow<Int> = _selectedLink

    private val _notes = MutableSharedFlow<List<Note>>()
    val notes: SharedFlow<List<Note>> = _notes

    private val _updatedNote = MutableSharedFlow<ModifiedNote>()
    val updatedNote: SharedFlow<ModifiedNote> = _updatedNote

    private val _updatedWord = MutableSharedFlow<ResultType>()
    val updatedWord: SharedFlow<ResultType> = _updatedWord

    private val _pageSavedWords = MutableSharedFlow<SavedWordsSection>()
    val pageSavedWords: SharedFlow<SavedWordsSection> = _pageSavedWords

    private var cacheWebLink: WebLink? = null

    private val _weblink = MutableLiveData<WebLink>()
    val webLink: LiveData<WebLink>
        get() = _weblink

    private var job: Job? = null
    private val _savedWord = MutableSharedFlow<WordLang>(1)

    private val wordIdToIndexes = hashMapOf<Int, MutableSet<Int>>()

    // Synchronously reads the preference
    var showWords = runBlocking { settings.showSavedWords().first() }
        set(value) {
            field = value
            runBlocking { settings.setShowSavedWords(value) }
        }

    var pageVersion: PageVersion = PageVersion.LOCAL
        private set

    // Path of the app's external storage folder
    var folderPath = ""

    var firstLoad = true

    init {
        loadDoc(url)
    }

    /**
     * Loads the [url] as a string and loads (or creates if it doesn't exist) a database entry for the [url].
     *
     * The [link] representing the url, used usually for testing.
     */
    fun loadDoc(url: String, link: WebLink? = null){
        _page.value = LoadResult.Loading

        viewModelScope.launch {
            wrapEspressoIdlingResource {

                try {
                    var webLink = webLinkDAO.getLink(url)
                    val pageInfo: PageInfo

                    if (webLink != null) {
                        val uuid = webLink.uuid

                        val isLocalPage = uuid != null && pageVersion == PageVersion.LOCAL
                        pageInfo = if (isLocalPage) {
                            PageInfo(readContentFile(uuid), true)
                        } else {
                            PageInfo(getPage(url).content, false)
                        }
                    } else {
                        val page = getPage(url)
                        webLink = link ?: WebLink(url, page.title)
                        pageInfo = PageInfo(page.content, false)
                    }

                    cacheWebLink = webLink
                    _page.value = LoadResult.Success(pageInfo)
                    _dialogState.update { it.copy(isPageSaved = pageInfo.isLocalPage) }
                    // Smart cast is not working with MutableLiveData#setValue, it has to be explicitly cast
                    // Bug report: https://issuetracker.google.com/issues/198313895
                    val safeLink: WebLink = webLink
                    _weblink.value = safeLink
                } catch (ex: IOException) {
                    _page.value = LoadResult.Error(ex)
                }
            }
        }
    }

    fun loadLocalPage() {
        val webLink = cacheWebLink ?: return

        webLink.uuid?.let { uuid ->
            _translatedParagraphs = mutableListOf()
            _page.value = LoadResult.Loading

            viewModelScope.launch {
                try {
                    val content = readContentFile(uuid)
                    _page.value = LoadResult.Success(PageInfo(content, true))
                    _dialogState.update { it.copy(isPageSaved = true) }
                } catch (ex: IOException){
                    _page.value = LoadResult.Error(ex)
                }
            }
        }
    }

    fun loadPageFromWeb(){
        val webLink = cacheWebLink ?: return
        _translatedParagraphs = mutableListOf()

        viewModelScope.launch {
            _page.value = LoadResult.Loading
            try {
                val page = getPage(webLink.url)
                _page.value = LoadResult.Success(PageInfo(page.content, false))
                _dialogState.update { it.copy(isPageSaved = false) }
            } catch (ex: IOException){
                _page.value = LoadResult.Error(ex)
            }
        }
    }

    /**
     * Reads the HTML file saved in the local storage. With [uuid] being the folder name.
     */
    private fun readContentFile(uuid: UUID): String {
        val rootFolder = File(folderPath, uuid.toString())
        val file = File(rootFolder, PAGE_FILENAME)
        val doc = Jsoup.parse(file, null)
        return doc.outerHtml()
    }

    private suspend fun getPage(url: String): Page = withContext(ioDispatcher){
        val result = runCatching {
            val doc = Jsoup.connect(url).get()
            val readability4J = Readability4J(url, doc)
            val content = readability4J.parse().content ?: simpleParse(doc)
            Page(doc.title(), content)
        }

        return@withContext result.getOrThrow()
    }

    private fun simpleParse(doc: Document): String {
        doc.body().select("menu, header, footer, logo, nav, search, link, button, btn, ad, script, style, noscript, img," +
                "form,fieldset,object,embed,link,iframe,input,textarea,select").remove()
        for (element in doc.select("*")) {
            if (!element.hasText() && element.isBlock) element.remove()
        }
        return doc.body().html()
    }

    fun saveWebLink(charPosition: Int) {
        viewModelScope.launch {
            cacheWebLink?.let {
                val newWebLink = it.copy(lastRead = Calendar.getInstance(), charPosition = charPosition)
                webLinkDAO.upsert(newWebLink)
            }
        }
    }

    fun createParagraphs(paragraphs: List<CharSequence>): List<SplitParagraph> {
        var splitParagraphs = cachedSplitParagraphs
        if(splitParagraphs == null) {

            splitParagraphs = splitBySentence(paragraphs)
            if (_translatedParagraphs.isEmpty())
                _translatedParagraphs = arrayOfNulls<Translation>(splitParagraphs.size).toMutableList()
        }
        return splitParagraphs
    }

    fun translateParagraph() : Boolean {
        val currentParagraph = _paragraphState.value.paragraph
        val pos = currentParagraph?.index ?: return false
        val paragraphs = cachedParagraphs ?: return false
        val paragraph = paragraphs[pos].translation

        val language = cacheWebLink?.language
        if(paragraph.translation.isNotBlank() && paragraph.sourceLang == language) {
            val state = currentParagraph.copy(translation = _translatedParagraphs[pos])
            _paragraphState.update { it.copy(paragraph = state) }
            return true
        }

        _paragraphState.update { it.copy(paragraph = currentParagraph.copy(isLoading = true)) }

        viewModelScope.launch {
            wrapEspressoIdlingResource {
                val result = withContext(ioDispatcher) {
                    getTranslationInteractor(paragraph.original, languageFrom = language ?: "auto")
                }

                when(result){
                    is Result.Success -> {
                        val translation = result.data
                        paragraph.translation = translation.translatedText
                        paragraph.sourceLang = translation.src
                        _translatedParagraphs[pos] = translation
                        _paragraphState.update { it.copy(paragraph = it.paragraph?.copy(isLoading = false, translation = translation)) }
                    }
                    is Result.Error -> _paragraphState.update { it.copy(paragraph = it.paragraph?.copy(isLoading = false)) }
                }
            }
        }

        return true
    }

    fun translateWord(word: String, span: Span) {
        if (showWords) {
            // This text is not a saved word, so skip directly to translation
            fetchTranslation(word, span, false, false) // it's a tapped word, no overlap with notes or saved words
        } else {
            // The word might be saved, query the database first to check
            searchSavedWord(word, null, span)
        }

        hideWordLinks()
    }

    fun translateText(text: String, span: Span, overlapsNote: Boolean, overlapsWord: Boolean) {
        if (showWords || !text.isWord()) {
            // This text is not a saved word, so skip directly to translation
            fetchTranslation(text, span, overlapsNote, overlapsWord)
        } else {
            // The text might be a saved word, query the database first to check
            viewModelScope.launch {
                _savedWord.emit(WordLang(text, null))
            }

            _dialogState.update { it.copy(isLoading = true) }

            launchSearchWordJob(span, overlapsNote, overlapsWord)
        }
    }

    fun translateWordInSentence(text: String, span: Span) {
        _dialogState.update { it.copy(isWordLoading = true) }

        if (showWords) {
            viewModelScope.launch {
                getTranslation(text) { translation ->
                    val translation = SimpleTranslation(text, translation.translatedText, translation.src)
                    _dialogState.update { it.copy(dialogState = DialogType.Translation(translation, span, false, false), isWordLoading = false) }
                }
            }
        } else {
            // The word might be a saved, query the database first to check
            viewModelScope.launch {
                _savedWord.emit(WordLang(text, null))
            }

            _dialogState.update { it.copy(isWordLoading = true) }

            launchSearchWordJob(span, false, false)
        }
    }

    fun clearTextInfo() {
        _dialogState.update { it.copy(dialogState = null, sentence = null, isLoading = false, isWordLoading = false) }
    }

    fun setNoteData(note: Note, sentenceSpan: Span?) {
        val noteSpan = note.span
        val sentence = if (sentenceSpan != null && sentenceSpan.hasInside(noteSpan))
            _dialogState.value.sentence
        else {
            unselectSentence()
            null
        }

        hideWordLinks()
        _dialogState.update { it.copy(dialogState = DialogType.Note(note), sentence = sentence) }
    }

    fun searchSavedWord(word: String, lang: String?, wordSpan: Span) {
        viewModelScope.launch {
            _savedWord.emit(WordLang(word, lang))
        }

        _dialogState.update { it.copy(isLoading = true, sentence = null) }

        launchSearchWordJob(wordSpan, false, false)
    }

    fun setSavedWord(id: Int, wordSpan: Span, sentenceSpan: Span?) {
        if (sentenceSpan != null && sentenceSpan.hasInside(wordSpan)) {
            _dialogState.update { it.copy(isWordLoading = true) }
        } else {
            _dialogState.update { it.copy(isLoading = true, sentence = null) }
            unselectSentence()
        }

        hideWordLinks()
        launchWordJob(id, wordSpan)
    }

    fun setSavedWord(id: Int, wordSpan: Span) {
        val sentence = _dialogState.value.sentence
        if (sentence != null)
            _dialogState.update { it.copy(isWordLoading = true) }
        else
            _dialogState.update { it.copy(isLoading = true) }

        launchWordJob(id, wordSpan)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun launchSearchWordJob(span: Span, overlapsNote: Boolean, overlapsWord: Boolean) {
        job?.cancel()
        job = viewModelScope.launch {
            _savedWord.flatMapLatest { data ->
                wordRepository.getLocalWord(data.word, data.lang ?: cacheWebLink?.language)
                    .distinctUntilChanged()
                    .asFlow()
            }.collectLatest { dbWord ->
                if (dbWord != null) {
                    _dialogState.update { it.copy(dialogState = DialogType.SavedWord(dbWord, span), isLoading = false, isWordLoading = false) }
                } else {
                    if (!showWords) {
                        val data = _savedWord.first()
                        getTranslationInfo(data.word, span, overlapsNote, overlapsWord)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun launchWordJob(id: Int, span: Span) {
        job?.cancel()
        job = viewModelScope.launch {
            wordRepository.getLocalWord(id)
                .distinctUntilChanged()
                .asFlow()
                .collectLatest { dbWord ->
                    if (dbWord != null) {
                        _dialogState.update { it.copy(dialogState = DialogType.SavedWord(dbWord, span), isLoading = false, isWordLoading = false) }
                    }
                }
        }
    }

    private fun fetchTranslation(text: String, span: Span, overlapsNote: Boolean, overlapsWord: Boolean) {
        _dialogState.update { it.copy(isLoading = true, sentence = null) }
        unselectSentence()

        viewModelScope.launch {
            getTranslation(text) { translation ->
                viewModelScope.launch {
                    val translation = SimpleTranslation(text, translation.translatedText, translation.src)
                    _dialogState.update { it.copy(dialogState = DialogType.Translation(translation, span, overlapsNote, overlapsWord), isLoading = false) }
                }
            }
        }
    }

    private suspend fun getTranslationInfo(text: String, span: Span, overlapsNote: Boolean, overlapsWord: Boolean) {
        getTranslation(text) { translation ->
            viewModelScope.launch {
                val translation = SimpleTranslation(text, translation.translatedText, translation.src)
                _dialogState.update { it.copy(dialogState = DialogType.Translation(translation, span, overlapsNote, overlapsWord), isWordLoading = false, isLoading = false) }
            }
        }
    }

    private suspend fun getTranslation(text: String, onResult: (Translation) -> Unit) {
        val result = withContext(ioDispatcher) {
            getTranslationInteractor(text, languageFrom = cacheWebLink?.language ?: "auto")
        }

        when(result){
            is Result.Success -> onResult(result.data)
            is Result.Error -> _dialogState.update { it.copy(error = result.exception.message, isLoading = false, isWordLoading = false) }
        }
    }

    fun setSentenceInParagraph(paragraphPos: Int, charIndex: Int) {
        val span = findSelectedSentence(paragraphPos, charIndex)
        _paragraphState.update { it.copy(paragraph = it.paragraph?.copy(highlights = span)) }
    }

    fun findSelectedSentence(paragraphPos: Int, charIndex: Int): SplitPageSpan? {
        val translation = translatedParagraphs[paragraphPos] ?: return null
        // Highlighting only makes sense where there are at least 2 sentences
        if(translation.sentences.size <= 1) return null

        var start = 0
        var originStart = 0

        for(sentence in translation.sentences){
            val end = start + sentence.trans.length
            val originalEnd = originStart + sentence.orig.length
            if(charIndex < end) {
                // indicate UI to highlight this sentence
                return SplitPageSpan(Span(originStart, originalEnd), Span(start, end))
            }
            start = end
            originStart = originalEnd
        }

        return null
    }

    /**
     * Called when a [word] in the original text is clicked. The [pos] being the index of the paragraph in the list.
     *
     * In this case, retrieves the external link for the language of the word and emits them.
     */
    fun onParagraphWordClicked(word: String, pos: Int, wordSpan: Span) {

        // first try to get the language from a translation, if not from the set language, else ignore.
        val lang = translatedParagraphs.getOrNull(pos)?.src ?: cacheWebLink?.language ?: return
        clearTextInfo()
        _paragraphState.update { it.copy(paragraph = it.paragraph?.copy(selectedWord = wordSpan)) }

        getLinksForWord(word, lang)
    }

    fun getLinksForWord(word: String, lang: String) {
        viewModelScope.launch {
            try {
                val links = withContext(ioDispatcher) { getExternalLinksInteractor(lang) }
                _linksForWord.value = DialogState.Success(WordAndLinks(word, links))
                _linksSheetExpanded.value = false
                // if out of index, default to the first item (zero index)
                if(_selectedLink.value >= links.size) _selectedLink.value = 0
            } catch (e: Exception) {
                _linksForWord.value = DialogState.Error(e)
            }
        }
    }

    fun getLinksForWord(word: String) {
        val lang = cacheWebLink?.language ?: return
        getLinksForWord(word, lang)
    }

    fun hideWordLinks() {
        _linksForWord.value = DialogState.Empty
        _linksSheetExpanded.value = null
        _paragraphState.update { it.copy(paragraph = it.paragraph?.copy(selectedWord = null)) }
    }

    fun setWordLink(position: Int) {
        _selectedLink.value = position
    }

    fun setLanguage(langShort: String?) {
        cacheWebLink?.language = langShort
    }

    fun getLanguage() = cacheWebLink?.language

    fun getWebLinkId() = cacheWebLink?.id

    fun getCharPos() = cacheWebLink?.charPosition ?: 0

    fun getGesturePreferences() = settings.getGestures()

    fun paragraphWordSelected() = _paragraphState.value.paragraph?.selectedWord != null

    private fun splitBySentence(paragraphs: List<CharSequence>): List<SplitParagraph> {
        val iterator = BreakIterator.getSentenceInstance()

        val splitParagraphs = arrayListOf<SplitParagraph>()
        val cachedParagraphs = arrayListOf<CachedParagraph>()

        for (paragraph in paragraphs){
            iterator.setText(paragraph.toString())
            var start = iterator.first()
            var end = iterator.next()
            val indexes = arrayListOf<Span>()
            val sentences = arrayListOf<String>()
            val cachedSentences = arrayListOf<SimpleTranslation>()
            while (end != BreakIterator.DONE) {
                val sentence = paragraph.substring(start, end)
                sentences.add(sentence)
                cachedSentences.add(SimpleTranslation(sentence))
                indexes.add(Span(start, end))
                start = end
                end = iterator.next()
            }
            splitParagraphs.add(SplitParagraph(paragraph, indexes, sentences))
            cachedParagraphs.add(CachedParagraph(SimpleTranslation(paragraph.toString()), cachedSentences))
        }
        this.cachedParagraphs = cachedParagraphs
        return splitParagraphs
    }

    fun translateSentence(paragraphIndex: Int, sentenceIndex: Int) {
        val paragraphs = cachedParagraphs ?: return
        val sentence = paragraphs[paragraphIndex].sentences[sentenceIndex]

        val language = cacheWebLink?.language
        if(sentence.translation.isNotBlank() && sentence.sourceLang == language) {
            val sentence = Sentence(sentence.translation, paragraphIndex)
            _dialogState.update { it.copy(sentence = sentence, dialogState = null) }
            return
        }

        viewModelScope.launch {

            _dialogState.update { it.copy(isLoading = true) }

            wrapEspressoIdlingResource {
                getTranslation(sentence.original) { translation ->
                    sentence.translation = translation.translatedText
                    sentence.sourceLang = translation.src
                    viewModelScope.launch {
                        val sentence = Sentence(translation.translatedText, paragraphIndex)
                        _dialogState.update { it.copy(sentence = sentence, dialogState = null, isLoading = false) }
                    }
                }
            }
        }
    }

    fun saveWebLinkFolder(rootPath: String, uuid: UUID, content: String) {
        val link = cacheWebLink ?: return
        val folder = File(rootPath, uuid.toString())

        if (!makeDir(folder)) return

        val contentFile = File(folder, PAGE_FILENAME)
        writeToFile(contentFile, content)

        link.uuid = uuid

        viewModelScope.launch {
            cacheWebLink?.let { link ->
                link.lastRead = Calendar.getInstance()
                webLinkDAO.upsert(link)
                cacheWebLink = webLinkDAO.getLink(link.url)
                _dialogState.update { it.copy(isPageSaved = true) }
            }
        }
    }

    fun deleteLinkFolder(rootPath: String) {
        val link = cacheWebLink ?: return
        val uuid = link.uuid ?: return
        deleteAllFolder(File(rootPath, uuid.toString()))
        link.uuid = null

        viewModelScope.launch {
            webLinkDAO.update(link)
            noteDAO.deleteByLinkId(link.id)
            loadPageFromWeb()
        }
    }

    fun saveCurrentNote(noteText: String, color: String) {
        val type = _editDialogs.value.isEditingType
        if (type is DialogType.Note) {
            val noteItem = type.item
            saveNote(noteItem.originalText, noteText, noteItem.span, noteItem.id, color)
        }
    }

    fun saveNote(text: String, noteText: String, selection: Span, id: Long, color: String) {
        val webLink = cacheWebLink ?: return
        viewModelScope.launch {
            wrapEspressoIdlingResource {
                val newNote = Note(noteText, text, selection.start, selection.end - selection.start, color, webLink.id, null, id)
                val resultId = noteDAO.upsert(newNote)
                // Upsert returns -1 when the operation was an update, use the parameter ID.
                val finalId = if(resultId == -1L) id else resultId
                val updatedNote = newNote.copy(id = finalId)
                _updatedNote.emit(ModifiedNote.Update(updatedNote))
                _editDialogs.update { it.copy(isEditingType = null) }
                _dialogState.update { it.copy(dialogState = DialogType.Note(updatedNote)) }
            }
        }
    }

    fun deleteCurrentNote() {
        val type = _editDialogs.value.isEditingType
        if (type is DialogType.Note) deleteNote(type.item.id)
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            wrapEspressoIdlingResource {
                noteDAO.delete(Note("", "", 0, 0, "", 0, null, id)) // only the id is necessary
                _updatedNote.emit(ModifiedNote.Delete(id))
                _editDialogs.update { it.copy(isEditingType = null, isDeleteDialogShown = false) }
                _dialogState.update { it.copy(dialogState = null) }
            }
        }
    }

    fun loadLocalWords(
        texts: List<String>,
        index: Int,
        onFinished: () -> Unit = {}
    ) {
        if (!showWords) return

        viewModelScope.launch {
            val sections = withContext(defaultDispatcher) { splitByWords(texts) }
            val words = sections.flatMap { it.words }
            wordRepository.findWords(words, object : GetWordsCallback {
                override fun onWordsLoaded(words: List<Words>) {
                    viewModelScope.launch {
                        val paragraphWords = withContext(defaultDispatcher) { findWordsInSections(words, sections, index) }
                        _pageSavedWords.emit(SavedWordsSection(paragraphWords, index))
                        onFinished()
                    }
                }

                override fun onDataNotAvailable(exception: Exception) {
                    Timber.e(exception, "Error loading db words for ${index..(index + texts.lastIndex)}")
                }
            })
        }
    }

    private fun findWordsInSections(dbWords: List<Words>, sections: List<WordSpans>, position: Int): List<List<WordState>> {
        return sections.mapIndexed { index, wordSpans ->
            findWordsInSection(dbWords, wordSpans, position + index)
        }
    }

    private fun findWordsInSection(dbWords: List<Words>, section: WordSpans, position: Int): List<WordState> {
        val words = arrayListOf<WordState>()
        section.words.forEachIndexed { index, word ->
            val dbWord = dbWords.find { it.word == word }
            if (dbWord != null) {
                words.add(WordState(dbWord.toUI(), dbWord.id, section.spans[index]))
                // Store position of the respective word id.
                val positions = wordIdToIndexes.getOrPut(dbWord.id, ::mutableSetOf)
                positions.add(position)
            }
        }
        return words
    }

    fun findWordsInParagraph(dbWords: List<Words>, text: String, position: Int): List<WordState> {
        val iterator = BreakIterator.getWordInstance()
        iterator.setText(text)
        var start = iterator.first()
        var end = iterator.next()

        val words = arrayListOf<WordState>()
        while (end != BreakIterator.DONE) {
            val possibleWord = text.substring(start, end)
            val dbWord = dbWords.find { it.word == possibleWord }
            if (dbWord != null) {
                words.add(WordState(dbWord.toUI(), dbWord.id, Span(start, end)))
                // Store position of the respective word id.
                val positions = wordIdToIndexes.getOrPut(dbWord.id, ::mutableSetOf)
                positions.add(position)
            }
            start = end
            end = iterator.next()
        }

        return words
    }

    /**
     * Takes a list of sections of the text (e.g. paragraphs of a page) and turns each section into an object containing
     * all the words and their positions with the text.
     */
    private fun splitByWords(sections: List<String>): List<WordSpans> {
        return sections.map { text ->
            val words = arrayListOf<String>()
            val spans = arrayListOf<Span>()
            val iterator = BreakIterator.getWordInstance()
            iterator.setText(text)
            var start = iterator.first()
            var end = iterator.next()

            while (end != BreakIterator.DONE) {
                val possibleWord = text.substring(start, end)
                if (possibleWord.isNotBlank()) {
                    words.add(possibleWord)
                    spans.add(Span(start, end))
                }
                start = end
                end = iterator.next()
            }
            WordSpans(words, spans)
        }
    }

    fun getWordIndexes(id: Int) = wordIdToIndexes[id] ?: emptySet()

    fun upsert(word: Words, wordSpan: Span) {
        viewModelScope.launch {
            val resultId = withContext(ioDispatcher) { wordRepository.upsert(word) }
            if(resultId != -1L) {
                word.id = resultId.toInt()
                setSavedWord(word.id, wordSpan)
                _updatedWord.emit(ResultType.Insert(word))
            }
            _editDialogs.update { it.copy(isEditingType = null) }
        }
    }

    fun deleteWord(word: Words) {
        viewModelScope.launch {
            withContext(ioDispatcher) { wordRepository.deleteWord(word) }
            _updatedWord.emit(ResultType.Delete(word.id))
            _dialogState.update { it.copy(dialogState = null) }
            _editDialogs.update { it.copy(isEditingType = null, isDeleteDialogShown = false) }
        }
    }

    fun resetWordData() {
        wordIdToIndexes.clear()
    }

    fun startEditing(type: DialogType) {
        _editDialogs.update { it.copy(isEditingType = type) }
    }

    fun startEditing() {
        val state = dialogState.value
        val type = state.dialogState ?: return
        when(type) {
            is DialogType.Note -> _editDialogs.update { it.copy(isEditingType = type) }
            is DialogType.SavedWord -> _editDialogs.update { it.copy(isEditingType = type) }
            is DialogType.Translation -> {
                val isWord = type.translation.original.isWord()

                if (isWord && !type.overlapsWord) {
                    if (state.isPageSaved && !type.overlapsNote) {
                        _editDialogs.update { it.copy(isEditingType = type) }
                    } else {
                        val trans = type.translation
                        val word = Words(trans.original, trans.sourceLang, trans.translation)
                        _editDialogs.update { it.copy(isEditingType = DialogType.SavedWord(word, type.span)) }
                    }
                } else {
                    if (state.isPageSaved && !type.overlapsNote) {
                        _editDialogs.update { it.copy(isEditingType = DialogType.Note(type.toNote())) }
                    }
                }
            }
        }
    }

    fun stopEditing() {
        _editDialogs.update { it.copy(isEditingType = null) }
    }

    fun setDeleteSate(isShown: Boolean) {
        _editDialogs.update { it.copy(isDeleteDialogShown = isShown) }
    }

    fun setPickInfoType(word: DialogType.SavedWord, note: DialogType.Note) {
        _editDialogs.update { it.copy(isPickingType = InfoType(word, note)) }
    }

    fun stopPickingInfo() {
        _editDialogs.update { it.copy(isPickingType = null) }
    }

    fun pickItem(isNote: Boolean, sentenceSpan: Span?) {
        val info = _editDialogs.value.isPickingType ?: return
        if (isNote) {
            setNoteData(info.note.item, sentenceSpan)
        } else {
            val state = info.word
            setSavedWord(state.word.id, state.span, sentenceSpan)
        }
    }

    /**
     * Create a new note from the current translation.
     */
    fun newNote() {
        val type = dialogState.value.dialogState
        if (type is DialogType.Translation) {
            _editDialogs.update { it.copy(isEditingType = DialogType.Note(type.toNote())) }
        }
    }

    /**
     * Create a new saved word from the current translation.
     */
    fun newSavedWord() {
        val type = dialogState.value.dialogState
        if (type is DialogType.Translation) {
            val trans = type.translation
            val word = Words(trans.original, trans.sourceLang, trans.translation)
            _editDialogs.update { it.copy(isEditingType = DialogType.SavedWord(word, type.span)) }
        }
    }

    fun setPageVersion(version: PageVersion) {
        when (version) {
            PageVersion.LOCAL -> loadLocalPage()
            PageVersion.WEB -> loadPageFromWeb()
        }

        pageVersion = version
    }

    fun sentenceSelected(paragraphIndex: Int, sentenceIndex: Int) {
        _paragraphState.update { it.copy(paragraphIndex = paragraphIndex, sentenceIndex = sentenceIndex, paragraph = null) }
        _linksForWord.value = DialogState.Empty
        _linksSheetExpanded.value = null
        _dialogState.update { it.copy(sentence = null, dialogState = null) }
    }

    fun unselectSentence() {
        _paragraphState.update { it.copy(paragraphIndex = null, sentenceIndex = null) }
    }

    fun paragraphSelected(index: Int?, pos: Int? = null) {
        if (index == null) {
            _paragraphState.update { it.copy(paragraph = null) }
            return
        }

        val state = _paragraphState.value
        val paragraph = state.paragraph
        if (paragraph?.index == index) {
            _paragraphState.update { it.copy(paragraph = null) }
        } else {
            val dialog = _dialogState.value
            if (dialog.sentence?.paragraphIndex == index) {
                // unselect sentence if it's in the same paragraph
                clearTextInfo()
                _paragraphState.update { it.copy(paragraph = SelectedParagraph(index), paragraphIndex = null, sentenceIndex = null) }
            } else {
                if (dialog.dialogState is DialogType.Note ||
                    dialog.dialogState is DialogType.SavedWord ||
                    index == pos) clearTextInfo()
                _paragraphState.update { it.copy(paragraph = SelectedParagraph(index)) }
            }
        }
    }

    fun errorShown() {
        _dialogState.update { it.copy(error = null) }
    }

    fun getNotes() {
        val page = _page.value
        if (page is LoadResult.Success<PageInfo>) {
            viewModelScope.launch {
                if (page.data.isLocalPage) {
                    val webLink = cacheWebLink ?: return@launch
                    _notes.emit(noteDAO.getNotes(webLink.id))
                } else {
                    _notes.emit(emptyList())
                }
            }
        }
    }

    fun setLinkSheetState(isExpanded: Boolean) {
        _linksSheetExpanded.value = isExpanded
    }

    data class CachedParagraph(val translation: SimpleTranslation, val sentences: List<SimpleTranslation>)

    data class Page(val title: String, val content: String)

    data class WordSpans(val words: List<String>, val spans: List<Span>)

    data class WordLang(val word: String, val lang: String?)

    companion object {
        private const val PAGE_FILENAME = "content.xml"
    }

    @Parcelize
    data class UiDialogState(
        val isLoading: Boolean = false,
        val isWordLoading: Boolean = false,
        val isPageSaved: Boolean = false,
        val dialogState: DialogType? = null,
        val sentence: Sentence? = null,
        val error: String? = null,
    ): Parcelable

    @Parcelize
    data class UiEditDialogsState(
        val isEditingType: DialogType? = null,
        val isDeleteDialogShown: Boolean = false,
        val isPickingType: InfoType? = null,
    ): Parcelable

    @Parcelize
    data class ParagraphUiState(
        val paragraph: SelectedParagraph? = null,
        val paragraphIndex: Int? = null,
        val sentenceIndex: Int? = null,
    ): Parcelable

    @AssistedFactory
    interface Factory {
        fun create(url: String): WebReaderViewModel
    }
}

@Parcelize
data class SimpleTranslation(val original: String, var translation: String = "", var sourceLang: String = ""): Parcelable

@Parcelize
data class Sentence(val text: String, val paragraphIndex: Int): Parcelable

@Parcelize
data class SelectedParagraph(
    val index: Int,
    val isLoading: Boolean = false,
    val translation: Translation? = null,
    val highlights: SplitPageSpan? = null,
    val selectedWord: Span? = null,
): Parcelable

@Parcelize
sealed interface DialogType: Parcelable {
    data class SavedWord(val word: Words, val span: Span): DialogType
    data class Note(val item: com.guillermonegrete.tts.webreader.db.Note): DialogType
    data class Translation(val translation: SimpleTranslation, val span: Span, val overlapsNote: Boolean, val overlapsWord: Boolean): DialogType
}

fun DialogType.Translation.toNote() = Note(translation.translation, translation.original, span.start, span.end - span.start, "")

@Parcelize
data class InfoType(val word: DialogType.SavedWord, val note: DialogType.Note): Parcelable

/**
 * Return class for the UI, used to display the paragraph with the notes
 */
data class PageInfo(val text: String, val isLocalPage: Boolean)

data class SavedWordsSection(val words: List<List<WordState>>, val start: Int)

enum class PageVersion {
    LOCAL,
    WEB
}
