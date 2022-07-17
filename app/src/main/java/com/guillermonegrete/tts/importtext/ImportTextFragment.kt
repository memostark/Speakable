package com.guillermonegrete.tts.importtext

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.databinding.FragmentImportTextBinding
import com.guillermonegrete.tts.importtext.tabs.EnterTextFragment
import com.guillermonegrete.tts.importtext.tabs.FilesFragment
import com.guillermonegrete.tts.importtext.tabs.WebLinksFragment

class ImportTextFragment: Fragment(R.layout.fragment_import_text) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        val binding = FragmentImportTextBinding.bind(view)

        val pager = binding.importTextPager
        pager.adapter = ImportAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)

        TabLayoutMediator(binding.importTabLayout, pager) { tab, position ->
            tab.text = when (position) {
                0 -> "Files"
                1 -> "Links"
                2 -> "Text"
                else -> ""
            }
        }.attach()
    }

    class ImportAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle): FragmentStateAdapter(fragmentManager, lifecycle){

        override fun getItemCount() = 3

        override fun createFragment(position: Int): Fragment {
            return when(position){
                0 -> FilesFragment()
                1 -> WebLinksFragment.newInstance()
                2 -> EnterTextFragment()
                else -> throw IllegalStateException("Out of position: $position")
            }
        }
    }
}