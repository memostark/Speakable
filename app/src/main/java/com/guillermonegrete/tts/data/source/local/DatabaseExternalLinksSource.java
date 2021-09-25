package com.guillermonegrete.tts.data.source.local;

import androidx.annotation.NonNull;

import com.guillermonegrete.tts.data.source.ExternalLinksDataSource;
import com.guillermonegrete.tts.db.ExternalLink;
import com.guillermonegrete.tts.db.ExternalLinksDAO;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DatabaseExternalLinksSource implements ExternalLinksDataSource {

    private final ExternalLinksDAO externalLinksDAO;

    private DatabaseExternalLinksSource(ExternalLinksDAO externalLinksDAO){this.externalLinksDAO = externalLinksDAO;}

    @Override
    public void getLanguageLinks(@NotNull String language, @NotNull ExternalLinksDataSource.Callback callback) {
        callback.onLinksRetrieved(externalLinksDAO.findLinksByLanguage(language));
    }

    @NonNull
    @Override
    public List<ExternalLink> getLanguageLinks(@NonNull String language) {
        return externalLinksDAO.findLinksByLanguage(language);
    }
}
