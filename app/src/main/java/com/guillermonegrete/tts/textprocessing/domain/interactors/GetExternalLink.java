package com.guillermonegrete.tts.textprocessing.domain.interactors;

import com.guillermonegrete.tts.AbstractInteractor;
import com.guillermonegrete.tts.Executor;
import com.guillermonegrete.tts.MainThread;
import com.guillermonegrete.tts.data.source.ExternalLinksDataSource;
import com.guillermonegrete.tts.db.ExternalLink;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GetExternalLink extends AbstractInteractor implements GetExternalLinksInteractor {

    private Callback callback;

    private ExternalLinksDataSource dataSource;

    private String language;

    public GetExternalLink(Executor executor, MainThread mainThread, Callback callback, ExternalLinksDataSource dataSource, String language) {
        super(executor, mainThread);
        this.callback = callback;
        this.dataSource = dataSource;
        this.language = language;
    }

    @Override
    public void run() {
        dataSource.getLanguageLinks(language, new ExternalLinksDataSource.Callback() {
            @Override
            public void onLinksRetrieved(@NotNull final List<? extends ExternalLink> links) {
                mMainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onExternalLinksRetrieved((List<ExternalLink>) links);
                    }
                });
            }
        });
    }
}
