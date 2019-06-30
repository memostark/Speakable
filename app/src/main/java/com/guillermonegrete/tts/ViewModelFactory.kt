package com.guillermonegrete.tts

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.guillermonegrete.tts.importtext.EpubParser
import com.guillermonegrete.tts.importtext.VisualizeTextViewModel

class ViewModelFactory private constructor(
    private val epubParser: EpubParser
): ViewModelProvider.NewInstanceFactory(){

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>) =
        with(modelClass){
            when{
                isAssignableFrom(VisualizeTextViewModel::class.java) ->
                    VisualizeTextViewModel(epubParser)
                else ->
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }as T

    companion object{
        @Volatile private var INSTANCE: ViewModelFactory? = null

        fun getInstance(application: Application): ViewModelFactory{
            val epubParser = EpubParser()

            return INSTANCE ?: synchronized(ViewModelFactory::class.java){
                INSTANCE ?: ViewModelFactory(epubParser)
            }
        }
    }
}