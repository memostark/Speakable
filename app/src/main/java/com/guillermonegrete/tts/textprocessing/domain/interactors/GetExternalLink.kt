package com.guillermonegrete.tts.textprocessing.domain.interactors

import com.guillermonegrete.tts.AbstractInteractor
import com.guillermonegrete.tts.MainThread
import com.guillermonegrete.tts.data.source.ExternalLinksDataSource
import com.guillermonegrete.tts.db.ExternalLink
import java.util.concurrent.ExecutorService

class GetExternalLink(
    executor: ExecutorService,
    mainThread: MainThread,
    private val dataSource: ExternalLinksDataSource
) : AbstractInteractor(executor, mainThread), GetExternalLinksInteractor {

    private var language = ""
    private var callback: GetExternalLinksInteractor.Callback? = null

    override fun run() {
        dataSource.getLanguageLinks(language, object : ExternalLinksDataSource.Callback {
            override fun onLinksRetrieved(links: List<ExternalLink>) {
                mMainThread.post { callback?.onExternalLinksRetrieved(links) }
            }
        })
    }

    operator fun invoke(language: String, callback: GetExternalLinksInteractor.Callback){
        this.language = language
        this.callback = callback
        if(!executorService.isShutdown) executorService.execute { run() }
    }
}
