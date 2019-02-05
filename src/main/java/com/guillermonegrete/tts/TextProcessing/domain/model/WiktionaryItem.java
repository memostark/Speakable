package com.guillermonegrete.tts.TextProcessing.domain.model;

public class WiktionaryItem implements WikiItem{
    public String itemText;
    public int itemType;
    private String subHeaderText;

    public WiktionaryItem(String bodyText, String subHeaderText ){
        this.itemText = bodyText;
        this.itemType = 3;
        this.subHeaderText = subHeaderText;
    }

    /*
     *  Used for Collections.frequency to count how many types are inside List
     *  Should find a better way to do this
     * */

    public String getItemText() {
        return itemText;
    }

    public String getSubHeaderText() {
        return subHeaderText;
    }

    @Override
    public boolean equals(Object o) {
        WiktionaryItem instance;
        if(!(o instanceof WiktionaryItem)) return false;
        else {
            instance = (WiktionaryItem) o;
            return this.itemType == instance.itemType;
        }
    }

    @Override
    public RowType getItemType() {
        return RowType.LIST_ITEM;
    }
}
