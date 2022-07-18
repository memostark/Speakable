package com.guillermonegrete.tts.importtext.tabs

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.databinding.FragmentWebLinksListBinding
import com.guillermonegrete.tts.db.WebLink
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * A fragment representing a list of Items.
 */
@AndroidEntryPoint
class WebLinksFragment : Fragment(R.layout.fragment_web_links_list) {

    private val viewModel: WebLinksViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentWebLinksListBinding.bind(view)

        lifecycleScope.launch {
            // repeatOnLifecycle launches the block in a new coroutine every time the
            // lifecycle is in the STARTED state (or above) and cancels it when it's STOPPED.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Trigger the flow and start listening for values.
                // Note that this happens when lifecycle is STARTED and stops
                // collecting when the lifecycle is STOPPED
                viewModel.uiState.collect { uiState ->
                    when (uiState) {
                        is LoadResult.Error -> Toast.makeText(context, "Failed fetching recent links", Toast.LENGTH_SHORT).show()
                        is LoadResult.Success -> binding.list.adapter = WebLinkAdapter(uiState.data)
                        LoadResult.Loading -> println("Loading links...")
                    }
                }
            }
        }

        binding.addBtn.setOnClickListener {
            viewModel.addNew(WebLink("https://en.wikipedia.org/wiki/Painting"))
        }

        viewModel.getRecentLinks()
    }

    companion object {
        @JvmStatic
        fun newInstance() = WebLinksFragment()
    }
}