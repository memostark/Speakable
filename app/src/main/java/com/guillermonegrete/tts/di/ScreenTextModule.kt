package com.guillermonegrete.tts.di

import com.guillermonegrete.tts.services.ScreenTextService
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
interface ScreenTextModule {
    @ContributesAndroidInjector
    fun screenTextService(): ScreenTextService
}