package com.guillermonegrete.tts.webreader

import androidx.lifecycle.*
import com.guillermonegrete.tts.common.models.Span
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.data.Result
import com.guillermonegrete.tts.data.Translation
import com.guillermonegrete.tts.data.source.WordRepositorySource
import com.guillermonegrete.tts.db.WebLink
import com.guillermonegrete.tts.db.WebLinkDAO
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.importtext.visualize.model.SplitPageSpan
import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetExternalLink
import com.guillermonegrete.tts.utils.wrapEspressoIdlingResource
import com.guillermonegrete.tts.webreader.db.Note
import com.guillermonegrete.tts.webreader.db.NoteDAO
import com.guillermonegrete.tts.webreader.model.ModifiedNote
import com.guillermonegrete.tts.webreader.model.SplitParagraph
import com.guillermonegrete.tts.webreader.model.WordAndLinks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import org.jsoup.Jsoup
import java.io.File
import java.io.IOException
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

    private val _clickedWord = MutableLiveData<WordAndLinks>()
    val clickedWord: LiveData<WordAndLinks> = _clickedWord

    private val _updatedNote = MutableLiveData<ModifiedNote>()
    val updatedNote: LiveData<ModifiedNote> = _updatedNote

    private var cacheWebLink: WebLink? = null

    private val _weblink = MutableLiveData<WebLink>()
    val webLink: LiveData<WebLink>
        get() = _weblink

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
        val file = File(rootFolder, page_filename)
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

    private var job: Job? = null

    fun translateText(text: String){
        translateText(text, _textInfo)
    }

    fun translateWordInSentence(text: String){
        translateText(text, _wordInfo)
    }

    private fun translateText(text: String, observer: MutableLiveData<LoadResult<WordResult>>) {
        observer.value = LoadResult.Loading

        job?.cancel() // cancel the previous job otherwise you'll receive its updates
        job = viewModelScope.launch {

            wordRepository.getLocalWord(text, cacheWebLink?.language ?: "en")
                .asFlow().collectLatest {

                    if(it == null) {
                        getTranslation(text) { translation ->
                            val word = Words(text, translation.src, translation.translatedText)
                            observer.value = LoadResult.Success(WordResult(word, false))
                        }
                    }  else {
                        observer.value = LoadResult.Success(WordResult(it, true))
                    }
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
            _clickedWord.value = WordAndLinks(word, links)
        }
    }

    fun setLanguage(langShort: String?) {
        cacheWebLink?.language = langShort
    }

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

    fun translateSelected(paragraphIndex: Int, sentenceIndex: Int) {
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


        val success = folder.mkdirs()
        if (!success) return

        val contentFile = File(folder, page_filename)

        contentFile.bufferedWriter().use { it.write(content) }

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
        val folder = File(rootPath, uuid.toString())

        val files = folder.listFiles()
        if (files != null) {
            for (child: File in files) {
                child.delete()
            }
        }

        folder.delete()
        link.uuid = null

        viewModelScope.launch {
            webLinkDAO.update(link)
            noteDAO.deleteByFileId(link.id)
            loadPageFromWeb()
        }
    }

    fun saveNote(text: String, selection: Span, id: Long, color: String) {
        val webLink = cacheWebLink ?: return
        viewModelScope.launch {
            val newNote = Note(text, selection.start, selection.end - selection.start, color, webLink.id, id)
            val resultId = noteDAO.upsert(newNote)
            // Upsert returns -1 when the operation was an update, use the parameter ID.
            val finalId = if(resultId == -1L) id else resultId
            val result = ModifiedNote.Update(Note(text, newNote.position, newNote.length, color, webLink.id, finalId))
            _updatedNote.value = result
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            noteDAO.delete(Note("", 0, 0, "", 0, id)) // only the id is necessary
            _updatedNote.value = ModifiedNote.Delete(id)
        }
    }

    data class WordResult(val word: Words, val isSaved: Boolean, val isSentence: Boolean = false)

    data class CachedParagraph(val translation: SimpleTranslation, val sentences: List<SimpleTranslation>)

    data class SimpleTranslation(val original: String, var translation: String = "", var sourceLang: String = "")

    data class Page(val title: String, val content: String)

    companion object {
        private const val page_filename = "content.xml"
    }
}

/**
 * Return class for the UI, used to display the paragraph with the notes
 */
data class PageInfo(val text: String, val notes: List<Note>, val isLocalPage: Boolean)
