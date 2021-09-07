package com.guillermonegrete.tts.di

import com.guillermonegrete.tts.textprocessing.ProcessTextContract
import com.guillermonegrete.tts.textprocessing.ProcessTextPresenter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.scopes.FragmentScoped

@InstallIn(FragmentComponent::class)
@Module
abstract class ProcessTextModuleBinds {

    @FragmentScoped
    @Binds
    abstract fun bindPresenter(presenter: ProcessTextPresenter): ProcessTextContract.Presenter
}