package com.guillermonegrete.tts.data.source.local

import android.app.Application
import com.guillermonegrete.tts.data.source.ExternalLinksDataSource
import com.guillermonegrete.tts.db.ExternalLink
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okio.buffer
import okio.source
import java.lang.Exception

class AssetsExternalLinksSource(
    private val appContext: Application,
    moshi: Moshi
): ExternalLinksDataSource {

    private val adapter = moshi.adapter<List<ExternalLink>>(Types.newParameterizedType(List::class.java, ExternalLink::class.java))

    override fun getLanguageLinks(language: String, callback: ExternalLinksDataSource.Callback) {

        try {
            val inputStream = appContext.assets.open(EXTERNAL_LINKS_DATA_FILENAME)
            val linkList: List<ExternalLink>? = adapter.fromJson(inputStream.source().buffer())
            if (linkList == null) {
                callback.onLinksRetrieved(emptyList())
                return
            }
            callback.onLinksRetrieved(linkList.filter { it.language == language })
        } catch (ex: Exception){
            println("Error loading asset ${ex.message}")
            callback.onLinksRetrieved(emptyList())
        }
    }

    override fun getLanguageLinks(language: String): List<ExternalLink> {

        return try {
            val inputStream = appContext.assets.open(EXTERNAL_LINKS_DATA_FILENAME)
            val linkList: List<ExternalLink>? = adapter.fromJson(inputStream.source().buffer())
            if (linkList == null) return emptyList()
            linkList.filter { it.language == language }
        } catch (ex: Exception){
            println("Error loading asset ${ex.message}")
            emptyList()
        }
    }

    companion object{
        const val EXTERNAL_LINKS_DATA_FILENAME = "external_links.json"
    }


}