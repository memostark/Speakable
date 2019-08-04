package com.guillermonegrete.tts.di

import androidx.lifecycle.ViewModel
import com.guillermonegrete.tts.importtext.VisualizeTextActivity
import com.guillermonegrete.tts.importtext.VisualizeTextViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

@Module
abstract class VisualizeTextModule {
    @ContributesAndroidInjector(modules = [
        ViewModelBuilder::class
    ])
    abstract fun visualizeTextActivity(): VisualizeTextActivity

    @Binds
    @IntoMap
    @ViewModelKey(VisualizeTextViewModel::class)
    abstract fun bindViewModel(viewModel: VisualizeTextViewModel): ViewModel
}