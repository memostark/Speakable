package com.guillermonegrete.tts.TextProcessing.domain.model;

public class WiktionaryItem{
    public String itemText;
    public int itemType;

    public WiktionaryItem(String itemText, int itemType){
        this.itemText = itemText;
        this.itemType = itemType;
    }

    /*
     *  Used for Collections.frequency to count how many types are inside List
     *  Should find a better way to do this
     * */
    @Override
    public boolean equals(Object o) {
        WiktionaryItem instance;
        if(!(o instanceof WiktionaryItem)) return false;
        else {
            instance = (WiktionaryItem) o;
            return this.itemType == instance.itemType;
        }
    }
}
