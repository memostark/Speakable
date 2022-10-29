package com.guillermonegrete.tts.db

import kotlinx.coroutines.flow.Flow

class FakeWebLinkDAO: WebLinkDAO {

    private val links = mutableListOf<WebLink>()

    override fun insert(link: WebLink): Long {
        links.add(link)
        return link.id.toLong()
    }

    override fun update(link: WebLink) {
        TODO("Not yet implemented")
    }

    override suspend fun delete(link: WebLink) {
        TODO("Not yet implemented")
    }

    override fun getRecentLinks(): Flow<List<WebLink>> {
        TODO("Not yet implemented")
    }

    override suspend fun getLink(url: String) = links.firstOrNull { it.url == url }
}
