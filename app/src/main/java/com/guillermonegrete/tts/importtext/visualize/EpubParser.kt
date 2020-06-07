package com.guillermonegrete.tts.importtext.visualize

import android.os.Build
import android.text.Html
import android.text.Spanned
import androidx.annotation.VisibleForTesting
import com.guillermonegrete.tts.importtext.epub.Book
import com.guillermonegrete.tts.importtext.epub.NavPoint
import com.guillermonegrete.tts.importtext.epub.SpineItem
import com.guillermonegrete.tts.importtext.epub.TableOfContents
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

    private var opfPath = ""
    private val spineIdRefs = mutableListOf<String>()
    private var chapterLength = 0
    private val manifest = mutableMapOf<String, String>()
    private var tocPath = ""

    private var currentFolder = ""
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
        return withContext(defaultDispatcher){
            val readers = zipFileReader.getAllReaders()

            val containerReader = readers[CONTAINER_FILE_PATH]
            parser.setInput(containerReader)
            parser.nextTag()
            opfPath = parseContainerFile(parser)
            extractBasePath(opfPath)

            val opfReader = readers[opfPath]
            parser.setInput(opfReader)
            parser.nextTag()
            parseOpfFile(parser)

            val fullTocPath = if(basePath.isEmpty()) tocPath else "$basePath/$tocPath"
            val tocReader = readers[fullTocPath]
            parser.setInput(tocReader)
            parser.nextTag()
            val toc = parseTableOfContents(parser)

            val spine = readSpineItemFiles(readers)

            return@withContext Book(title, currentChapter, spine, manifest, toc)
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
        parser.require(XmlPullParser.START_TAG, ns, XML_ELEMENT_MANIFEST)

        manifest.clear()

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if(parser.name == XML_ELEMENT_MANIFESTITEM) readManifestItem(parser)
            else skip(parser)
        }
    }

    private fun readManifestItem(parser: XmlPullParser){
        parser.require(XmlPullParser.START_TAG, ns, XML_ELEMENT_MANIFESTITEM)

        val id = parser.getAttributeValue(null, "id")
        val href = parser.getAttributeValue(null, "href")
        parser.nextTag()
        manifest[id] = href
        parser.require(XmlPullParser.END_TAG, ns, XML_ELEMENT_MANIFESTITEM)
    }

    private fun readSpine(parser: XmlPullParser) {
        parser.require(XmlPullParser.START_TAG, ns, XML_ELEMENT_SPINE)

        val tocId = parser.getAttributeValue(null, XML_ATTRIBUTE_TOC)
        tocPath = manifest[tocId] ?: ""

        spineIdRefs.clear()

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if(parser.name == XML_ELEMENT_ITEMREF) readSpineItem(parser)
            else skip(parser)
        }
    }

    private fun readSpineItem(parser: XmlPullParser) {
        parser.require(XmlPullParser.START_TAG, ns, XML_ELEMENT_ITEMREF)
        val idRef = parser.getAttributeValue(null, XML_ATTRIBUTE_IDREF)

        parser.nextTag()
        spineIdRefs.add(idRef)
        parser.require(XmlPullParser.END_TAG, ns, XML_ELEMENT_ITEMREF)
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

    private suspend fun readSpineItemFiles(readers: Map<String, StringReader>) = coroutineScope{

        spineIdRefs.mapIndexed { index, idRef ->
            val href = manifest[idRef] ?: ""
            val fullPath = if(basePath.isEmpty()) href else "$basePath/$href"
            currentFolder = File(fullPath).parent
            val reader = readers[fullPath]

            parser.setInput(reader)
            parser.nextTag()

            parseChapterHtml(parser, index == 0) // In this case the first item is the current

            SpineItem(idRef, href, chapterLength)
        }
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