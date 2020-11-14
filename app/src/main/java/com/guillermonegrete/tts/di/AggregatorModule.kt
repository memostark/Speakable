package com.guillermonegrete.tts.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module(includes = [
    ApplicationModule::class,
    ProcessTextModule::class,
    VisualizeTextModule::class,
    SavedWordsModule::class,
    ImportTextModule::class
])
interface AggregatorModule
