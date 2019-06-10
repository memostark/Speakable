package com.guillermonegrete.tts.textprocessing.domain.interactors;

import com.guillermonegrete.tts.db.ExternalLink;

import java.util.List;

public interface GetExternalLinksInteractor {
    interface Callback{
        void onExternalLinksRetrieved(List<ExternalLink> links);
    }
}
