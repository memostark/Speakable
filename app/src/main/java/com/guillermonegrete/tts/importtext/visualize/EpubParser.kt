package com.guillermonegrete.tts.importtext.visualize

import androidx.annotation.VisibleForTesting
import com.guillermonegrete.tts.importtext.epub.Book
import com.guillermonegrete.tts.importtext.epub.NavPoint
import com.guillermonegrete.tts.importtext.epub.TableOfContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.*
import java.lang.StringBuilder
import java.util.zip.ZipInputStream
import javax.inject.Inject

/**
 * Based on this project: https://www.codeproject.com/Articles/592909/EPUB-Viewer-for-Android-with-Text-to-Speech
 */
class EpubParser @Inject constructor(private val parser: XmlPullParser) {

    var basePath = ""
        private set

    private var opfPath = ""
    private var spine = mutableListOf<String>()
    private val manifest = mutableMapOf<String, String>()
    private var tocPath = ""

    private var currentChapter = ""

    private var title = "Untitled file"

    private val navPoints = mutableListOf<NavPoint>()

    private val ns: String? = null

    init {
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
    }

    suspend fun parseBook(
        zipFileReader: ZipFileReader
    ): Book {
        // Create input stream that supports reset, so we can use it multiple times.
        return withContext(Dispatchers.IO){
            val containerStream = zipFileReader.getFileStream(CONTAINER_FILE_PATH)
            parser.setInput(containerStream, null)
            parser.nextTag()
            opfPath = parseContainerFile(parser)
            extractBasePath(opfPath)


            val opfStream = zipFileReader.getFileStream(opfPath)
            parser.setInput(opfStream, null)
            parser.nextTag()
            parseOpfFile(parser)

            val fullTocPath = if(basePath.isEmpty()) tocPath else "$basePath/$tocPath"
            val tocStream = zipFileReader.getFileStream(fullTocPath)
            parser.setInput(tocStream, null)
            parser.nextTag()
            val toc = parseTableOfContents(parser)


            if(spine.isNotEmpty()){
                val firstChapterPath = manifest[spine.first()]
                if(firstChapterPath != null) {
                    val fullPath = if(basePath.isEmpty()) firstChapterPath else "$basePath/$firstChapterPath"
                    println("Chapter path: $fullPath")

                    val chapterStream = zipFileReader.getFileStream(fullPath)
                    parser.setInput(chapterStream, null)
                    parser.nextTag()
                    parseChapterHtml(parser)
                }
            }
            return@withContext Book(title, currentChapter, spine, manifest, toc)
        }
    }

    fun getChapterBodyTextFromPath(
        path: String,
        zipFileReader: ZipFileReader
    ): String{
        val fullPath = if(basePath.isEmpty()) path else "$basePath/$path"
        val chapterStream = zipFileReader.getFileStream(fullPath)
        parser.setInput(chapterStream, null)
        parser.nextTag()
        parseChapterHtml(parser)
        return currentChapter
    }

    private fun extractBasePath(fullPath: String){
        val segments = fullPath.split("/")
        basePath = if(segments.size > 1) segments.first() else "" // No base path, files are in root directory
    }

    private fun getFileStreamFromZip(fileName: String, inputStream: InputStream): InputStream?{
        val zipStream: ZipInputStream?

        try {
            zipStream = ZipInputStream(BufferedInputStream(inputStream))
            var zipEntry = zipStream.nextEntry

            while (zipEntry != null){
                if(fileName == zipEntry.name) return zipStream

                zipStream.closeEntry()
                zipEntry = zipStream.nextEntry
            }
        }catch (e: IOException){
            println("Error opening file $fileName: $e")
        }
        return null
    }

    /**
     *  Gets path of the .opf file which is in the full-path attribute of a rootfile tag .
     *  Tag hierarchy: container -> rootfiles -> rootfile
     *
     */
    private fun parseContainerFile(parser: XmlPullParser): String{

        parser.require(XmlPullParser.START_TAG, ns,
            XML_ELEMENT_CONTAINER
        )
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if(parser.name == XML_ELEMENT_ROOTFILE) return parser.getAttributeValue(null,
                XML_ATTRIBUTE_FULLPATH
            )
        }
        return ""
    }

    /**
     *  Gets path of table of contents file, manifest items and spine
     *  Tag hierarchy:
     *  package -> metadata -> dc:title
     *             manifest -> item (id, href, media-type)
     *             spine (toc) -> itemref (idref)
     */
    private fun parseOpfFile(parser: XmlPullParser){
        parser.require(XmlPullParser.START_TAG, ns,
            XML_ELEMENT_PACKAGE
        )
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when(parser.name){
                XML_ELEMENT_METADATA -> readMetadata()
                XML_ELEMENT_MANIFEST -> readManifest(parser)
                XML_ELEMENT_SPINE -> readSpine(parser)
                else -> skip(parser)
            }
        }
    }

    private fun readMetadata() {
        parser.require(XmlPullParser.START_TAG, ns,
            XML_ELEMENT_METADATA
        )
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if(parser.name == XML_ELEMENT_DCTITLE) readTitle()
            else skip(parser)
        }
    }

    /**
     * Reads text inside dc:title tag
     * Example tag: <dc:title>Hunger: Book One</dc:title>
     */
    private fun readTitle() {
        parser.require(XmlPullParser.START_TAG, ns,
            XML_ELEMENT_DCTITLE
        )
        title = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns,
            XML_ELEMENT_DCTITLE
        )
    }

    private fun readManifest(parser: XmlPullParser){
        parser.require(XmlPullParser.START_TAG, ns,
            XML_ELEMENT_MANIFEST
        )
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if(parser.name == XML_ELEMENT_MANIFESTITEM) readManifestItem(parser)
            else skip(parser)
        }
    }

    private fun readManifestItem(parser: XmlPullParser){
        parser.require(XmlPullParser.START_TAG, ns,
            XML_ELEMENT_MANIFESTITEM
        )
        val id = parser.getAttributeValue(null, "id")
        val href = parser.getAttributeValue(null, "href")
        parser.nextTag()
        manifest[id] = href
        parser.require(XmlPullParser.END_TAG, ns,
            XML_ELEMENT_MANIFESTITEM
        )
    }

    private fun readSpine(parser: XmlPullParser) {
        parser.require(XmlPullParser.START_TAG, ns,
            XML_ELEMENT_SPINE
        )
        val tocId = parser.getAttributeValue(null,
            XML_ATTRIBUTE_TOC
        )
        tocPath = manifest[tocId] ?: ""
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if(parser.name == XML_ELEMENT_ITEMREF) readSpineItem(parser)
            else skip(parser)
        }
    }

    private fun readSpineItem(parser: XmlPullParser) {
        parser.require(XmlPullParser.START_TAG, ns,
            XML_ELEMENT_ITEMREF
        )
        val idref = parser.getAttributeValue(null,
            XML_ATTRIBUTE_IDREF
        )
        parser.nextTag()
        spine.add(idref)
        parser.require(XmlPullParser.END_TAG, ns,
            XML_ELEMENT_ITEMREF
        )
    }

    /**
     *  Get contents of body tag in html/xhtml
     *  Tags heirarchy:
     *  html -> head
     *          body
     */
    private fun parseChapterHtml(parser: XmlPullParser){
        parser.require(XmlPullParser.START_TAG, ns, "html")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if(parser.name == "body") readBodyTag(parser)
            else skip(parser)
        }
    }

    private fun readBodyTag(parser: XmlPullParser) {
        currentChapter = getInnerXml(parser)
    }


    @VisibleForTesting
    @Throws(XmlPullParserException::class, IOException::class)
    fun parseTableOfContents(parser: XmlPullParser): TableOfContents{
        navPoints.clear()

        parser.require(XmlPullParser.START_TAG, ns, "ncx")
        while (parser.next() != XmlPullParser.END_TAG){
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if(parser.name == "navMap") readMapTag(parser)
            else skip(parser)
        }
        return TableOfContents(navPoints)
    }

    private fun readMapTag(parser: XmlPullParser){
        parser.require(XmlPullParser.START_TAG, ns, "navMap")
        while (parser.next() != XmlPullParser.END_TAG){
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when(parser.name){
                "navPoint" -> readPoint(parser)
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, "navMap")
    }

    private fun readPoint(parser: XmlPullParser){
        parser.require(XmlPullParser.START_TAG, ns, "navPoint")

        parser.nextTag()
        val label = readLabel(parser)
        parser.nextTag()
        parser.require(XmlPullParser.START_TAG, ns, "content")
        val content = parser.getAttributeValue(null, "src")
        parser.nextTag()
        parser.require(XmlPullParser.END_TAG, ns, "content")
        navPoints.add(NavPoint(label, content))

        while (parser.next() != XmlPullParser.END_TAG){
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when(parser.name){
                "navPoint" -> readPoint(parser)
                else -> skip(parser) // Don't read nested nav points, implement later
            }
        }

        parser.require(XmlPullParser.END_TAG, ns, "navPoint")
    }

    private fun readLabel(parser: XmlPullParser): String{
        parser.require(XmlPullParser.START_TAG, ns, "navLabel")
        parser.nextTag()
        parser.require(XmlPullParser.START_TAG, ns, "text")
        val result = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "text")
        parser.nextTag()
        parser.require(XmlPullParser.END_TAG, ns, "navLabel")
        return result
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
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
                    if(parser.name == "img"){
                        attrs.append(" ")
                        for (i in 0 until parser.attributeCount){
                            attrs.append(
                                parser.getAttributeName(i) + "=\""
                                + parser.getAttributeValue(i) + "\" "
                            )
                        }
                    }
                    sb.append("<" + parser.name + attrs.toString())
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

    companion object {
        const val CONTAINER_FILE_PATH = "META-INF/container.xml"

        // Container XML
        const val XML_ELEMENT_CONTAINER = "container"
        const val XML_ELEMENT_ROOTFILE = "rootfile"
        const val XML_ATTRIBUTE_FULLPATH = "full-path"

        // .opf XML
        const val XML_ELEMENT_PACKAGE = "package"
        const val XML_ELEMENT_METADATA = "metadata"
        const val XML_ELEMENT_DCTITLE = "dc:title"
        const val XML_ELEMENT_MANIFEST = "manifest"
        const val XML_ELEMENT_MANIFESTITEM = "item"
        const val XML_ELEMENT_SPINE = "spine"
        const val XML_ATTRIBUTE_TOC = "toc"
        const val XML_ELEMENT_ITEMREF = "itemref"
        const val XML_ATTRIBUTE_IDREF = "idref"
    }
}