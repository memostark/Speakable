package com.guillermonegrete.tts.webreader

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guillermonegrete.tts.Event
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation
import com.guillermonegrete.tts.data.Result
import kotlinx.coroutines.*
import org.jsoup.Jsoup

class WebReaderViewModel @ViewModelInject constructor(private val getTranslationInteractor: GetLangAndTranslation): ViewModel() {

    private val _page = MutableLiveData<String>()
    val page: LiveData<String>
        get() = _page

    private var cachedParagraphs: List<Words>? = null
    val paragraphs = cachedParagraphs

    private val _paragraphIndex = MutableLiveData<Event<Int>>()
    val paragraphIndex: LiveData<Event<Int>> = _paragraphIndex

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
        if(cachedParagraphs == null) cachedParagraphs = newParagraphs
        return newParagraphs
    }

    fun translateParagraph(pos: Int) {
        val paragraphs = cachedParagraphs ?: return
        val paragraph = paragraphs[pos]

        if(paragraph.definition.isNotBlank()) return

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                getTranslationInteractor(paragraph.word)
            }
            if(result is Result.Success) {
                paragraph.definition = result.data.definition
                _paragraphIndex.value = Event(pos)
            }
        }
    }
}
