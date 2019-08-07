package com.guillermonegrete.tts.di

import androidx.lifecycle.ViewModel
import com.guillermonegrete.tts.savedwords.SavedWordsFragment
import com.guillermonegrete.tts.savedwords.SavedWordsViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

@Module
abstract class SavedWordsModule {
    @ContributesAndroidInjector(modules = [
        ViewModelBuilder::class
    ])
    abstract fun savedWordsFragment(): SavedWordsFragment

    @Binds
    @IntoMap
    @ViewModelKey(SavedWordsViewModel::class)
    abstract fun bindViewModel(viewModel: SavedWordsViewModel): ViewModel
}