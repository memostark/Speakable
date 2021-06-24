package com.guillermonegrete.tts.webreader

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.databinding.FragmentWebReaderBinding

class WebReaderFragment : Fragment(R.layout.fragment_web_reader){

    private  var _binding: FragmentWebReaderBinding? = null
    private val binding get() = _binding!!

    val args: WebReaderFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        _binding = FragmentWebReaderBinding.bind(view)

        with(binding){
            urlText.text = args.link
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}