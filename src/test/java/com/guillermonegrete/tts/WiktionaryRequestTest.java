package com.guillermonegrete.tts;

import com.guillermonegrete.tts.TextProcessing.ProcessTextActivity;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Collections;
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
        List<String> result = ProcessTextActivity.getLanguages(input);
        int expectedSize = 13;
        assertEquals(expectedSize, result.size());
    }

    @Test
    public void parser_generates_one_of_each_type(){
        String input = "\n== English ==\n\n\n=== Etymology ===\nBorrowed from Spanish hola.";
        ProcessTextActivity.WiktionaryParser parser = new ProcessTextActivity.WiktionaryParser(input);
        List<ProcessTextActivity.WiktionaryParser.WiktionaryItem> result_items = parser.parse();

        assertEquals(1, getItemTypeOccurrences(result_items, 100));
        assertEquals(1, getItemTypeOccurrences(result_items, 101));
        assertEquals(1, getItemTypeOccurrences(result_items, 102));
    }

    @Test
    public void parses_two_languages(){
        String input = "\n== English ==\n\n\n=== Etymology ===\nBorrowed from Spanish hola.\n== Spanish ==\n\n\n=== Etymology ===\nBorrowed from Spanish hola.";
        ProcessTextActivity.WiktionaryParser parser = new ProcessTextActivity.WiktionaryParser(input);
        List<ProcessTextActivity.WiktionaryParser.WiktionaryItem> result_items = parser.parse();

        assertEquals(2, getItemTypeOccurrences(result_items, 100));
        assertEquals(2, getItemTypeOccurrences(result_items, 101));
        assertEquals(2, getItemTypeOccurrences(result_items, 102));
    }

    private int getItemTypeOccurrences(List<ProcessTextActivity.WiktionaryParser.WiktionaryItem> items, int type){
        return Collections.frequency(items, new ProcessTextActivity.WiktionaryParser.WiktionaryItem("test", type));
    }
}
