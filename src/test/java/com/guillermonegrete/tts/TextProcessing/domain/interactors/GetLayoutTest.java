package com.guillermonegrete.tts.TextProcessing.domain.interactors;

import com.guillermonegrete.tts.Executor;
import com.guillermonegrete.tts.MainThread;
import com.guillermonegrete.tts.threading.TestMainThread;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GetLayoutTest {
    private MainThread mainThread;
    @Mock private Executor executor;
    @Mock private GetLayout.Callback callback;

    @Before
    public void setUp() throws Exception{
        MockitoAnnotations.initMocks(this);
        mainThread = new TestMainThread();
    }

    @Test
    public void testLayoutDetermined(){
        GetLayout interactor = new GetLayout(executor, mainThread, callback);
        interactor.run();

    }


}
