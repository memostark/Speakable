package com.guillermonegrete.tts.TextProcessing.domain.interactors;

// Must delete this imports later
import android.os.Handler;
import android.os.Looper;

import com.guillermonegrete.tts.AbstractInteractor;
import com.guillermonegrete.tts.Executor;
import com.guillermonegrete.tts.MainThread;
import com.guillermonegrete.tts.TextProcessing.ProcessTextLayoutType;
import com.guillermonegrete.tts.TextProcessing.domain.model.WikiItem;
import com.guillermonegrete.tts.data.source.DictionaryDataSource;
import com.guillermonegrete.tts.data.source.DictionaryRepository;
import com.guillermonegrete.tts.data.source.WordRepository;
import com.guillermonegrete.tts.data.source.WordRepositorySource;
import com.guillermonegrete.tts.db.Words;

import java.util.List;


public class GetLayout extends AbstractInteractor implements GetLayoutInteractor {

    private GetLayoutInteractor.Callback mCallback;
    private WordRepository mRepository;
    private DictionaryRepository dictionaryRepository;
    private String mText;

    private boolean insideDictionary;
    private boolean dictionaryRequestDone;
    private boolean translationDone;

    private Words mWord;
    private List<WikiItem> items;

    public GetLayout(Executor threadExecutor, MainThread mainThread, Callback callback, WordRepository repository, DictionaryRepository dictRepository, String text){
        super(threadExecutor, mainThread);
        mCallback = callback;
        mText = text;
        mRepository = repository;
        dictionaryRepository = dictRepository;

        insideDictionary = false;
        dictionaryRequestDone = false;
        translationDone = false;
    }

    @Override
    public void run() {
        System.out.println("Running get layout interactor");
        String[] splittedText = mText.split(" ");
        /*Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                System.out.println("Get layout callback to main thread");
                mCallback.onDictionaryLayoutDetermined(new ArrayList<WikiItem>());
            }
        });*/

        if(splittedText.length > 1){
            System.out.print("Get sentence layout");
            // Get translation, wait for callback
            mRepository.getLanguageAndTranslation(mText, new WordRepositorySource.GetTranslationCallback() {
                @Override
                public void onTranslationAndLanguage(Words word) {
                    mCallback.onLayoutDetermined(word, ProcessTextLayoutType.SENTENCE_TRANSLATION);
                }

                @Override
                public void onDataNotAvailable() {

                }
            });
        }else{
            // Search in database
            System.out.println("Request layouts");
            mRepository.getWordLanguageInfo(mText, new WordRepositorySource.GetWordRepositoryCallback() {
                @Override
                public void onLocalWordLoaded(Words word) {
                    mCallback.onLayoutDetermined(word, ProcessTextLayoutType.SAVED_WORD);
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
                public void onDataNotAvailable() {

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
                System.out.println(String.format("In background thread, get layout interactor, items size: %s", String.valueOf(items.size())));
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onDictionaryLayoutDetermined(items);
                    }
                });
            }else{
                mCallback.onLayoutDetermined(mWord, ProcessTextLayoutType.WORD_TRANSLATION);
            }
        }
    }


}
