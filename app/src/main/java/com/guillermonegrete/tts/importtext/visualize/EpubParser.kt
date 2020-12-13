package com.guillermonegrete.tts.importtext.visualize

import android.os.Build
import android.text.Html
import android.text.Spanned
import androidx.annotation.VisibleForTesting
import com.guillermonegrete.tts.importtext.epub.Book
import com.guillermonegrete.tts.importtext.visualize.epub.NCXParser
import com.guillermonegrete.tts.importtext.visualize.epub.OPFParser
import kotlinx.coroutines.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.*
import java.lang.StringBuilder
import javax.inject.Inject

/**
 * Based on this project: https://www.codeproject.com/Articles/592909/EPUB-Viewer-for-Android-with-Text-to-Speech
 */
class EpubParser constructor(
    private val parser: XmlPullParser,
    private val defaultDispatcher: CoroutineDispatcher
) {
    @Inject constructor(parser: XmlPullParser): this(parser, Dispatchers.Default)

    var basePath = ""
        private set

    private var currentFolder: String? = null
    private var chapterLength = 0
    private var currentChapter = ""

    private val ns: String? = null

    private val opfParser = OPFParser(parser)
    private val ncxParser = NCXParser(parser)

    init {
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
    }

    suspend fun parseBook(
        zipFileReader: ZipFileReader
    ): Book {
        // Create input stream that supports reset, so we can use it multiple times.
        return withContext(defaultDispatcher){
            val readers = zipFileReader.getAllReaders()

            val containerReader = readers[CONTAINER_FILE_PATH]
            parser.setInput(containerReader)
            parser.nextTag()
            val opfPath = parseContainerFile(parser)
            extractBasePath(opfPath)

            val opfReader = readers[opfPath]
            parser.setInput(opfReader)
            parser.nextTag()

            val tempBook = opfParser.parse()

            // TODO add media-type field to manifest and get path by searching for media-type: "application/x-dtbncx+xml"
            val tocPath = tempBook.manifest["ncx"] ?: ""

            val fullTocPath = if(basePath.isEmpty()) tocPath else "$basePath/$tocPath"
            val tocReader = readers[fullTocPath]
            parser.setInput(tocReader)
            parser.nextTag()

            val toc = ncxParser.parse()
            toc.navPoints.forEach {
                tempBook.tableOfContents.add(it)
            }

            readSpineItemFiles(readers, tempBook)
            tempBook.currentChapter = currentChapter

            return@withContext tempBook
        }
    }

    suspend fun getChapterBodyTextFromPath(
        path: String,
        zipFileReader: ZipFileReader
    ): String{
        return withContext(Dispatchers.Default){
            val fullPath = if(basePath.isEmpty()) path else "$basePath/$path"
            currentFolder = File(fullPath).parent

            val chapterStream = zipFileReader.getFileStream(fullPath)
            parser.setInput(chapterStream, null)
            parser.nextTag()
            parseChapterHtml(parser, true)
            return@withContext currentChapter
        }
    }

    private fun extractBasePath(fullPath: String){
        val segments = fullPath.split("/")
        basePath = if(segments.size > 1) segments.first() else "" // No base path, files are in root directory
    }

    /**
     *  Gets path of the .opf file which is in the full-path attribute of a rootfile tag .
     *  Tag hierarchy: container -> rootfiles -> rootfile
     *
     */
    private fun parseContainerFile(parser: XmlPullParser): String{

        parser.require(XmlPullParser.START_TAG, ns, XML_ELEMENT_CONTAINER)

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if(parser.name == XML_ELEMENT_ROOTFILE) return parser.getAttributeValue(null, XML_ATTRIBUTE_FULLPATH)
        }
        return ""
    }

    /**
     *  Get contents of body tag in html/xhtml
     *  Tags hierarchy:
     *  html -> head
     *          body
     */
    private fun parseChapterHtml(parser: XmlPullParser, isCurrentChapter: Boolean){
        parser.require(XmlPullParser.START_TAG, ns, "html")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if(parser.name == "body") readBodyTag(parser, isCurrentChapter)
            else skip(parser)
        }
    }

    private fun readBodyTag(parser: XmlPullParser, isCurrentChapter: Boolean) {
        val innerXml = getInnerXml(parser)
        chapterLength = formatHtml(innerXml).length
        if(isCurrentChapter) currentChapter = innerXml
    }

    /**
     * Format the raw xhtml text to get a more accurate length of the text.
     */
    private fun formatHtml(text: CharSequence): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(text.toString(), Html.FROM_HTML_MODE_COMPACT, null, null)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(text.toString(), null, null)
        }
    }


    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    /**
     *  Get tags, attributes and text inside a tag
     *  Taken from: https://stackoverflow.com/questions/16069425/xmlpullparser-get-inner-text-including-xml-tags
     */
    @VisibleForTesting
    @Throws(XmlPullParserException::class, IOException::class)
    fun getInnerXml(parser: XmlPullParser):String {
        val sb = StringBuilder()
        var depth = 1
        var lastTagWasStart = false
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> {
                    depth--
                    if (depth > 0) {
                        if(lastTagWasStart) sb.append("/>") else sb.append("</" + parser.name + ">")
                    }
                    lastTagWasStart = false
                }
                XmlPullParser.START_TAG -> {
                    if(lastTagWasStart) sb.append(">")
                    lastTagWasStart = true
                    depth++
                    val attrs = StringBuilder()

                    when(parser.name){
                        "img" -> {
                            attrs.append(" ")
                            for (i in 0 until parser.attributeCount){
                                val attrName = parser.getAttributeName(i)

                                val rawValue = parser.getAttributeValue(i)
                                val attrValue = if(attrName == "src") pathToAbsolute(rawValue) else rawValue

                                attrs.append("$attrName=\"$attrValue\" ")
                            }

                            sb.append("<" + parser.name + attrs.toString())
                        }
                        "svg" -> {
                            replaceSvgTag(parser, sb)
                            lastTagWasStart = false
                            depth--
                        }
                        else -> sb.append("<" + parser.name + attrs.toString())
                    }
                }
                else -> {
                    if(lastTagWasStart) sb.append(">")
                    sb.append(parser.text)
                    lastTagWasStart = false
                }
            }
        }
        return sb.toString()
    }

    private fun replaceSvgTag(parser: XmlPullParser, sb: StringBuilder){
        var depth = 1
        var tagAttrs: String? = null

        while (depth != 0) {
            when (parser.next()) {

                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> {
                    depth++

                    when(parser.name){
                        "image" -> tagAttrs = parser.getAttributeValue(null, "xlink:href")
                    }
                }
            }
        }

        if(tagAttrs != null) {
            val fullPath = pathToAbsolute(tagAttrs)
            sb.append("<img src=\"$fullPath\"/>")
        }
    }

    private fun pathToAbsolute(path: String): String{
        return  File(currentFolder, path).canonicalPath
    }

    private suspend fun readSpineItemFiles(readers: Map<String, StringReader>, book: Book) = coroutineScope{

        book.spine.mapIndexed { index, item ->
            val href = item.href
            val fullPath = if(basePath.isEmpty()) href else "$basePath/$href"
            currentFolder = File(fullPath).parent
            val reader = readers[fullPath]

            parser.setInput(reader)
            parser.nextTag()

            parseChapterHtml(parser, index == 0) // In this case the first item is the current

            item.charCount = chapterLength
        }
    }

    companion object {
        const val CONTAINER_FILE_PATH = "META-INF/container.xml"

        // Container XML
        const val XML_ELEMENT_CONTAINER = "container"
        const val XML_ELEMENT_ROOTFILE = "rootfile"
        const val XML_ATTRIBUTE_FULLPATH = "full-path"
    }
}