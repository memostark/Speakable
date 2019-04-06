package com.guillermonegrete.tts.TextProcessing.domain.interactors

import com.guillermonegrete.tts.TextProcessing.domain.model.WikiItem

interface GetDictionaryEntryInteractor{
    interface Callback{
        fun onDictionaryLayoutDetermined(items: MutableList<WikiItem>)

        fun onEntryNotAvailable()
    }
}