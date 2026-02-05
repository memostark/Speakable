package com.guillermonegrete.tts.common.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import com.guillermonegrete.tts.ui.theme.AppTheme

class NotesListFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = content {
        AppTheme {
            val dummyNotes = listOf(NoteItem("Dummy text", "Dummy note text"), NoteItem("Another dummy text", "More dummy note text"))
            NotesListScreen(dummyNotes)
        }
    }
}
