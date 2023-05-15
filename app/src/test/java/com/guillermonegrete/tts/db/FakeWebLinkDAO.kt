package com.guillermonegrete.tts.db

import com.guillermonegrete.tts.webreader.db.LinkWithNotes
import kotlinx.coroutines.flow.Flow

class FakeWebLinkDAO: WebLinkDAO {

    private val links = mutableListOf<WebLink>()

    override suspend fun update(link: WebLink) {
        links.removeIf { it.id == link.id }
        links.add(link)
    }

    override suspend fun upsert(link: WebLink): Long {
        if (link in links) {
            update(link)
        } else {
            links.add(link)
        }
        return link.id.toLong()
    }

    override suspend fun delete(link: WebLink) {
        TODO("Not yet implemented")
    }

    override fun getRecentLinks(): Flow<List<WebLink>> {
        TODO("Not yet implemented")
    }

    override suspend fun getLink(url: String) = links.firstOrNull { it.url == url }
    override suspend fun getLinkWithNotes(url: String): LinkWithNotes? {
        val savedLink = links.find { it.url == url } ?: return null
        return LinkWithNotes(savedLink, emptyList())
    }
}
