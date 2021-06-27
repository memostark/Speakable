package com.guillermonegrete.tts.webreader

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class WebReaderViewModel: ViewModel() {

    private val _page = MutableLiveData<String>()
    val page: LiveData<String>
        get() = _page

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
}
