package com.guillermonegrete.tts.importtext

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ImportTextViewModel: ViewModel() {

    private val _openTextVisualizer = MutableLiveData<Uri>()
    val openTextVisualizer: LiveData<Uri> = _openTextVisualizer

    fun openVisualizer(uri: Uri){
        _openTextVisualizer.value = uri
    }
}