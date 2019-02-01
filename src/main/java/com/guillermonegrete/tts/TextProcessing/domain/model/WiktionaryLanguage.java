package com.guillermonegrete.tts.TextProcessing.domain.model;

import java.util.List;

public class WiktionaryLanguage {
    private List<WiktionaryItem>  wiktionaryItems;
    private String language;

    public WiktionaryLanguage(List<WiktionaryItem> items, String lang){
        wiktionaryItems = items;
        language = lang;
    }

    public List<WiktionaryItem> getWiktionaryItems() {
        return wiktionaryItems;
    }

    public String getLanguage(){
        return language;
    }
}
