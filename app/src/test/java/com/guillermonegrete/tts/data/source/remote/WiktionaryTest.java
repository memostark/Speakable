package com.guillermonegrete.tts.data.source.remote;

import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem;
import com.guillermonegrete.tts.textprocessing.domain.model.WiktionaryItem;
import com.guillermonegrete.tts.textprocessing.domain.model.WiktionaryLangHeader;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class WiktionaryTest {


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

    @Test
    public void process_sub_header_with_five_equals(){
        String wikiExtract = "\n== German ==\n\n\n=== Pronunciation ===\nDescription 1\n\n\n==== Adverb ====\nDescription 2\n\n\n===== Synonyms =====\nDescription 3\n\n\n";
        List<WikiItem> resultItems = WiktionarySource.WiktionaryParser.parse(wikiExtract);

        List<WikiItem> expectedItems = Arrays.asList(
                new WiktionaryLangHeader("German"),
                new WiktionaryItem("Description 1\n\n\n Adverb \nDescription 2\n\n\n Synonyms \nDescription 3\n\n\n", "Pronunciation")
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
