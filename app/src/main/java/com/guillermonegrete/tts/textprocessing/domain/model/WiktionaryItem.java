package com.guillermonegrete.tts.textprocessing.domain.model;

public class WiktionaryItem implements WikiItem{
    private final CharSequence itemText;
    private final String subHeaderText;

    public WiktionaryItem(CharSequence bodyText, String subHeaderText ){
        this.itemText = bodyText;
        this.subHeaderText = subHeaderText;
    }

    public CharSequence getItemText() {
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
