package com.guillermonegrete.tts.di

import androidx.lifecycle.ViewModel
import com.guillermonegrete.tts.importtext.ImportTextViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ImportTextModule {

    @Binds
    @IntoMap
    @ViewModelKey(ImportTextViewModel::class)
    abstract fun bindViewModel(viewModel: ImportTextViewModel): ViewModel
}