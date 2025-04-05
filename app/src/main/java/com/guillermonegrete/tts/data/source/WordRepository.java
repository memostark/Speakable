package com.guillermonegrete.tts.data.source;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.guillermonegrete.tts.data.Result;
import com.guillermonegrete.tts.data.Translation;
import com.guillermonegrete.tts.data.TranslationKey;
import com.guillermonegrete.tts.data.TranslationKt;
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

    private final WordDataSource mWordLocalDataSource;

    private final TranslationSource translationSource;

    private final ConcurrentMap<TranslationKey, Translation> translationCache;


    @Inject
    public WordRepository(@ApplicationModule.WordsLocalDataSource WordDataSource wordLocalDataSource,
                          @ApplicationModule.RemoteTranslationSource @NonNull TranslationSource translationSource){
        mWordLocalDataSource = checkNotNull(wordLocalDataSource);
        this.translationSource = translationSource;

        translationCache = new ConcurrentHashMap<>();
    }

    @Override
    public List<Words> getWords() {
        return mWordLocalDataSource.getWords();
    }

    @NonNull
    @Override
    public LiveData<List<Words>> getWordsStream() {
        return mWordLocalDataSource.getWordsStream();
    }

    @Override
    public LiveData<Words> getLocalWord(@NonNull String word, String language) {
        return mWordLocalDataSource.loadWord(word, language);
    }

    @Override
    public LiveData<Words> getLocalWord(@NonNull Integer id) {
        return mWordLocalDataSource.loadWord(id);
    }

    @Override
    public List<String> getLanguagesISO() {
        return mWordLocalDataSource.getLanguagesISO();
    }

    @Override
    public void findWords(@NonNull List<String> words, GetWordsCallback callback) {
        mWordLocalDataSource.findWords(words, new WordDataSource.GetWordsCallback() {
            @Override
            public void onWordsLoaded(@NonNull List<Words> words) { callback.onWordsLoaded(words); }

            @Override
            public void onDataNotAvailable(@NonNull Exception exception) { callback.onDataNotAvailable(exception); }
        });
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
        try{
            Translation translation = translationSource.getTranslation(text, languageFrom, languageTo);
            callback.onTranslationAndLanguage(new Words(text, translation.getSrc(), translation.getTranslatedText()));
        }catch (Exception e){
            callback.onDataNotAvailable();
        }
    }

    @Override
    public Result<Translation> getTranslation(@NonNull String text, @NonNull String languageFrom, @NonNull String languageTo) {
        var key = new TranslationKey(text, languageFrom, languageTo);
        var cacheTranslation = translationCache.get(key);
        if (cacheTranslation != null) {
            return new Result.Success<>(cacheTranslation);
        }

        try{
            Translation wordTranslation = translationSource.getTranslation(text, languageFrom, languageTo);
            translationCache.put(key, wordTranslation);
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

    @Override
    public long upsert(@NonNull Words word) {
        return mWordLocalDataSource.upsert(word);
    }

    private void getRemoteWord(String wordText, String languageFrom, String languageTo, final GetWordRepositoryCallback callback) {

        var key = new TranslationKey(wordText, languageFrom, languageTo);
        var cacheTranslation = translationCache.get(key);
        if (cacheTranslation != null) {
            var word = TranslationKt.toWord(cacheTranslation);
            callback.onRemoteWordLoaded(word);
            return;
        }

        try{
            var translation = translationSource.getTranslation(wordText, languageFrom, languageTo);
            callback.onRemoteWordLoaded(TranslationKt.toWord(translation));
            translationCache.put(key, translation);
        }catch (Exception e){
            callback.onDataNotAvailable(new Words(wordText, "un", "un"));
        }
    }
}
