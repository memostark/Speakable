package com.guillermonegrete.tts.db;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

@Database(entities = {Words.class}, version = 2)
public abstract class WordsDatabase extends RoomDatabase {
    private static WordsDatabase INSTANCE;
    private static final String DB_NAME = "words.db";

    public static WordsDatabase getDatabase(final Context context){
        if(INSTANCE == null){
            synchronized (WordsDatabase.class){
                if(INSTANCE == null){
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            WordsDatabase.class, DB_NAME)
                            .allowMainThreadQueries()
                            .addMigrations(MIGRATION_1_2)
                            .addCallback(new RoomDatabase.Callback(){
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    Log.d("WordsDatabase", "populating with data...");
                                    new PopulateDbAsync(INSTANCE).execute();
                                }
                            }).build();
                }
            }

        }
        return INSTANCE;
    }

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE words "
                    + " ADD COLUMN notes TEXT");
        }
    };

    public void clearDb(){
        if (INSTANCE != null){
            new PopulateDbAsync(INSTANCE).execute();
        }
    }

    public abstract WordsDAO wordsDAO();

    private static class PopulateDbAsync extends AsyncTask<Void, Void, Void> {
        private final WordsDAO wordsDAO;

        public PopulateDbAsync(WordsDatabase instance){
            wordsDAO = instance.wordsDAO();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            wordsDAO.deleteAll();

            Words first_word = new Words("Hallo", "DE", "Hello");
            Words second_word = new Words("Hallo", "DE", "Hello");

            wordsDAO.insert(first_word, second_word);

            return null;
        }
    }
}
