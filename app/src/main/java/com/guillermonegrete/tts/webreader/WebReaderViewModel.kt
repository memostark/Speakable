package com.guillermonegrete.tts.webreader

import androidx.lifecycle.*
import com.guillermonegrete.tts.common.models.Span
import com.guillermonegrete.tts.common.models.toUI
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.data.Result
import com.guillermonegrete.tts.data.Translation
import com.guillermonegrete.tts.data.source.WordRepositorySource
import com.guillermonegrete.tts.data.source.WordRepositorySource.GetWordsCallback
import com.guillermonegrete.tts.db.WebLink
import com.guillermonegrete.tts.db.WebLinkDAO
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.importtext.visualize.model.SplitPageSpan
import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation
import com.guillermonegrete.tts.savedwords.ResultType
import com.guillermonegrete.tts.textprocessing.WordState
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetExternalLink
import com.guillermonegrete.tts.utils.deleteAllFolder
import com.guillermonegrete.tts.utils.makeDir
import com.guillermonegrete.tts.utils.wrapEspressoIdlingResource
import com.guillermonegrete.tts.utils.writeToFile
import com.guillermonegrete.tts.webreader.db.Note
import com.guillermonegrete.tts.webreader.db.NoteDAO
import com.guillermonegrete.tts.webreader.model.ModifiedNote
import com.guillermonegrete.tts.webreader.model.SplitParagraph
import com.guillermonegrete.tts.webreader.model.WordAndLinks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import org.jsoup.Jsoup
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.BreakIterator
import java.util.*
import javax.inject.Inject

@HiltViewModel
class WebReaderViewModel @Inject constructor(
    private val getTranslationInteractor: GetLangAndTranslation,
    private val getExternalLinksInteractor: GetExternalLink,
    private val wordRepository: WordRepositorySource,
    private val webLinkDAO: WebLinkDAO,
    private val noteDAO: NoteDAO,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
): ViewModel() {

    private val _page = MutableLiveData<LoadResult<PageInfo>>()
    val page: LiveData<LoadResult<PageInfo>>
        get() = _page

    private var cachedParagraphs: List<CachedParagraph>? = null
    private var cachedSplitParagraphs: List<SplitParagraph>? = null

    private var _translatedParagraphs = mutableListOf<Translation?>()
    val translatedParagraphs: List<Translation?>
        get() = _translatedParagraphs

    private val _translatedParagraph = MutableLiveData<LoadResult<Int>>()
    val translatedParagraph: LiveData<LoadResult<Int>> = _translatedParagraph

    private val _textInfo = MutableLiveData<LoadResult<WordResult>>()
    val textInfo: LiveData<LoadResult<WordResult>> = _textInfo

    private val _wordInfo = MutableLiveData<LoadResult<WordResult>>()
    val wordInfo: LiveData<LoadResult<WordResult>> = _wordInfo

    private val _linksForWord = MutableLiveData<WordAndLinks>()
    val linksForWord: LiveData<WordAndLinks> = _linksForWord

    private val _updatedNote = MutableLiveData<ModifiedNote>()
    val updatedNote: LiveData<ModifiedNote> = _updatedNote

    private val _updatedWord = MutableLiveData<ResultType>()
    val updatedWord: LiveData<ResultType> = _updatedWord

    private val _pageSavedWords = MutableSharedFlow<SavedWordsSection>()
    val pageSavedWords: SharedFlow<SavedWordsSection> = _pageSavedWords

    private var cacheWebLink: WebLink? = null

    private val _weblink = MutableLiveData<WebLink>()
    val webLink: LiveData<WebLink>
        get() = _weblink

    private var job: Job? = null
    private val _savedWord = MutableSharedFlow<String>(1)

    private val wordIdToIndexes = hashMapOf<Int, MutableSet<Int>>()

    // Path of the app's external storage folder
    var folderPath = ""

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
                    val linkAndNotes = webLinkDAO.getLinkWithNotes(url)
                    val pageInfo: PageInfo
                    val webLink: WebLink

                    if (linkAndNotes != null) {
                        webLink = linkAndNotes.webLink
                        val uuid = webLink.uuid
                        val pageContent = if (uuid != null) {
                            readContentFile(uuid)
                        } else {
                            val page = getPage(url)
                            page.content
                        }
                        val isPageSaved = uuid != null
                        pageInfo = PageInfo(pageContent, linkAndNotes.notes, isPageSaved)
                    } else {
                        val page = getPage(url)
                        webLink = link ?: WebLink(url, page.title)
                        pageInfo = PageInfo(page.content, emptyList(), false)
                    }

                    _page.value = LoadResult.Success(pageInfo)
                    cacheWebLink = webLink
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
            _page.value = LoadResult.Loading

            viewModelScope.launch {
                try {
                    val notes = noteDAO.getNotes(webLink.id)
                    val content = readContentFile(uuid)
                    _page.value = LoadResult.Success(PageInfo(content, notes, true))
                } catch (ex: IOException){
                    _page.value = LoadResult.Error(ex)
                }
            }
        }
    }

    fun loadPageFromWeb(){
        val webLink = cacheWebLink ?: return

        viewModelScope.launch {
            _page.value = LoadResult.Loading
            try {
                val page = getPage(webLink.url)
                _page.value = LoadResult.Success(PageInfo(page.content, emptyList(), false))
            } catch (ex: IOException){
                _page.value = LoadResult.Error(ex)
            }
        }
    }

    /**
     * Reads the html file saved in the local storage. With [uuid] being the folder name.
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
            doc.body().select("menu, header, footer, logo, nav, search, link, button, btn, ad, script, style, img").remove()
            // Removes empty tags (e.g. <div></div>) and keeps self closing tags e.g. <br/>
            for (element in doc.select("*")) {
                if (!element.hasText() && element.isBlock) element.remove()
            }
            Page(doc.title(), doc.body().html())
        }

        return@withContext result.getOrThrow()
    }

    fun saveWebLink(){
        viewModelScope.launch {
            cacheWebLink?.let {
                it.lastRead = Calendar.getInstance()
                webLinkDAO.upsert(it)
            }
        }
    }

    fun createParagraphs(paragraphs: List<CharSequence>): List<SplitParagraph> {
        var splitParagraphs = cachedSplitParagraphs
        if(splitParagraphs == null) {

            splitParagraphs = splitBySentence(paragraphs)
            _translatedParagraphs = arrayOfNulls<Translation>(splitParagraphs.size).toMutableList()
        }
        return splitParagraphs
    }

    fun translateParagraph(pos: Int) {
        val paragraphs = cachedParagraphs ?: return
        val paragraph = paragraphs[pos].translation

        val language = cacheWebLink?.language
        if(paragraph.translation.isNotBlank() && paragraph.sourceLang == language) return

        _translatedParagraph.value = LoadResult.Loading

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
                        _translatedParagraph.value = LoadResult.Success(pos)
                    }
                    is Result.Error -> _translatedParagraph.value = LoadResult.Error(result.exception)
                }
            }
        }
    }

    fun translateText(text: String){
        translateText(text, _textInfo)
    }

    fun translateWordInSentence(text: String){
        translateText(text, _wordInfo)
    }

    fun setSavedWord(word: String) {
        viewModelScope.launch {
            _savedWord.emit(word)
        }
        if (job == null) {
            launchWordJob()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun launchWordJob() {
        job = viewModelScope.launch {
            _savedWord.flatMapLatest { word ->
                wordRepository.getLocalWord(word, cacheWebLink?.language ?: "en")
                    .distinctUntilChanged()
                    .asFlow()
            }.collectLatest {
                if (it != null) {
                    _updatedWord.value = ResultType.Update(it)
                }
            }
        }
    }

    private fun translateText(text: String, observer: MutableLiveData<LoadResult<WordResult>>) {
        observer.value = LoadResult.Loading

        viewModelScope.launch {
            getTranslation(text) { translation ->
                val word = Words(text, translation.src, translation.translatedText)
                observer.value = LoadResult.Success(WordResult(word, false))
            }
        }
    }

    private suspend fun getTranslation(text: String, onResult: (Translation) -> Unit) {
        val result = withContext(ioDispatcher) {
            getTranslationInteractor(text, languageFrom = cacheWebLink?.language ?: "auto")
        }

        when(result){
            is Result.Success -> onResult(result.data)
            is Result.Error -> _textInfo.value = LoadResult.Error(result.exception)
        }
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
    fun onWordClicked(word: String, pos: Int) {

        // first try to get the language from a translation, if not from the set language, else ignore.
        val lang = translatedParagraphs.getOrNull(pos)?.src ?: cacheWebLink?.language ?: return

        getLinksForWord(word, lang)
    }

    fun getLinksForWord(word: String, lang: String) {
        viewModelScope.launch {
            val links = withContext(ioDispatcher) { getExternalLinksInteractor(lang) }
            _linksForWord.value = WordAndLinks(word, links)
        }
    }

    fun getLinksForWord(word: String) {
        val lang = cacheWebLink?.language ?: return
        getLinksForWord(word, lang)
    }

    fun setLanguage(langShort: String?) {
        cacheWebLink?.language = langShort
    }

    fun getLanguage() = cacheWebLink?.language

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
            val word = Words(sentence.original, sentence.sourceLang, sentence.translation)
            _textInfo.value = LoadResult.Success(WordResult(word, isSaved = false, isSentence = true))
            return
        }

        _textInfo.value = LoadResult.Loading

        viewModelScope.launch {
            wrapEspressoIdlingResource {
                getTranslation(sentence.original) { translation ->
                    sentence.translation = translation.translatedText
                    sentence.sourceLang = translation.src
                    val word = Words(sentence.original, translation.src, translation.translatedText)
                    _textInfo.value = LoadResult.Success(WordResult(word, isSaved = false, isSentence = true))
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
            cacheWebLink?.let {
                it.lastRead = Calendar.getInstance()
                webLinkDAO.upsert(it)
                cacheWebLink = webLinkDAO.getLink(link.url)
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

    fun saveNote(text:String, noteText: String, selection: Span, id: Long, color: String) {
        val webLink = cacheWebLink ?: return
        viewModelScope.launch {
            wrapEspressoIdlingResource {
                val newNote = Note(noteText, text, selection.start, selection.end - selection.start, color, webLink.id, null, id)
                val resultId = noteDAO.upsert(newNote)
                // Upsert returns -1 when the operation was an update, use the parameter ID.
                val finalId = if(resultId == -1L) id else resultId
                val result = ModifiedNote.Update(newNote.copy(id = finalId))
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

    fun loadLocalWords(texts: List<String>, index: Int) {
        viewModelScope.launch {
            val sections = withContext(defaultDispatcher) { splitByWords(texts) }
            val words = sections.flatMap { it.words }
            wordRepository.findWords(words, object : GetWordsCallback {
                override fun onWordsLoaded(words: List<Words>) {
                    viewModelScope.launch {
                        val paragraphWords = withContext(defaultDispatcher) { findWordsInSections(words, sections, index) }
                        _pageSavedWords.emit(SavedWordsSection(paragraphWords, index))
                    }
                }

                override fun onDataNotAvailable(exception: Exception) {
                    Timber.e(exception, "Error loading db words for ${index..(index + texts.lastIndex)}")
                }
            })
        }
    }

    private fun findWordsInSections(dbWords: List<Words>, sections: List<WordSpans>, position: Int): List<List<WordState>> {
        val paragraphWords = arrayListOf<List<WordState>>()
        sections.forEachIndexed { index, wordSpans ->
            val words = findWordsInSection(dbWords, wordSpans, position + index)
            paragraphWords.add(words)
        }
        return paragraphWords
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

    fun upsert(word: Words) {
        viewModelScope.launch {
            val resultId = withContext(ioDispatcher) { wordRepository.upsert(word) }
            if(resultId != -1L) {
                word.id = resultId.toInt()
                _updatedWord.value = ResultType.Insert(word)
            }
        }
    }

    fun deleteWord(word: Words) {
        viewModelScope.launch {
            withContext(ioDispatcher) { wordRepository.deleteWord(word) }
            _updatedWord.value = ResultType.Delete(word.id)
        }
    }

    data class WordResult(val word: Words, val isSaved: Boolean, val isSentence: Boolean = false)

    data class CachedParagraph(val translation: SimpleTranslation, val sentences: List<SimpleTranslation>)

    data class SimpleTranslation(val original: String, var translation: String = "", var sourceLang: String = "")

    data class Page(val title: String, val content: String)

    data class WordSpans(val words: List<String>, val spans: List<Span>)

    companion object {
        private const val PAGE_FILENAME = "content.xml"
    }
}

/**
 * Return class for the UI, used to display the paragraph with the notes
 */
data class PageInfo(val text: String, val notes: List<Note>, val isLocalPage: Boolean)

data class SavedWordsSection(val words: List<List<WordState>>, val start: Int)
