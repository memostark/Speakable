package com.guillermonegrete.tts.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import com.guillermonegrete.tts.R

/**
 * A fragment representing a list of Items.
 */
class PickLanguageFragment : Fragment(R.layout.fragment_pick_language) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (view is RecyclerView) {
            with(view) {
                layoutManager =  LinearLayoutManager(context)
                adapter = LanguageAdapter(listOf("English", "Spanish", "French"))
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = PickLanguageFragment()
    }
}