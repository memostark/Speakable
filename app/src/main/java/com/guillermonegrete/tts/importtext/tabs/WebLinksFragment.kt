package com.guillermonegrete.tts.importtext.tabs

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.common.compose.YesNoDialog
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.databinding.DialogOpenLinkBinding
import com.guillermonegrete.tts.databinding.FragmentWebLinksListBinding
import com.guillermonegrete.tts.importtext.ImportTextFragmentDirections
import com.guillermonegrete.tts.ui.theme.AppTheme
import com.guillermonegrete.tts.utils.dpToPixel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * A fragment representing a list of Items.
 */
@AndroidEntryPoint
class WebLinksFragment : Fragment(R.layout.fragment_web_links_list) {

    private val viewModel: WebLinksViewModel by viewModels()

    private var _binding: FragmentWebLinksListBinding? = null
    private val binding get() = _binding!!

    private var fabBottomMargin = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fabBottomMargin = requireArguments().getInt(MARGIN_OFFSET_KEY) + 8.dpToPixel
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWebLinksListBinding.bind(view)
        val adapter = WebLinkAdapter { viewModel.setSelectedLink(it) }
        binding.list.adapter = adapter

        lifecycleScope.launch {
            // repeatOnLifecycle launches the block in a new coroutine every time the
            // lifecycle is in the STARTED state (or above) and cancels it when it's STOPPED.
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Trigger the flow and start listening for values.
                // Note that this happens when lifecycle is STARTED and stops
                // collecting when the lifecycle is STOPPED
                viewModel.uiState.collect { uiState ->
                    when (uiState) {
                        is LoadResult.Error -> Toast.makeText(context, "Failed fetching recent links", Toast.LENGTH_SHORT).show()
                        is LoadResult.Success -> {
                            binding.noLinksMessage.isVisible = uiState.data.isEmpty()
                            adapter.submitList(uiState.data)
                        }
                        LoadResult.Loading -> {}
                    }
                    binding.webLinksProgressBar.isVisible = uiState is LoadResult.Loading
                }
            }
        }

        with(binding){
            list.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            list.addOnScrollListener(object: RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy<0 && !addBtn.isShown)
                        addBtn.isInvisible = false
                    else if(dy>0 && addBtn.isShown)
                        addBtn.isInvisible = true
                }
            })

            addBtn.setOnClickListener { showAddNewDialog() }
            (addBtn.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = fabBottomMargin
            ViewCompat.setOnApplyWindowInsetsListener(root) { v, rootInsets ->
                val insets = rootInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
                addBtn.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = insets.bottom + insets.top + fabBottomMargin
                }
                list.updatePadding(bottom = insets.bottom)

                rootInsets
            }
        }
        
        setUpCompose()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun setUpCompose() {
        val externalPath = context?.getExternalFilesDir(null)?.absolutePath.toString()

        binding.composeRoot.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                AppTheme {

                    val link = viewModel.showBottomSheet
                    if (link != null) {
                        var deleteDialogShown by rememberSaveable { mutableStateOf(false) }

                        ModalBottomSheet(
                            onDismissRequest = { viewModel.removeSelectedLink() },
                        ) {
                            WebLinkMenu { item ->
                                when (item) {
                                    WebLinkMenuItem.DELETE -> deleteDialogShown = true
                                    WebLinkMenuItem.COPY_LINK -> {
                                        val clipboardManager = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val linkText = requireContext().getString(R.string.link_description)
                                        clipboardManager.setPrimaryClip(ClipData.newPlainText(linkText, link.url))
                                        viewModel.removeSelectedLink()
                                    }
                                }
                            }
                        }

                        if (deleteDialogShown) {
                            YesNoDialog(
                                onDismissRequest = { deleteDialogShown = false },
                                onConfirmation = {
                                    viewModel.delete(link, externalPath)
                                    deleteDialogShown = false
                                },
                                dialogTitle = context.getString(R.string.delete_item),
                                dialogText = null
                            )
                        }
                    }
                }
            }
        }
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
        fun newInstance(marginOffset: Int = 0) = WebLinksFragment().apply {
            arguments = bundleOf(MARGIN_OFFSET_KEY to marginOffset)
        }

        const val MARGIN_OFFSET_KEY = "margin_offset_key"
    }
}

@Composable
fun WebLinkMenu(onItemClick: (item: WebLinkMenuItem) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        val deleteDesc = stringResource(R.string.delete)
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Filled.Delete, deleteDesc) },
            text = { Text(deleteDesc) },
            onClick = { onItemClick(WebLinkMenuItem.DELETE) }
        )

        DropdownMenuItem(
            leadingIcon = { Icon(painterResource(R.drawable.baseline_link_24), stringResource(R.string.link_description)) },
            text = { Text(stringResource(R.string.copy_link)) },
            onClick = { onItemClick(WebLinkMenuItem.COPY_LINK) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun WebLinkMenuPreview() {
    AppTheme {
        Surface {
            WebLinkMenu {}
        }
    }
}

enum class WebLinkMenuItem {
    DELETE,
    COPY_LINK;
}
