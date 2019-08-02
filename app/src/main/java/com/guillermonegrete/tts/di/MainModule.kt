package com.guillermonegrete.tts.di

import com.guillermonegrete.tts.main.TextToSpeechFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
interface MainModule{
    @ContributesAndroidInjector
    fun mainFragment(): TextToSpeechFragment
}