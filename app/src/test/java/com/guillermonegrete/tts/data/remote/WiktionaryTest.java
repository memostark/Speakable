package com.guillermonegrete.tts.data.remote;

import com.guillermonegrete.tts.TextProcessing.domain.model.WikiItem;
import com.guillermonegrete.tts.TextProcessing.domain.model.WiktionaryItem;
import com.guillermonegrete.tts.TextProcessing.domain.model.WiktionaryLangHeader;
import com.guillermonegrete.tts.data.source.DictionaryDataSource;
import com.guillermonegrete.tts.data.source.remote.WiktionarySource;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class WiktionaryTest {

    private WiktionarySource dataSource;
    private final CountDownLatch latch = new CountDownLatch(1);

    @Before
    public void setDataSource(){
        dataSource = WiktionarySource.getInstance();
    }

    @Test
    public void TranslatorTest(){
        dataSource.getDefinition("prueba", new DictionaryDataSource.GetDefinitionCallback() {
            @Override
            public void onDefinitionLoaded(List<WikiItem> definitions) {
                for (WikiItem def : definitions){
                    if(def instanceof WiktionaryItem){
                        WiktionaryItem item = (WiktionaryItem) def;
                        System.out.println("---------Item type:");
                        System.out.println(item.getSubHeaderText());
                        System.out.println(item.getItemText());
                    }else{
                        WiktionaryLangHeader item = (WiktionaryLangHeader) def;
                        System.out.println("---------Header type:");
                        System.out.println(item.getLanguage());
                    }
                }
                latch.countDown();
            }

            @Override
            public void onDataNotAvailable() {
                latch.countDown();
            }
        } );

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
