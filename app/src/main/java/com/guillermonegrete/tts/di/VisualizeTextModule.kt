package com.guillermonegrete.tts.di

import com.guillermonegrete.tts.importtext.VisualizeTextActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
interface VisualizeTextModule {
    @ContributesAndroidInjector
    fun visualizeTextActivity(): VisualizeTextActivity
}