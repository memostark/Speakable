package com.guillermonegrete.tts.webreader

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation
import com.guillermonegrete.tts.data.Result
import com.guillermonegrete.tts.data.Translation
import com.guillermonegrete.tts.importtext.visualize.model.Span
import com.guillermonegrete.tts.importtext.visualize.model.SplitPageSpan
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetExternalLink
import kotlinx.coroutines.*
import org.jsoup.Jsoup

class WebReaderViewModel @ViewModelInject constructor(
    private val getTranslationInteractor: GetLangAndTranslation,
    private val getExternalLinksInteractor: GetExternalLink
): ViewModel() {

    private val _page = MutableLiveData<String>()
    val page: LiveData<String>
        get() = _page

    private var cachedParagraphs: List<Words>? = null
    val paragraphs = cachedParagraphs

    private var _translatedParagraphs = mutableListOf<Translation?>()
    val translatedParagraphs: List<Translation?>
        get() = _translatedParagraphs

    private val _translatedParagraph = MutableLiveData<LoadResult<Int>>()
    val translatedParagraph: LiveData<LoadResult<Int>> = _translatedParagraph

    private val _clickedWord = MutableLiveData<String>()
    val clickedWord: LiveData<String> = _clickedWord

    fun loadDoc(url: String){
        viewModelScope.launch {
            val page = getPage(url)
            _page.value = page
        }
    }

    suspend fun getPage(url: String): String = withContext(Dispatchers.IO){
        val doc = Jsoup.connect(url).get()
        doc.body().select("menu, header, footer, logo, nav, search, link, button, btn, ad, script, style").remove()
        doc.body().html()
    }

    fun createParagraphs(text: String): List<Words> {
        val newParagraphs = cachedParagraphs ?: text.split("\n").map { Words(it, "", "") }
        if(cachedParagraphs == null) {
            cachedParagraphs = newParagraphs
            _translatedParagraphs = arrayOfNulls<Translation>(newParagraphs.size).toMutableList()
        }
        return newParagraphs
    }

    fun translateParagraph(pos: Int) {
        val paragraphs = cachedParagraphs ?: return
        val paragraph = paragraphs[pos]

        if(paragraph.definition.isNotBlank()) return

        _translatedParagraph.value = LoadResult.Loading

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                getTranslationInteractor(paragraph.word)
            }

            when(result){
                is Result.Success -> {
                    paragraph.definition = result.data.translatedText
                    _translatedParagraphs[pos] = result.data
                    _translatedParagraph.value = LoadResult.Success(pos)
                }
                is Result.Error -> _translatedParagraph.value = LoadResult.Error(result.exception)
            }
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

    fun onWordClicked(word: String, pos: Int) {
        val translation = translatedParagraphs[pos] ?: return

        viewModelScope.launch {
            val links = withContext(Dispatchers.IO) { getExternalLinksInteractor(translation.src) }
            _clickedWord.value = links.first().link.replace("{q}", word)
        }
    }
}
