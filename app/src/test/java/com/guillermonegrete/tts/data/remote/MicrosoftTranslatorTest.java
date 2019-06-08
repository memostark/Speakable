package com.guillermonegrete.tts.data.remote;

import com.guillermonegrete.tts.BuildConfig;
import com.guillermonegrete.tts.data.source.WordDataSource;
import com.guillermonegrete.tts.data.source.remote.MSTranslatorSource;
import com.guillermonegrete.tts.db.Words;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class MicrosoftTranslatorTest {

    private MSTranslatorSource dataSource;
    private final CountDownLatch latch = new CountDownLatch(1);

    @Before
    public void setDataSource(){
        dataSource = MSTranslatorSource.getInstance(BuildConfig.TTSApiKey);
    }

    @Test
    public void TranslatorTest(){
        dataSource.getWordLanguageInfo("Prueba", "auto", "en", new WordDataSource.GetWordCallback() {
            @Override
            public void onWordLoaded(Words word) {
                latch.countDown();
            }

            @Override
            public void onDataNotAvailable() {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
