package com.guillermonegrete.tts.importtext.tabs

import android.app.AlertDialog
import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.databinding.DialogOpenLinkBinding
import com.guillermonegrete.tts.databinding.FragmentWebLinksListBinding
import com.guillermonegrete.tts.importtext.ImportTextFragmentDirections
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * A fragment representing a list of Items.
 */
@AndroidEntryPoint
class WebLinksFragment : Fragment(R.layout.fragment_web_links_list) {

    private val viewModel: WebLinksViewModel by viewModels()

    private  var _binding: FragmentWebLinksListBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWebLinksListBinding.bind(view)

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
                        is LoadResult.Success -> binding.list.adapter = WebLinkAdapter(uiState.data) { viewModel.delete(it) }
                        LoadResult.Loading -> println("Loading links...")
                    }
                }
            }
        }

        binding.list.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        binding.addBtn.setOnClickListener { showAddNewDialog() }

        viewModel.getRecentLinks()
    }

    private fun showAddNewDialog() {
        val builder = AlertDialog.Builder(context)
        val dialogBinding = DialogOpenLinkBinding.inflate(layoutInflater)
        val urlEdit = dialogBinding.input

        builder.setView(dialogBinding.root)
        builder.setTitle(getString(R.string.open_link)).setPositiveButton(R.string.add) { _, _ ->
            val action = ImportTextFragmentDirections.toWebReaderFragment(urlEdit.text.toString())
            findNavController().navigate(action)
        }.setNegativeButton(R.string.cancel) { dialogInterface, _ ->
            dialogInterface.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
        val posBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        posBtn.isEnabled = false

        // Enable the positive button when a valid url is set
        urlEdit.doAfterTextChanged {
            posBtn.isEnabled = Patterns.WEB_URL.matcher(it.toString()).matches()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.list.adapter = null
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = WebLinksFragment()
    }
}