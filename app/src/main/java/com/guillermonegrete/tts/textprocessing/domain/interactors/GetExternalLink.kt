package com.guillermonegrete.tts.textprocessing.domain.interactors

import com.guillermonegrete.tts.AbstractInteractor
import com.guillermonegrete.tts.Executor
import com.guillermonegrete.tts.MainThread
import com.guillermonegrete.tts.data.source.ExternalLinksDataSource
import com.guillermonegrete.tts.db.ExternalLink

class GetExternalLink(
    executor: Executor,
    mainThread: MainThread,
    private val callback: GetExternalLinksInteractor.Callback,
    private val dataSource: ExternalLinksDataSource,
    private val language: String
) : AbstractInteractor(executor, mainThread), GetExternalLinksInteractor {

    override fun run() {
        dataSource.getLanguageLinks(language, object : ExternalLinksDataSource.Callback {
            override fun onLinksRetrieved(links: List<ExternalLink>) {
                mMainThread.post { callback.onExternalLinksRetrieved(links) }
            }
        })
    }
}
