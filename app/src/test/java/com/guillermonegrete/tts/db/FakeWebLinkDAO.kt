package com.guillermonegrete.tts.db

import kotlinx.coroutines.flow.Flow

class FakeWebLinkDAO: WebLinkDAO {
    override fun insert(link: WebLink): Long {
        TODO("Not yet implemented")
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

    override suspend fun getLink(url: String): WebLink? {
        TODO("Not yet implemented")
    }
}
