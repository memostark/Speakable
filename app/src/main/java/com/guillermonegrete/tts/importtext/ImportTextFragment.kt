package com.guillermonegrete.tts.importtext

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.data.preferences.SettingsRepository
import com.guillermonegrete.tts.databinding.FragmentImportTextBinding
import com.guillermonegrete.tts.importtext.tabs.EnterTextFragment
import com.guillermonegrete.tts.importtext.tabs.FilesFragment
import com.guillermonegrete.tts.importtext.tabs.WebLinksFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ImportTextFragment: Fragment(R.layout.fragment_import_text) {

    private  var _binding: FragmentImportTextBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var settings: SettingsRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        _binding = FragmentImportTextBinding.bind(view)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settings.getImportTabPosition().collect {
                    binding.importTextPager.setCurrentItem(it, false)
                }
            }
        }

        val pager = binding.importTextPager
        pager.adapter = ImportAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)

        TabLayoutMediator(binding.importTabLayout, pager) { tab, position ->
            tab.text = when (position) {
                FilesIndex -> "Files"
                WebLinksIndex -> "Links"
                EnterTextIndex -> "Text"
                else -> ""
            }
        }.attach()
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch {
            settings.setImportTabPosition(binding.importTextPager.currentItem)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class ImportAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle): FragmentStateAdapter(fragmentManager, lifecycle){

        override fun getItemCount() = 3

        override fun createFragment(position: Int): Fragment {
            return when(position){
                FilesIndex -> FilesFragment()
                WebLinksIndex -> WebLinksFragment.newInstance()
                EnterTextIndex -> EnterTextFragment()
                else -> throw IllegalStateException("Out of position: $position")
            }
        }
    }

    companion object{

        const val FilesIndex = 0
        const val WebLinksIndex = 1
        const val EnterTextIndex = 2
    }
}
