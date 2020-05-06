package com.guillermonegrete.tts.importtext

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.ClipboardManager
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
import com.google.android.material.textfield.TextInputEditText
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.importtext.visualize.VisualizeTextActivity
import dagger.android.support.AndroidSupportInjection
import java.io.*
import java.lang.StringBuilder
import javax.inject.Inject


class ImportTextFragment: Fragment() {

    private lateinit var clipboardManager: ClipboardManager
    private var fileType = ImportedFileType.TXT

    private lateinit var recentFilesList: RecyclerView
    private lateinit var adapter: RecentFilesAdapter

    private lateinit var progressBar: ProgressBar

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(ImportTextViewModel::class.java)
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        clipboardManager = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_import_text, container, false)
        val editText: TextInputEditText = root.findViewById(R.id.import_text_edit)

        val pasteButton: Button = root.findViewById(R.id.paste_btn)
        pasteButton.setOnClickListener { editText.setText(getClipboardText()) }

        val visualizeButton: Button = root.findViewById(R.id.visualize_btn)
        visualizeButton.setOnClickListener {
            visualizeText(editText.text.toString())
        }

        root.findViewById<Button>(R.id.pick_txt_file_btn).apply {
            setOnClickListener {
                fileType = ImportedFileType.TXT
                checkPermissions()
            }
        }

        root.findViewById<Button>(R.id.pick_epub_file_btn).apply {
            setOnClickListener {
                fileType = ImportedFileType.EPUB
                checkPermissions()
            }
        }

        progressBar = root.findViewById(R.id.recent_files_progress_bar)

        setViewModel()

        recentFilesList = root.findViewById(R.id.recent_files_list)
        recentFilesList.layoutManager = LinearLayoutManager(context)

        return root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(data != null && resultCode == RESULT_OK){
            when(requestCode){
                REQUEST_PICK_FILE -> {
                    val uri = data.data
                    if(uri != null) {
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
    }

    private fun setViewModel(){
        viewModel.apply {
            openTextVisualizer.observe(viewLifecycleOwner, Observer {
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
        }
    }

    private fun checkPermissions(){

        context?.let{

            if (ContextCompat.checkSelfPermission(it, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    READ_STORAGE_PERMISSION_REQUEST)
            } else {
                pickFile()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            READ_STORAGE_PERMISSION_REQUEST -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickFile()
                }else {
                    Toast.makeText(context, "Read storage permission is necessary to load images", Toast.LENGTH_SHORT).show()
                }
                return
            }
            else -> {}
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
            println("Epub uri: $uri")
            startActivity(intent)
        }else{
            Toast.makeText(context, "Couldn't open file", Toast.LENGTH_SHORT).show()
        }
    }



    private fun getClipboardText(): String{

        val clip = clipboardManager.primaryClip ?: return ""
        if (clip.itemCount <= 0) return ""
        val pasteData = clip.getItemAt(0).text
        return pasteData?.toString() ?: ""
    }

    companion object{
        private const val REQUEST_PICK_FILE = 112
        private const val READ_STORAGE_PERMISSION_REQUEST = 113
    }

}