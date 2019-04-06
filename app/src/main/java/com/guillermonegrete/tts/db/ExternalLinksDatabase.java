package com.guillermonegrete.tts.db;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {ExternalLink.class}, version = 1)
public abstract class ExternalLinksDatabase extends RoomDatabase {

    private static ExternalLinksDatabase INSTANCE;
    private static final String DB_NAME = "external_links.db";

    public static ExternalLinksDatabase getDatabase(final Context context){
        if(INSTANCE == null){
            synchronized (ExternalLinksDatabase.class){
                if(INSTANCE == null){
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            ExternalLinksDatabase.class, DB_NAME)
                            .allowMainThreadQueries()
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    Log.d("WordsDatabase", "populating with data...");
                                    new ExternalLinksDatabase.PopulateDbAsync(INSTANCE).execute();
                                }
                            })
                            .build();
                }
            }
        }

        return INSTANCE;
    }

    public abstract ExternalLinksDAO externalLinksDAO();

    private static class PopulateDbAsync extends AsyncTask<Void, Void, Void>{

        private final ExternalLinksDAO externalLinksDAO;

        public PopulateDbAsync(ExternalLinksDatabase instance){this.externalLinksDAO = instance.externalLinksDAO();}

        @Override
        protected Void doInBackground(Void... voids) {
            externalLinksDAO.deleteAll();

            ExternalLink link1 = new ExternalLink("Reverso Conjugator","http://conjugator.reverso.net/conjugation-hebrew-verb-{q}.html","he");
            ExternalLink link2 = new ExternalLink("Pealim","https://www.pealim.com/search/?q={q}","he");
            ExternalLink link3 = new ExternalLink("Wiktionary","https://en.wiktionary.org/wiki/{q}","he");

            ExternalLink link4 = new ExternalLink("Cambridge Dictionary","https://dictionary.cambridge.org/dictionary/english/{q}","en");

            externalLinksDAO.insert(link1, link2, link3, link4);
            return null;
        }
    }

}
