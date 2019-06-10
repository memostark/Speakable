package com.guillermonegrete.tts.textprocessing.domain.interactors

import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem

interface GetDictionaryEntryInteractor{
    interface Callback{
        fun onDictionaryLayoutDetermined(items: MutableList<WikiItem>)

        fun onEntryNotAvailable()
    }
}