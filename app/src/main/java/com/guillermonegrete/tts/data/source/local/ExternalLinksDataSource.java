package com.guillermonegrete.tts.data.source.local;

import com.guillermonegrete.tts.db.ExternalLink;
import com.guillermonegrete.tts.db.ExternalLinksDAO;

import java.util.List;

public class ExternalLinksDataSource {

    private static volatile ExternalLinksDataSource INSTANCE;

    private ExternalLinksDAO externalLinksDAO;

    private ExternalLinksDataSource(ExternalLinksDAO externalLinksDAO){this.externalLinksDAO = externalLinksDAO;}

    public static ExternalLinksDataSource getInstance(ExternalLinksDAO externalLinksDAO){
        if(INSTANCE == null){
            synchronized (ExternalLinksDataSource.class){
                if(INSTANCE == null)
                    INSTANCE = new ExternalLinksDataSource(externalLinksDAO);
            }
        }
        return INSTANCE;
    }

    public void getLanguageLinks(String language, Callback callback){
        callback.onLinksRetrieved(externalLinksDAO.findLinksByLanguage(language));

    }

    public interface Callback{
        void onLinksRetrieved(List<ExternalLink> links);
    }
}
