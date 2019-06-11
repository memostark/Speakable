package com.guillermonegrete.tts.data.source.local;

import com.guillermonegrete.tts.data.source.ExternalLinksDataSource;
import com.guillermonegrete.tts.db.ExternalLink;
import com.guillermonegrete.tts.db.ExternalLinksDAO;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DatabaseExternalLinksSource implements ExternalLinksDataSource {

    private static volatile DatabaseExternalLinksSource INSTANCE;

    private ExternalLinksDAO externalLinksDAO;

    private DatabaseExternalLinksSource(ExternalLinksDAO externalLinksDAO){this.externalLinksDAO = externalLinksDAO;}

    public static DatabaseExternalLinksSource getInstance(ExternalLinksDAO externalLinksDAO){
        if(INSTANCE == null){
            synchronized (DatabaseExternalLinksSource.class){
                if(INSTANCE == null)
                    INSTANCE = new DatabaseExternalLinksSource(externalLinksDAO);
            }
        }
        return INSTANCE;
    }

    @Override
    public void getLanguageLinks(@NotNull String language, @NotNull ExternalLinksDataSource.Callback callback) {
        callback.onLinksRetrieved(externalLinksDAO.findLinksByLanguage(language));
    }
}
