package com.guillermonegrete.tts.webreader

import androidx.lifecycle.*
import com.guillermonegrete.tts.common.models.Span
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.data.Result
import com.guillermonegrete.tts.data.Translation
import com.guillermonegrete.tts.data.source.WordRepository
import com.guillermonegrete.tts.db.WebLink
import com.guillermonegrete.tts.db.WebLinkDAO
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.importtext.visualize.model.SplitPageSpan
import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetExternalLink
import com.guillermonegrete.tts.utils.wrapEspressoIdlingResource
import com.guillermonegrete.tts.webreader.model.SplitParagraph
import com.guillermonegrete.tts.webreader.model.WordAndLinks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException
import java.text.BreakIterator
import java.util.*
import javax.inject.Inject

@HiltViewModel
class WebReaderViewModel @Inject constructor(
    private val getTranslationInteractor: GetLangAndTranslation,
    private val getExternalLinksInteractor: GetExternalLink,
    private val wordRepository: WordRepository,
    private val webLinkDAO: WebLinkDAO,
): ViewModel() {

    private val _page = MutableLiveData<LoadResult<String>>()
    val page: LiveData<LoadResult<String>>
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

    private val _clickedWord = MutableLiveData<WordAndLinks>()
    val clickedWord: LiveData<WordAndLinks> = _clickedWord

    private var cacheWebLink: WebLink? = null

    private val _weblink = MutableLiveData<WebLink>()
    val webLink: LiveData<WebLink>
        get() = _weblink

    fun loadDoc(url: String){
        _page.value = LoadResult.Loading

        viewModelScope.launch {
            wrapEspressoIdlingResource {
                try {
                    val page = getPage(url)
                    _page.value = LoadResult.Success(page)
                } catch (ex: IOException){
                    _page.value = LoadResult.Error(ex)
                }
                cacheWebLink = webLinkDAO.getLink(url) ?: WebLink(url)
                _weblink.value = cacheWebLink
            }
        }
    }

    private suspend fun getPage(url: String): String = withContext(Dispatchers.IO){
        val doc = Jsoup.connect(url).get()
        doc.body().select("menu, header, footer, logo, nav, search, link, button, btn, ad, script, style, img").remove()
        // Removes empty tags (e.g. <div></div>) and keeps self closing tags e.g. <br/>
        for (element in doc.select("*")) {
            if (!element.hasText() && element.isBlock) element.remove()
        }
        doc.body().html()
    }

    fun saveWebLink(){
        viewModelScope.launch {
            withContext(Dispatchers.IO){
                cacheWebLink?.let {
                    it.lastRead = Calendar.getInstance()
                    webLinkDAO.upsert(it)
                }
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
            val result = withContext(Dispatchers.IO) {
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

    private var job: Job? = null

    fun translateText(text: String){
        _textInfo.value = LoadResult.Loading

        job?.cancel() // cancel the previous job otherwise you'll receive its updates
        job = viewModelScope.launch {

            wordRepository.getLocalWord(text, cacheWebLink?.language ?: "en")
                .asFlow().collectLatest {

                    if(it == null) {
                        getTranslation(text) { translation ->
                            val word = Words(text, translation.src, translation.translatedText)
                            _textInfo.value = LoadResult.Success(WordResult(word, false))
                        }
                    }  else {
                        _textInfo.value = LoadResult.Success(WordResult(it, true))
                    }
                }
        }
    }

    private suspend fun getTranslation(text: String, onResult: (Translation) -> Unit) {
        val result = withContext(Dispatchers.IO) {
            getTranslationInteractor(text, languageFrom = cacheWebLink?.language ?: "auto")
        }

        when(result){
            is Result.Success -> onResult(result.data)
            is Result.Error -> _translatedParagraph.value = LoadResult.Error(result.exception)
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

        viewModelScope.launch {
            val links = withContext(Dispatchers.IO) { getExternalLinksInteractor(lang) }
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
            getTranslation(sentence.original) { translation ->
                sentence.translation = translation.translatedText
                sentence.sourceLang = translation.src
                val word = Words(sentence.original, translation.src, translation.translatedText)
                _textInfo.value = LoadResult.Success(WordResult(word, isSaved = false, isSentence = true))
            }
        }
    }

    data class WordResult(val word: Words, val isSaved: Boolean, val isSentence: Boolean = false)

    data class CachedParagraph(val translation: SimpleTranslation, val sentences: List<SimpleTranslation>)

    data class SimpleTranslation(val original: String, var translation: String = "", var sourceLang: String = "")
}
