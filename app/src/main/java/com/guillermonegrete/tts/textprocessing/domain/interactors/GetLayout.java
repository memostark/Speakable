package com.guillermonegrete.tts.textprocessing.domain.interactors;

import com.guillermonegrete.tts.AbstractInteractor;
import com.guillermonegrete.tts.Executor;
import com.guillermonegrete.tts.MainThread;
import com.guillermonegrete.tts.textprocessing.ProcessTextLayoutType;
import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem;
import com.guillermonegrete.tts.data.source.DictionaryDataSource;
import com.guillermonegrete.tts.data.source.DictionaryRepository;
import com.guillermonegrete.tts.data.source.WordRepository;
import com.guillermonegrete.tts.data.source.WordRepositorySource;
import com.guillermonegrete.tts.db.Words;

import java.util.List;


public class GetLayout extends AbstractInteractor implements GetLayoutInteractor {

    private GetLayoutInteractor.Callback mCallback;
    private WordRepository wordRepository;
    private DictionaryRepository dictionaryRepository;
    private String mText;
    private String languageFrom;
    private String preferenceLanguage;

    private boolean insideDictionary;
    private boolean dictionaryRequestDone;
    private boolean translationDone;

    private Words mWord;
    private List<WikiItem> items;


    public GetLayout(Executor threadExecutor, MainThread mainThread,
                     Callback callback, WordRepository repository,
                     DictionaryRepository dictRepository, String text,
                     String languageFrom,
                     String preferenceLanguage){
        super(threadExecutor, mainThread);
        mCallback = callback;
        mText = text;
        wordRepository = repository;
        dictionaryRepository = dictRepository;

        insideDictionary = false;
        dictionaryRequestDone = false;
        translationDone = false;
        this.languageFrom = languageFrom;
        this.preferenceLanguage = preferenceLanguage;
    }

    @Override
    public void run() {
        String[] splittedText = mText.split(" ");

        if(splittedText.length > 1){
            // Get translation, wait for callback
            System.out.println("Preference language: " + preferenceLanguage);
            wordRepository.getLanguageAndTranslation(mText, languageFrom, preferenceLanguage, new WordRepositorySource.GetTranslationCallback() {
                @Override
                public void onTranslationAndLanguage(Words word) {
                    mCallback.onLayoutDetermined(word, ProcessTextLayoutType.SENTENCE_TRANSLATION);
                }

                @Override
                public void onDataNotAvailable() {}
            });
        }else{
            // Search in database
            wordRepository.getWordLanguageInfo(mText, languageFrom , preferenceLanguage, new WordRepositorySource.GetWordRepositoryCallback() {
                @Override
                public void onLocalWordLoaded(final Words word) {
                    mMainThread.post(() -> mCallback.onLayoutDetermined(word, ProcessTextLayoutType.SAVED_WORD));
                }

                @Override
                public void onLocalWordNotAvailable() {
                    getDictionaryEntry(mText);
                }

                @Override
                public void onRemoteWordLoaded(Words word) {
                    translationDone = true;
                    mWord = word;
                    setRemoteLayout();
                }

                @Override
                public void onDataNotAvailable(Words emptyWord) {
                    translationDone = true;
                    mWord = emptyWord;
                    setRemoteLayout();
                }
            });
        }

    }

    private void getDictionaryEntry(String mText) {
        dictionaryRepository.getDefinition(mText, new DictionaryDataSource.GetDefinitionCallback() {
            @Override
            public void onDefinitionLoaded(List<WikiItem> definitions) {
                insideDictionary = true;
                dictionaryRequestDone = true;
                items = definitions;
                setRemoteLayout();
            }

            @Override
            public void onDataNotAvailable() {
                insideDictionary = false;
                dictionaryRequestDone = true;
                setRemoteLayout();

            }
        });
    }

    private void setRemoteLayout(){

        if(dictionaryRequestDone && translationDone){
            if(insideDictionary){
                mMainThread.post(() -> mCallback.onDictionaryLayoutDetermined(mWord, items));
            }else{
                mCallback.onLayoutDetermined(mWord, ProcessTextLayoutType.WORD_TRANSLATION);
            }

            if(mWord.lang.equals("un")){
                mCallback.onTranslationError("Error");
            }
        }
    }


}
