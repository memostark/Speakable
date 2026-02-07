package com.guillermonegrete.tts.common.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.navigation.fragment.navArgs
import com.guillermonegrete.tts.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlin.getValue

@AndroidEntryPoint
class NotesListFragment: Fragment() {

    private val viewModel: NotesListViewModel by viewModels(extrasProducer = {
        defaultViewModelCreationExtras.withCreationCallback<NotesListViewModel.Factory> { factory ->
            factory.create(args.id)
        }
    })

    private val args: NotesListFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = content {
        AppTheme {
            NotesListScreen(viewModel)
        }
    }
}
