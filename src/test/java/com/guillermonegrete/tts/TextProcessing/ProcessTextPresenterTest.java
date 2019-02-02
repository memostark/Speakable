package com.guillermonegrete.tts.TextProcessing;

import com.guillermonegrete.tts.Executor;
import com.guillermonegrete.tts.MainThread;
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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

public class ProcessTextPresenterTest {

    @Mock private ProcessTextContract.View view;
    @Mock private WordRepository repository;
    @Mock private Executor executor;

    @Captor
    private ArgumentCaptor<WordRepositorySource.GetWordRepositoryCallback> getWordCallbackCaptor;

    @Captor
    private ArgumentCaptor<WordRepositorySource.GetTranslationCallback> getTranslationCallbackCaptor;

    private ProcessTextPresenter presenter;

    @Before
    public void setupPresenter(){
        MockitoAnnotations.initMocks(this);
        presenter = givenPresenter();
    }

    private ProcessTextPresenter givenPresenter(){
        MainThread mainThread = new TestMainThread();
        return new ProcessTextPresenter(executor, mainThread, view, repository);
    }

    @Test
    public void createPresenter_setsThePresenterToView() {
        // Get a reference to the class under test
        presenter = givenPresenter();

        // Then the presenter is set to the view
        verify(view).setPresenter(presenter);
    }

    @Test
    public void setTranslationLayout(){
        String test_text = "Prueba";
        presenter.getLayout(test_text);
//        verify(view).setWiktionaryLayout();
        verify(repository).getWordLanguageInfo(eq(test_text), getWordCallbackCaptor.capture());
        Words return_word = new Words(test_text,"ES", "Test");
        getWordCallbackCaptor.getValue().onRemoteWordLoaded(return_word);

        verify(view).setTranslationLayout(return_word);

    }

    @Test
    public void setSavedWordLayout(){
        String test_text = "Prueba";
        presenter.getLayout(test_text);

        verify(repository).getWordLanguageInfo(eq(test_text), getWordCallbackCaptor.capture());
        Words return_word = new Words(test_text,"ES", "Test");
        getWordCallbackCaptor.getValue().onLocalWordLoaded(return_word);

        verify(view).setSavedWordLayout(return_word);
    }

    @Test
    public void setSentenceLayout(){
        String test_text = "Prueba oracion";
        presenter.getLayout(test_text);

        verify(repository).getLanguageAndTranslation(eq(test_text), getTranslationCallbackCaptor.capture());
        Words return_word = new Words(test_text,"ES", "Sentence test");
        getTranslationCallbackCaptor.getValue().onTranslationAndLanguage(return_word);

        verify(view).setSentenceLayout(return_word);
    }

}
