package com.guillermonegrete.tts.textprocessing.domain.model;

public interface WikiItem {
    RowType getItemType();

    enum RowType {
        LIST_ITEM, HEADER_ITEM
    }
}
