package com.guillermonegrete.tts.importtext

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.importtext.tabs.EnterTextFragment
import com.guillermonegrete.tts.importtext.tabs.FilesFragment

class ImportTextFragment: Fragment() {

    private lateinit var pager: ViewPager2

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_import_text, container, false)

        pager = root.findViewById(R.id.import_text_pager)
        pager.adapter = ImportAdapter(this)

        val tabLayout: TabLayout = root.findViewById(R.id.import_tab_layout)
        TabLayoutMediator(tabLayout, pager,
            TabLayoutMediator.TabConfigurationStrategy { tab, position ->
                tab.text = when(position){
                    0 -> "Files"
                    1 -> "Text"
                    else -> ""
                }
            }).attach()

        return root
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