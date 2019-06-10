package com.guillermonegrete.tts.data.remote;

import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem;
import com.guillermonegrete.tts.textprocessing.domain.model.WiktionaryItem;
import com.guillermonegrete.tts.textprocessing.domain.model.WiktionaryLangHeader;
import com.guillermonegrete.tts.data.source.DictionaryDataSource;
import com.guillermonegrete.tts.data.source.remote.WiktionarySource;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

public class WiktionaryTest {

    private WiktionarySource dataSource;
    private final CountDownLatch latch = new CountDownLatch(1);

    @Before
    public void setDataSource(){
        dataSource = WiktionarySource.getInstance();
    }

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


    @Test
    public void process_sub_header_without_body_text(){
        // Extract from: https://en.wiktionary.org/w/api.php?action=query&prop=extracts&format=json&explaintext=&redirects=1&titles=castell
        String wikiExtract = "\n== Welsh ==\n\n\n=== Etymology ===\nBorrowed from Latin castellum.\n\n\n=== Pronunciation ===\n(North Wales) (standard) (colloquial) IPA(key): /ˈkasdɛɬ/, [ˈkʰastɛɬ]\n\n\n=== Noun ===\ncastell m (plural cestyll or castelli)\n\ncastle (fortified building)\nrook (chess piece)\n\n\n==== Synonyms ====\ncaer\n\n\n=== Mutation ===";

        List<WikiItem> resultItems = WiktionarySource.WiktionaryParser.parse(wikiExtract);

        List<WikiItem> expectedItems = Arrays.asList(
                new WiktionaryLangHeader("Welsh"),
                new WiktionaryItem("Borrowed from Latin castellum.\n\n", "Etymology"),
                new WiktionaryItem("(North Wales) (standard) (colloquial) IPA(key): /ˈkasdɛɬ/, [ˈkʰastɛɬ]\n\n", "Pronunciation"),
                new WiktionaryItem("castell m (plural cestyll or castelli)\n\ncastle (fortified building)\nrook (chess piece)\n\n\n Synonyms \ncaer\n\n", "Noun"),
                new WiktionaryItem("", "Mutation ")
        );

        assertWikiItemList(expectedItems, resultItems);
    }

    private void assertWikiItemList(List<WikiItem> expected, List<WikiItem> actual){
        assertEquals(expected.size(), actual.size());

        for(int i=0; i<expected.size(); i++){
            assertWikiItem(expected.get(i), actual.get(i));
        }
    }

    private void assertWikiItem(WikiItem expected, WikiItem actual){
        assertEquals(expected.getClass(), actual.getClass());

        if(expected instanceof WiktionaryLangHeader){
            WiktionaryLangHeader expectedHeader = (WiktionaryLangHeader) expected;
            WiktionaryLangHeader actualHeader = (WiktionaryLangHeader) actual;
            assertEquals(expectedHeader.getLanguage(), actualHeader.getLanguage());
        }else if(expected instanceof  WiktionaryItem){
            WiktionaryItem expectedHeader = (WiktionaryItem) expected;
            WiktionaryItem actualHeader = (WiktionaryItem) actual;
            assertEquals(expectedHeader.getItemText(), actualHeader.getItemText());
            assertEquals(expectedHeader.getSubHeaderText(), actualHeader.getSubHeaderText());
        }
    }
}
