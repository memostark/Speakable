package com.guillermonegrete.tts.db

import com.guillermonegrete.tts.webreader.db.LinkWithNotes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeWebLinkDAO: WebLinkDAO {

    val links = mutableListOf<WebLink>()

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
        links.removeIf { it.id == link.id }
    }

    override fun getRecentLinks(): Flow<List<WebLink>> {
        return flowOf(links.sortedByDescending { it.lastRead })
    }

    override suspend fun getLink(url: String) = links.firstOrNull { it.url == url }

    override suspend fun getLinkWithNotes(url: String): LinkWithNotes? {
        val savedLink = links.find { it.url == url } ?: return null
        return LinkWithNotes(savedLink, emptyList())
    }

    fun addLinks(vararg links: WebLink) {
        this.links.addAll(links)
    }
}
