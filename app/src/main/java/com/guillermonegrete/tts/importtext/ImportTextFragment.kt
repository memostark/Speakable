package com.guillermonegrete.tts.importtext

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.databinding.FragmentImportTextBinding
import com.guillermonegrete.tts.importtext.tabs.EnterTextFragment
import com.guillermonegrete.tts.importtext.tabs.FilesFragment

class ImportTextFragment: Fragment(R.layout.fragment_import_text) {

    private  var _binding: FragmentImportTextBinding? = null
    private val binding get() = _binding!!

    private lateinit var pager: ViewPager2

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentImportTextBinding.bind(view)

        pager = binding.importTextPager
        pager.adapter = ImportAdapter(this)

        TabLayoutMediator(binding.importTabLayout, pager,
            TabLayoutMediator.TabConfigurationStrategy { tab, position ->
                tab.text = when(position){
                    0 -> "Files"
                    1 -> "Text"
                    else -> ""
                }
            }).attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class ImportAdapter(fragment: Fragment): FragmentStateAdapter(fragment){

        override fun getItemCount() = 2

        override fun createFragment(position: Int): Fragment {
            return when(position){
                0 -> FilesFragment()
                1 -> EnterTextFragment()
                else -> throw IllegalStateException("Out of position: $position")
            }
        }
    }
}