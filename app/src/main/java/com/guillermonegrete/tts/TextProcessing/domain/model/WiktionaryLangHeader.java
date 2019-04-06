package com.guillermonegrete.tts.TextProcessing.domain.model;


public class WiktionaryLangHeader implements WikiItem {

    private String language;

    public WiktionaryLangHeader(String lang){
        language = lang;
    }


    public String getLanguage(){
        return language;
    }

    @Override
    public RowType getItemType() {
        return RowType.HEADER_ITEM;
    }
}
