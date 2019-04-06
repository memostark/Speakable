package com.guillermonegrete.tts.TextProcessing.domain.model;

public interface WikiItem {
    RowType getItemType();

    enum RowType {
        LIST_ITEM, HEADER_ITEM
    }
}
