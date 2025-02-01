package com.guillermonegrete.tts.data.source.remote;


import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem;
import com.guillermonegrete.tts.textprocessing.domain.model.WiktionaryItem;
import com.guillermonegrete.tts.textprocessing.domain.model.WiktionaryLangHeader;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.List;

public class WiktionaryRequestTest {

    @Test
    public void splitRequestByLanguage(){

        String input = "\n== English ==\n\n\n=== Etymology ===\nBorrowed from Spanish hola.\n\n\n=== Anagrams ===\nHALO, halo, halo-\n\n" +
                "\n== Asturian ==\n\n\n=== Pronunciation ===\nIPA(key): /\u02c8o.la/\n\n\n=== Interjection ===\nhola\n\nhello, hi\n\n" +
                "\n== Catalan ==\n\n\n=== Etymology ===\nFrom Spanish hola\n\n(Valencian) IPA(key): /\u02c8o.la/\n\n\n=== Interjection ===\nhola\n\nhello, hi\n\n" +
                "\n== Dutch ==\n\n\n=== Pronunciation ===\n\n\n=== Interjection ===\nhola\n\nhallo, hoi\n\n\n=== Anagrams ===\nhalo\n\n" +
                "\n== Esperanto ==\n\n\n=== Pronunciation ===\nIPA(key): /\u02c8ho.la/\n\n\n=== Interjection ===\nhola\n\nhey, oi.\n\n" +
                "\n== French ==\n\n\n=== Pronunciation ===\n(aspirated h) IPA(key): /\u0254\n\n\n=== Anagrams ===\nhalo\n\n" +
                "\n== Icelandic ==\n\n\n=== Pronunciation ===\nIPA(key): /\u02c8h\u0254\u02d0la/\nRhymes: -\u0254\u02d0la\n\n" +
                "\n== Ido ==\n\n\n=== Etymology ===\nFrom Spanish hola.\n\n\n=== Pronunciation ===\nIPA(key): /\u02c8ho.la/\n\n" +
                "\n== Irish ==\n\n\n=== Pronunciation ===\nIPA(key): [\u02c8h\u0254l\u032a\u02e0\u0259]\n\n\n=== Noun ===\nhola m\n\nh-prothesized form of ola\n\n" +
                "\n== Norwegian Bokm\u00e5l ==\n\n\n=== Alternative forms ===\nholen\n\n\n=== Noun ===\nhola m, f\n\ndefinite feminine singular of hole\n\n" +
                "\n== Norwegian Nynorsk ==\n\n\n=== Noun ===\nhola f\n\ndefinite singular of hole\n\n" +
                "\n== Spanish ==\n\n\n=== Etymology ===\n\nUnknown. Hola is etymologically unrelated to the Germanic expressions hello in English and hallo in German";
        List<String> result = WiktionarySource.WiktionaryParser.getLanguages(input);
        int expectedSize = 12;
        assertEquals(expectedSize, result.size());
    }

    @Test
    public void parser_generates_one_of_each_type(){
        String input = "\n== English ==\n\n\n=== Etymology ===\nBorrowed from Spanish hola.";
        List<WikiItem> result_items = WiktionarySource.WiktionaryParser.parse(input);

        assertEquals(1, getItemTypeOccurrences(result_items, WiktionaryItem.class));
        assertEquals(1, getItemTypeOccurrences(result_items, WiktionaryLangHeader.class));
    }

    @Test
    public void parses_two_languages(){
        String input = "\n== English ==\n\n\n=== Etymology ===\nBorrowed from Spanish hola.\n== Spanish ==\n\n\n=== Etymology ===\nBorrowed from Spanish hola.";
        List<WikiItem> result_items = WiktionarySource.WiktionaryParser.parse(input);

        assertEquals(2, getItemTypeOccurrences(result_items, WiktionaryItem.class));
        assertEquals(2, getItemTypeOccurrences(result_items, WiktionaryLangHeader.class));
    }

    private int getItemTypeOccurrences(List<WikiItem> items, Class type){

        int count = 0;
        for (WikiItem item : items){
            if(item.getClass() ==  type){
                count++;
            }
        }
        return count;
    }
}
