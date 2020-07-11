package com.guillermonegrete.tts.importtext.tabs

import android.Manifest
import android.animation.Animator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.guillermonegrete.tts.EventObserver
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.databinding.FilesLayoutBinding
import com.guillermonegrete.tts.importtext.ImportTextViewModel
import com.guillermonegrete.tts.importtext.ImportedFileType
import com.guillermonegrete.tts.importtext.RecentFilesAdapter
import com.guillermonegrete.tts.importtext.UriValidator
import com.guillermonegrete.tts.importtext.visualize.VisualizeTextActivity
import dagger.android.support.AndroidSupportInjection
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.StringBuilder
import javax.inject.Inject

class FilesFragment: Fragment(R.layout.files_layout) {

    private  var _binding: FilesLayoutBinding? = null
    private val binding get() = _binding!!

    private var fileType = ImportedFileType.TXT

    private lateinit var adapter: RecentFilesAdapter

    private lateinit var recentFilesList: RecyclerView
    private lateinit var progressBar: ProgressBar

    private lateinit var pickEpubBtn: FloatingActionButton
    private lateinit var pickTxtBtn: FloatingActionButton

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(ImportTextViewModel::class.java)
    }

    private var fabOpen = false

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FilesLayoutBinding.bind(view)

        progressBar = binding.recentFilesProgressBar

        binding.pickFileFab.setOnClickListener { toggleButtons() }

        pickTxtBtn = binding.pickTxtFileBtn.apply {
            setOnClickListener {
                fileType = ImportedFileType.TXT
                checkFileReadPermission()
            }
            post { translationY = height.toFloat() }
        }

        pickEpubBtn = binding.pickEpubFileBtn.apply {
            setOnClickListener {
                fileType = ImportedFileType.EPUB
                checkFileReadPermission()
            }
            post { translationY = height.toFloat() }
        }

        recentFilesList = binding.recentFilesList
        recentFilesList.layoutManager = LinearLayoutManager(context)

        setViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setViewModel(){
        viewModel.apply {
            openTextVisualizer.observe(viewLifecycleOwner, EventObserver {
                visualizeEpub(Uri.parse(it.uri), it.id)
            })

            dataLoading.observe(viewLifecycleOwner, Observer {
                progressBar.visibility = if(it) View.VISIBLE else View.INVISIBLE
            })

            loadRecentFiles()
            files.observe(viewLifecycleOwner, Observer {
                adapter = RecentFilesAdapter(it, viewModel)
                recentFilesList.adapter = adapter
            })

            openItemMenu.observe(viewLifecycleOwner, EventObserver{
                RecentFileMenu.newInstance(it).show(childFragmentManager, "Item menu")
            })
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
        startActivityForResult(intent, REQUEST_PICK_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        data ?: return

        if(resultCode == Activity.RESULT_OK){
            when(requestCode){
                REQUEST_PICK_FILE -> {
                    val uri = data.data ?: return

                    // Necessary for persisting URIs
                    val takeFlags = data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
                    requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)

                    when(fileType){
                        ImportedFileType.EPUB -> visualizeEpub(uri, -1)
                        ImportedFileType.TXT -> readTextFile(uri)
                    }
                }
            }
        }
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

        if(fabOpen) {
            ViewAnimation.showIn(pickEpubBtn)
            ViewAnimation.showIn(pickTxtBtn)
        }else{
            ViewAnimation.showOut(pickEpubBtn)
            ViewAnimation.showOut(pickTxtBtn)
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
                        override fun onAnimationEnd(animation: Animator?) {
                            view.visibility = View.GONE
                            super.onAnimationEnd(animation)
                        }
                    })
                    .start()
            }
        }

        open class SimpleAnimatorListener: Animator.AnimatorListener{

            override fun onAnimationRepeat(animation: Animator?) {}

            override fun onAnimationEnd(animation: Animator?) {}

            override fun onAnimationCancel(animation: Animator?) {}

            override fun onAnimationStart(animation: Animator?) {}
        }
    }

    class RecentFileMenu private constructor(): BottomSheetDialogFragment(){

        private var itemPos: Int? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            itemPos = arguments?.getInt(item_pos_key)
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val root = inflater.inflate(R.layout.recent_files_menu, container, false)

            val menu = root.findViewById<Button>(R.id.delete_button)
            menu.setOnClickListener { Toast.makeText(context, "Delete item: $itemPos", Toast.LENGTH_SHORT).show() }

            return root
        }

        companion object{
            fun newInstance(itemPos: Int) = RecentFileMenu().apply {
                arguments = Bundle().apply {
                    putInt(item_pos_key, itemPos)
                }
            }

            private const val item_pos_key = "item_pos"
        }

    }

    companion object{
        private const val REQUEST_PICK_FILE = 112
        private const val READ_STORAGE_PERMISSION_REQUEST = 113
    }
}