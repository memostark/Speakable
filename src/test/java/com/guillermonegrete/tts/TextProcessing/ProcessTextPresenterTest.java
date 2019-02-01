package com.guillermonegrete.tts.TextProcessing;

import com.guillermonegrete.tts.Executor;
import com.guillermonegrete.tts.MainThread;
import com.guillermonegrete.tts.TextProcessing.domain.interactors.GetLayout;
import com.guillermonegrete.tts.data.source.WordRepository;
import com.guillermonegrete.tts.threading.TestMainThread;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

public class ProcessTextPresenterTest {

    @Mock private ProcessTextContract.View view;

    @Mock private WordRepository repository;

    @Mock private Executor executor;

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

}
