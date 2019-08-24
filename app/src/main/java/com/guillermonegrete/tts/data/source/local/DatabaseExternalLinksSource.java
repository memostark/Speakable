package com.guillermonegrete.tts.data.source.local;

import com.guillermonegrete.tts.data.source.ExternalLinksDataSource;
import com.guillermonegrete.tts.db.ExternalLinksDAO;
import org.jetbrains.annotations.NotNull;

public class DatabaseExternalLinksSource implements ExternalLinksDataSource {

    private ExternalLinksDAO externalLinksDAO;

    private DatabaseExternalLinksSource(ExternalLinksDAO externalLinksDAO){this.externalLinksDAO = externalLinksDAO;}

    @Override
    public void getLanguageLinks(@NotNull String language, @NotNull ExternalLinksDataSource.Callback callback) {
        callback.onLinksRetrieved(externalLinksDAO.findLinksByLanguage(language));
    }
}
