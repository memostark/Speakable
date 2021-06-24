package com.guillermonegrete.tts.webreader

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.guillermonegrete.tts.R

class WebReaderFragment : Fragment(R.layout.fragment_web_reader){

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }
}