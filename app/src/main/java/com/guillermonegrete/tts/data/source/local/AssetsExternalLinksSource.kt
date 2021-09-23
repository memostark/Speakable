package com.guillermonegrete.tts.data.source.local

import android.app.Application
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.guillermonegrete.tts.data.source.ExternalLinksDataSource
import com.guillermonegrete.tts.db.ExternalLink
import java.lang.Exception

class AssetsExternalLinksSource constructor(private val appContext: Application): ExternalLinksDataSource {

    override fun getLanguageLinks(language: String, callback: ExternalLinksDataSource.Callback) {
        val linkType = object : TypeToken<List<ExternalLink>>() {}.type
        var jsonReader: JsonReader? = null

        try {
            val inputStream = appContext.assets.open(EXTERNAL_LINKS_DATA_FILENAME)
            jsonReader = JsonReader(inputStream.reader())
            val linkList: List<ExternalLink> = Gson().fromJson(jsonReader, linkType)
            callback.onLinksRetrieved(linkList.filter { it.language == language })
        } catch (ex: Exception){
            println("Error loading asset $ex")
            callback.onLinksRetrieved(emptyList())
        } finally {
            jsonReader?.close()
        }
    }

    override fun getLanguageLinks(language: String): List<ExternalLink> {
        val linkType = object : TypeToken<List<ExternalLink>>() {}.type
        var jsonReader: JsonReader? = null

        return try {
            val inputStream = appContext.assets.open(EXTERNAL_LINKS_DATA_FILENAME)
            jsonReader = JsonReader(inputStream.reader())
            val linkList: List<ExternalLink> = Gson().fromJson(jsonReader, linkType)
            linkList.filter { it.language == language }
        } catch (ex: Exception){
            println("Error loading asset $ex")
            emptyList()
        } finally {
            jsonReader?.close()
        }
    }

    companion object{
        const val EXTERNAL_LINKS_DATA_FILENAME = "external_links.json"
    }


}