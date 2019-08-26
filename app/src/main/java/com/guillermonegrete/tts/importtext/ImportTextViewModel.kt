package com.guillermonegrete.tts.importtext

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.guillermonegrete.tts.db.BookFile

class ImportTextViewModel: ViewModel() {

    private val _openTextVisualizer = MutableLiveData<BookFile>()
    val openTextVisualizer: LiveData<BookFile> = _openTextVisualizer

    fun openVisualizer(book: BookFile){
        _openTextVisualizer.value = book
    }
}