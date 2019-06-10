package com.guillermonegrete.tts.textprocessing.domain.model;

public class WiktionaryItem implements WikiItem{
    private String itemText;
    private String subHeaderText;

    public WiktionaryItem(String bodyText, String subHeaderText ){
        this.itemText = bodyText;
        this.subHeaderText = subHeaderText;
    }

    public String getItemText() {
        return itemText;
    }

    public String getSubHeaderText() {
        return subHeaderText;
    }

    @Override
    public RowType getItemType() {
        return RowType.LIST_ITEM;
    }
}
