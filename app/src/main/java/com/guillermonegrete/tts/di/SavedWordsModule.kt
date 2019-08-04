package com.guillermonegrete.tts.di

import com.guillermonegrete.tts.savedwords.SavedWordsFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
interface SavedWordsModule {
    @ContributesAndroidInjector
    fun savedWordsFragment(): SavedWordsFragment
}