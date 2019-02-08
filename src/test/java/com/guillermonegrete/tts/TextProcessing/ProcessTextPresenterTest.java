package com.guillermonegrete.tts.TextProcessing;

import com.guillermonegrete.tts.CustomTTS.CustomTTS;
import com.guillermonegrete.tts.Executor;
import com.guillermonegrete.tts.MainThread;
import com.guillermonegrete.tts.TestThreadExecutor;
import com.guillermonegrete.tts.TextProcessing.domain.model.WikiItem;
import com.guillermonegrete.tts.ThreadExecutor;
import com.guillermonegrete.tts.data.source.DictionaryDataSource;
import com.guillermonegrete.tts.data.source.DictionaryRepository;
import com.guillermonegrete.tts.data.source.WordRepository;
import com.guillermonegrete.tts.data.source.WordRepositorySource;
import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.threading.TestMainThread;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class ProcessTextPresenterTest {

    @Mock private ProcessTextContract.View view;
    @Mock private WordRepository wordRepository;
    @Mock private DictionaryRepository dictionaryRepository;
    @Mock private CustomTTS customTTS;

    @Captor
    private ArgumentCaptor<WordRepositorySource.GetWordRepositoryCallback> getWordCallbackCaptor;

    @Captor
    private ArgumentCaptor<WordRepositorySource.GetTranslationCallback> getTranslationCallbackCaptor;

    @Captor
    private ArgumentCaptor<DictionaryDataSource.GetDefinitionCallback> getDefinitionCallbacCaptor;

    private ProcessTextPresenter presenter;

    @Before
    public void setupPresenter(){
        MockitoAnnotations.initMocks(this);
        presenter = givenPresenter();
    }

    private ProcessTextPresenter givenPresenter(){
        MainThread mainThread = new TestMainThread();
        Executor executor = new TestThreadExecutor();
        return new ProcessTextPresenter(executor, mainThread, view, wordRepository, dictionaryRepository, customTTS);
    }

    @Test
    public void createPresenter_setsThePresenterToView() {
        // Get a reference to the class under test
        presenter = givenPresenter();

        // Then the presenter is set to the view
        verify(view).setPresenter(presenter);
    }


    @Test
    public void setSavedWordLayout(){
        String test_text = "Prueba";
        presenter.getLayout(test_text);

        verify(wordRepository).getWordLanguageInfo(eq(test_text), getWordCallbackCaptor.capture());
        Words return_word = new Words(test_text,"ES", "Test");
        getWordCallbackCaptor.getValue().onLocalWordLoaded(return_word);

        verify(view).setSavedWordLayout(return_word);
    }

    @Test
    public void setSentenceLayout(){
        String test_text = "Prueba oracion";
        presenter.getLayout(test_text);

        verify(wordRepository).getLanguageAndTranslation(eq(test_text), getTranslationCallbackCaptor.capture());
        Words return_word = new Words(test_text,"ES", "Sentence test");
        getTranslationCallbackCaptor.getValue().onTranslationAndLanguage(return_word);

        verify(view).setSentenceLayout(return_word);
    }

    @Test
    public void setTranslationLayout(){
        String test_text = "Prueba";
        presenter.getLayout(test_text);

        verify(wordRepository).getWordLanguageInfo(eq(test_text), getWordCallbackCaptor.capture());
        getWordCallbackCaptor.getValue().onLocalWordNotAvailable();

        Words return_word = new Words(test_text,"ES", "Test");
        getWordCallbackCaptor.getValue().onRemoteWordLoaded(return_word);

        verify(dictionaryRepository).getDefinition(eq(test_text), getDefinitionCallbacCaptor.capture());
        getDefinitionCallbacCaptor.getValue().onDataNotAvailable();

        verify(view).setTranslationLayout(return_word);

    }

    @Test
    public void setExternalDictionaryLayout(){
        String test_text = "Prueba";
        System.out.println("setExternalDict:");
        presenter.getLayout(test_text);

        verify(wordRepository).getWordLanguageInfo(eq(test_text), getWordCallbackCaptor.capture());
        getWordCallbackCaptor.getValue().onLocalWordNotAvailable();

        Words return_word = new Words(test_text,"ES", "Sentence test");
        getWordCallbackCaptor.getValue().onRemoteWordLoaded(return_word);

        List<WikiItem> wiktionaryLanguages = new ArrayList<>();
        verify(dictionaryRepository).getDefinition(eq(test_text), getDefinitionCallbacCaptor.capture());
        getDefinitionCallbacCaptor.getValue().onDefinitionLoaded(wiktionaryLanguages);


        verify(view, never()).setTranslationLayout(return_word);
        verify(view).setWiktionaryLayout(wiktionaryLanguages);

    }

}
