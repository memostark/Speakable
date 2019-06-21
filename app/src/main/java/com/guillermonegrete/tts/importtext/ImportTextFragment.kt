package com.guillermonegrete.tts.importtext

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Xml
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.guillermonegrete.tts.R
import org.xmlpull.v1.XmlPullParser
import java.io.*
import java.lang.StringBuilder


class ImportTextFragment: Fragment() {

    private lateinit var clipboardManager: ClipboardManager
    private var fileType = ImportedFileType.TXT

    override fun onAttach(context: Context) {
        super.onAttach(context)
        clipboardManager = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_import_text, container, false)
        val editText: TextView = root.findViewById(R.id.import_text_edit)

        val pasteButton: Button = root.findViewById(R.id.paste_btn)
        pasteButton.setOnClickListener { editText.text = getClipboardText() }

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

        return root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(data != null && resultCode == RESULT_OK){
            when(requestCode){
                REQUEST_PICK_FILE -> {
                    val uri = data.data
                    if(uri != null) {
                        when(fileType){
                            ImportedFileType.EPUB -> readEpubFile(uri)
                            ImportedFileType.TXT -> readTextFile(uri)
                        }
                    }
                }
            }
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

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).setType(fileType.mimeType)
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

    private fun readEpubFile(uri: Uri) {
        println("Epub selected $uri, path: ${uri.path}")
        val path = uri.path
        if(path != null) {
            context?.let {
//                book.fetchFromZip(uri, it)

                val rootStream = it.contentResolver.openInputStream(uri)
                val parser: XmlPullParser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)

                val epubParser = EpubParser()

                val book = epubParser.parseBook(parser, rootStream)
                visualizeText(book.chapters.first())
            }
        }
    }

    private fun visualizeText(text: String){
        val intent = Intent(context, VisualizeTextActivity::class.java)
        intent.putExtra(VisualizeTextActivity.IMPORTED_TEXT, text)
        startActivity(intent)
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