package com.guillermonegrete.tts.importtext.tabs

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.databinding.FragmentWebLinksListBinding
import com.guillermonegrete.tts.db.WebLink

/**
 * A fragment representing a list of Items.
 */
class WebLinksFragment : Fragment(R.layout.fragment_web_links_list) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentWebLinksListBinding.bind(view)
        val dummyData = listOf(
            WebLink("https://en.wikipedia.org/wiki/Painting"),
            WebLink("https://en.wikipedia.org/wiki/Photorealism")
        )


        binding.list.adapter = WebLinkAdapter(dummyData)
    }

    companion object {
        @JvmStatic
        fun newInstance() = WebLinksFragment()
    }
}