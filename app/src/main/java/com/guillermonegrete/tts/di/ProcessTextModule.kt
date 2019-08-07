package com.guillermonegrete.tts.di

import com.guillermonegrete.tts.textprocessing.ProcessTextActivity
import com.guillermonegrete.tts.textprocessing.ProcessTextContract
import com.guillermonegrete.tts.textprocessing.ProcessTextPresenter
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ProcessTextModule {
    @ContributesAndroidInjector
    abstract fun processTextActivity(): ProcessTextActivity

    @Binds
    abstract fun bindPresenter(presenter: ProcessTextPresenter): ProcessTextContract.Presenter
}