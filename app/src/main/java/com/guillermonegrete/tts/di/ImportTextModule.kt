package com.guillermonegrete.tts.di

import androidx.lifecycle.ViewModel
import com.guillermonegrete.tts.importtext.ImportTextViewModel
import com.guillermonegrete.tts.importtext.tabs.FilesFragment
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

@Module
abstract class ImportTextModule {
    @ContributesAndroidInjector(modules = [
        ViewModelBuilder::class
    ])
    abstract fun filesFragment(): FilesFragment

    @Binds
    @IntoMap
    @ViewModelKey(ImportTextViewModel::class)
    abstract fun bindViewModel(viewModel: ImportTextViewModel): ViewModel
}