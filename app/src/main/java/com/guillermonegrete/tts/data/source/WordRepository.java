package com.guillermonegrete.tts.data.source;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.guillermonegrete.tts.data.Result;
import com.guillermonegrete.tts.data.Translation;
import com.guillermonegrete.tts.data.source.local.WordLocalDataSource;
import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.di.ApplicationModule;


import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.android.gms.common.internal.Preconditions.checkNotNull;

@Singleton
public class WordRepository implements WordRepositorySource {

    private final WordDataSource remoteTranslatorSource;
    private final WordDataSource mWordLocalDataSource;

    private final TranslationSource translationSource;

    private final ConcurrentMap<String, Words> cachedWords;


    @Inject
    public WordRepository(@ApplicationModule.RemoteTranslationDataSource WordDataSource remoteTranslatorSource,
                          @ApplicationModule.WordsLocalDataSource WordDataSource wordLocalDataSource,
                          @NonNull TranslationSource translationSource){
        this.remoteTranslatorSource = checkNotNull(remoteTranslatorSource);
        mWordLocalDataSource = checkNotNull(wordLocalDataSource);
        this.translationSource = translationSource;

        cachedWords = new ConcurrentHashMap<>();
    }

    @Override
    public List<Words> getWords() {
        return mWordLocalDataSource.getWords();
    }

    @Override
    public LiveData<List<Words>> getWordsStream() {
        return mWordLocalDataSource.getWordsStream();
    }

    @Override
    public List<String> getLanguagesISO() {
        return mWordLocalDataSource.getLanguagesISO();
    }

    @Override
    public void getWordLanguageInfo(String wordText, GetWordRepositoryCallback callback) {
        getWordLanguageInfo(wordText, "auto", "en", callback);
    }

    @Override
    public void getWordLanguageInfo(final @NonNull String wordText, final @NonNull String languageFrom, final @NonNull String languageTo, final @NonNull GetWordRepositoryCallback callback) {
        mWordLocalDataSource.getWordLanguageInfo(wordText, languageFrom, languageTo, new WordDataSource.GetWordCallback(){

            @Override
            public void onWordLoaded(Words word) {
                callback.onLocalWordLoaded(word);
            }

            @Override
            public void onDataNotAvailable() {
                callback.onLocalWordNotAvailable();
                getRemoteWord(wordText, languageFrom, languageTo, callback);
            }
        });

    }

    @Override
    public void getLanguageAndTranslation(@NonNull String text, @NonNull GetTranslationCallback callback) {
        getLanguageAndTranslation(text, "auto", "en", callback);
    }

    @Override
    public void getLanguageAndTranslation(@NonNull String text, @NonNull String languageFrom, @NonNull String languageTo, final @NonNull GetTranslationCallback callback) {
        remoteTranslatorSource.getWordLanguageInfo(text, languageFrom , languageTo, new WordDataSource.GetWordCallback() {
            @Override
            public void onWordLoaded(Words word) {
                callback.onTranslationAndLanguage(word);
                cachedWords.put(text, word);
            }

            @Override
            public void onDataNotAvailable() {
                callback.onDataNotAvailable();
            }
        });

    }

    @Override
    public Result<Translation> getTranslation(@NonNull String text, @NonNull String languageFrom, @NonNull String languageTo) {
        try{
            Translation wordTranslation = translationSource.getTranslation(text, languageFrom, languageTo);
            return new Result.Success<>(wordTranslation);
        }catch (Exception e){
            return new Result.Error<>(e);
        }
    }

    @Override
    public void deleteWord(String word) {
        WordLocalDataSource wordSource = (WordLocalDataSource) mWordLocalDataSource;
        wordSource.deleteWord(word);
    }

    @Override
    public void deleteWord(Words word) {
        WordLocalDataSource wordSource = (WordLocalDataSource) mWordLocalDataSource;
        wordSource.deleteWord(word);
    }

    @Override
    public void delete(Words... words) {
        mWordLocalDataSource.deleteWords(words);
    }

    @Override
    public void insert(Words... words) {
        mWordLocalDataSource.insertWords(words);
    }

    private void getRemoteWord(String wordText, String languageFrom, String languageTo, final GetWordRepositoryCallback callback) {

        Words cacheWord = cachedWords.get(wordText);
        if(cachedWords.get(wordText) != null){
            callback.onRemoteWordLoaded(cacheWord);
            return;
        }

        remoteTranslatorSource.getWordLanguageInfo(wordText, languageFrom, languageTo, new WordDataSource.GetWordCallback() {

            @Override
            public void onWordLoaded(Words word) {
                callback.onRemoteWordLoaded(word);
                cachedWords.put(wordText, word);
            }

            @Override
            public void onDataNotAvailable() {
                callback.onDataNotAvailable(new Words(wordText, "un", "un"));
            }
        });
    }
}
