package com.guillermonegrete.tts.importtext

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.data.preferences.SettingsRepository
import com.guillermonegrete.tts.databinding.FragmentImportTextBinding
import com.guillermonegrete.tts.importtext.tabs.EnterTextFragment
import com.guillermonegrete.tts.importtext.tabs.FilesFragment
import com.guillermonegrete.tts.importtext.tabs.WebLinksFragment
import com.guillermonegrete.tts.utils.dpToPixel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ImportTextFragment: Fragment(R.layout.fragment_import_text) {

    private  var _binding: FragmentImportTextBinding? = null
    private val binding get() = _binding!!

    val args: ImportTextFragmentArgs by navArgs()

    @Inject lateinit var settings: SettingsRepository

    private var offsetMargin = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        offsetMargin = args.offsetMargin + 48.dpToPixel
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                FILES_INDEX -> "Files"
                WEB_LINKS_INDEX -> "Links"
                ENTER_TEXT_INDEX -> "Text"
                else -> ""
            }
        }.attach()
    }

    override fun onResume() {
        super.onResume()
        // Only show bar when this fragment is fully visible, showing the bar earlier causes problems with nav animations
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
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

    inner class ImportAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle): FragmentStateAdapter(fragmentManager, lifecycle){

        override fun getItemCount() = 3

        override fun createFragment(position: Int): Fragment {
            return when(position){
                FILES_INDEX -> FilesFragment.newInstance(offsetMargin)
                WEB_LINKS_INDEX -> WebLinksFragment.newInstance(offsetMargin)
                ENTER_TEXT_INDEX -> EnterTextFragment()
                else -> throw IllegalStateException("Out of position: $position")
            }
        }
    }

    companion object{
        const val FILES_INDEX = 0
        const val WEB_LINKS_INDEX = 1
        const val ENTER_TEXT_INDEX = 2

        const val MARGIN_OFFSET_NAME = "offsetMargin"
    }
}
