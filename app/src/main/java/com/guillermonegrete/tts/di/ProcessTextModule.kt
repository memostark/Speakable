package com.guillermonegrete.tts.di

import com.guillermonegrete.tts.textprocessing.ProcessTextContract
import com.guillermonegrete.tts.textprocessing.ProcessTextPresenter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
abstract class ProcessTextModule {

    @Binds
    abstract fun bindPresenter(presenter: ProcessTextPresenter): ProcessTextContract.Presenter
}