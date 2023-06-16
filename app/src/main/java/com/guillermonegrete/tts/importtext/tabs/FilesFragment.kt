package com.guillermonegrete.tts.importtext.tabs

import android.Manifest
import android.animation.Animator
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.databinding.FilesLayoutBinding
import com.guillermonegrete.tts.databinding.RecentFilesMenuBinding
import com.guillermonegrete.tts.importtext.FilesViewModel
import com.guillermonegrete.tts.importtext.ImportedFileType
import com.guillermonegrete.tts.importtext.RecentFilesAdapter
import com.guillermonegrete.tts.importtext.UriValidator
import com.guillermonegrete.tts.importtext.visualize.VisualizeTextActivity
import com.guillermonegrete.tts.utils.actionBarSize
import com.guillermonegrete.tts.utils.dpToPixel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.StringBuilder

@AndroidEntryPoint
class FilesFragment: Fragment(R.layout.files_layout) {

    private  var _binding: FilesLayoutBinding? = null
    private val binding get() = _binding!!

    private var fileType = ImportedFileType.TXT

    private val viewModel: FilesViewModel by viewModels()

    private var fabOpen = false
    private var fabBottomMargin = 0

    private lateinit var pickFile: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pickFile = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data ?: return@registerForActivityResult

            if(result.resultCode == Activity.RESULT_OK){
                val uri = data.data ?: return@registerForActivityResult

                try {
                    // When getting the uri of a file using "ACTION_OPEN_DOCUMENT" this makes it persistable
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: SecurityException){
                    Timber.e("Couldn't make the uri persistable")
                }

                when(fileType){
                    ImportedFileType.EPUB -> visualizeEpub(uri, -1)
                    ImportedFileType.TXT -> readTextFile(uri)
                }
            }
        }

        val cont = context ?: return
        // So the fab is not overlapping with the action bar
        fabBottomMargin = cont.actionBarSize + cont.dpToPixel(8)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FilesLayoutBinding.bind(view)

        with(binding){

            pickFileFab.setOnClickListener { toggleButtons() }
            fabContainer.setBottomMargin(fabBottomMargin)
            // the view is centered but it's not taking into account the bottom nav bar, add the missing offset
            noFilesMessage.setBottomMargin(noFilesMessage.marginBottom + requireContext().actionBarSize / 2)

            pickTxtFileBtn.apply {
                setOnClickListener {
                    fileType = ImportedFileType.TXT
                    checkFileReadPermission()
                }
                post { translationY = height.toFloat() }
            }

            pickEpubFileBtn.apply {
                setOnClickListener {
                    fileType = ImportedFileType.EPUB
                    checkFileReadPermission()
                }
                post { translationY = height.toFloat() }
            }

            recentFilesList.layoutManager = LinearLayoutManager(context)
            recentFilesList.addOnScrollListener(object: RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy<0 && !fabContainer.isVisible)
                        fabContainer.isInvisible = false
                    else if(dy>0 && fabContainer.isVisible)
                        fabContainer.isInvisible = true
                }
            })
        }

        setViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setViewModel(){
        viewModel.apply {

            val adapter = RecentFilesAdapter(
                viewModel.filesPath,
                onClick = { visualizeEpub(Uri.parse(it.uri), it.id) },
                onMenuButtonClick = {
                    RecentFileMenu.newInstance().show(childFragmentManager, "Item menu")
                    childFragmentManager.setFragmentResultListener(RecentFileMenu.REQUEST_KEY, viewLifecycleOwner) { _, _ ->
                        // only one option so it has to be delete
                        viewModel.deleteFile(it)
                    }
                }
            )
            binding.recentFilesList.adapter = adapter

            lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    files.collect { uiState ->
                        when (uiState) {
                            is LoadResult.Error -> {
                                Toast.makeText(context, "Failed fetching recent links", Toast.LENGTH_SHORT).show()
                                Timber.e(uiState.exception, "Failed fetching recent links")
                                binding.recentFilesProgressBar.isVisible = false
                            }
                            is LoadResult.Success -> {
                                binding.noFilesMessage.isVisible = uiState.data.isEmpty()
                                adapter.submitList(uiState.data)
                                binding.recentFilesProgressBar.isVisible = false
                            }
                            LoadResult.Loading -> binding.recentFilesProgressBar.isVisible = true
                        }
                    }
                }
            }

            loadFiles()
        }
    }

    private fun checkFileReadPermission(){

        val con = context ?: return

        if (ContextCompat.checkSelfPermission(con, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                READ_STORAGE_PERMISSION_REQUEST
            )
        } else {
            pickFile()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            READ_STORAGE_PERMISSION_REQUEST -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickFile()
                } else {
                    Toast.makeText(context, getString(R.string.no_file_read_permission), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * How to persist uri access permission: https://stackoverflow.com/questions/25414352/how-to-persist-permission-in-android-api-19-kitkat
     */
    private fun pickFile() {
        // Have to use Intent.ACTION_OPEN_DOCUMENT otherwise the access to file permission is revoked after the activity is destroyed
        // More info here: Intent.ACTION_OPEN_DOCUMENT
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            type = fileType.mimeType
        }

        pickFile.launch(intent)
    }

    // TODO execute in background thread
    private fun readTextFile(uri: Uri){

        val text = StringBuilder()
        var br: BufferedReader? = null
        var inputStream: InputStream? = null

        try {
            inputStream = context?.contentResolver?.openInputStream(uri)
            br = BufferedReader(InputStreamReader(inputStream))
            var line = br.readLine()

            while (line != null) {
                text.append(line)
                text.append('\n')
                line = br.readLine()
            }

        } catch (e: IOException) {
            //You'll need to add proper error handling here
        } finally {
            br?.close()
            inputStream?.close()
        }

        if(text.isNotBlank()) visualizeText(text.toString())
    }

    private fun visualizeText(text: String){
        val intent = Intent(context, VisualizeTextActivity::class.java)
        intent.putExtra(VisualizeTextActivity.IMPORTED_TEXT, text)
        startActivity(intent)
    }

    private fun visualizeEpub(
        uri: Uri,
        fileId: Int
    ){
        val uriValidator = UriValidator()

        if (uriValidator.isLoadable(requireContext(), uri)) {

            val intent = Intent(context, VisualizeTextActivity::class.java).apply {
                action = VisualizeTextActivity.SHOW_EPUB
                putExtra(VisualizeTextActivity.EPUB_URI, uri)
                putExtra(VisualizeTextActivity.FILE_ID, fileId)
            }

            startActivity(intent)
        }else{
            Toast.makeText(context, "Couldn't open file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleButtons(){
        fabOpen = !fabOpen

        val pickEpub = binding.pickEpubFileBtn
        val pickText = binding.pickTxtFileBtn

        if(fabOpen) {
            ViewAnimation.showIn(pickEpub)
            ViewAnimation.showIn(pickText)
        }else{
            ViewAnimation.showOut(pickEpub)
            ViewAnimation.showOut(pickText)
        }
    }

    object ViewAnimation{

        fun showIn(view: View){

            with(view){
                visibility = View.VISIBLE
                alpha = 0f
                translationY = view.height.toFloat()
                animate()
                    .setDuration(200)
                    .translationY(0f)
                    .alpha(1f)
                    .setListener(SimpleAnimatorListener())
                    .start()
            }
        }

        fun showOut(view: View){

            with(view){
                visibility = View.VISIBLE
                alpha = 1f
                translationY = 0f
                animate()
                    .setDuration(200)
                    .translationY(view.height.toFloat())
                    .alpha(0f)
                    .setListener(object : SimpleAnimatorListener(){
                        override fun onAnimationEnd(animation: Animator) {
                            view.visibility = View.GONE
                            super.onAnimationEnd(animation)
                        }
                    })
                    .start()
            }
        }

        open class SimpleAnimatorListener: Animator.AnimatorListener{

            override fun onAnimationRepeat(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {}

            override fun onAnimationCancel(animation: Animator) {}

            override fun onAnimationStart(animation: Animator) {}
        }
    }

    private fun View.setBottomMargin(margin: Int) {
        (layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = margin
    }

    companion object{
        private const val READ_STORAGE_PERMISSION_REQUEST = 113
    }
}

class RecentFileMenu private constructor(): BottomSheetDialogFragment(){

    private  var _binding: RecentFilesMenuBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = RecentFilesMenuBinding.inflate(inflater, container, false)

        binding.deleteButton.setOnClickListener {
            setFragmentResult(REQUEST_KEY, bundleOf())
            dismiss()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object{
        fun newInstance() = RecentFileMenu()

        const val REQUEST_KEY = "recent_file_key"
    }
}
