package com.guillermonegrete.tts.textprocessing.domain.interactors;

import com.guillermonegrete.tts.Executor;
import com.guillermonegrete.tts.MainThread;
import com.guillermonegrete.tts.data.source.DictionaryRepository;
import com.guillermonegrete.tts.data.source.WordRepository;
import com.guillermonegrete.tts.threading.TestMainThread;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GetLayoutTest {
    private MainThread mainThread;
    @Mock private Executor executor;
    @Mock private GetLayout.Callback callback;
    @Mock private WordRepository repository;
    @Mock private DictionaryRepository dictionaryRepository;

    @Before
    public void setUp() throws Exception{
        MockitoAnnotations.initMocks(this);
        mainThread = new TestMainThread();
    }

    @Test
    public void setLayoutForWordInDatabase(){

        String word = "Test";
        System.out.print("Running my tests...");
        GetLayout interactor = new GetLayout(executor, mainThread, callback, repository, dictionaryRepository, word, "", "");
        interactor.run();

        //Assert that onLocalWordLoaded is called.
    }

    @Test
    public void setLayoutForSentence(){

    }

    @Test
    public void setLayoutForExternalDictionary(){

    }

    @Test
    public void setLayoutForWordInDBandExternalDict(){

    }


}
