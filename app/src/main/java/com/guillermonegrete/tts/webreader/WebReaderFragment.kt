package com.guillermonegrete.tts.webreader

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.databinding.FragmentWebReaderBinding

class WebReaderFragment : Fragment(R.layout.fragment_web_reader){

    private val viewModel: WebReaderViewModel by viewModels()

    private  var _binding: FragmentWebReaderBinding? = null
    private val binding get() = _binding!!

    val args: WebReaderFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        _binding = FragmentWebReaderBinding.bind(view)

        viewModel.page.observe(viewLifecycleOwner, {
            binding.bodyText.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(it, Html.FROM_HTML_MODE_COMPACT)
            } else {
                Html.fromHtml(it)
            }
        })

        viewModel.loadDoc(args.link)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}