package com.guillermonegrete.tts.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.findNavController
import com.guillermonegrete.tts.R

/**
 * A fragment representing a list of Items.
 */
class PickLanguageFragment : Fragment(R.layout.fragment_pick_language) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (view is RecyclerView) {
            with(view) {
                val auto = context.getString(R.string.auto_detect)
                val values = listOf(auto, "English", "Spanish", "French")

                layoutManager =  LinearLayoutManager(context)
                adapter = LanguageAdapter(values) {
                    setFragmentResult("requestKey", bundleOf("lang" to values[it]))
                    findNavController().navigateUp()
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = PickLanguageFragment()
    }
}