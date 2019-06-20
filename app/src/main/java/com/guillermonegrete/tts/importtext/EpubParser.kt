package com.guillermonegrete.tts.importtext

import org.apache.commons.io.IOUtils
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.*
import java.lang.StringBuilder
import java.util.zip.ZipInputStream

class EpubParser {

    private var basePath = ""

    private var opfPath = ""
    private var spine = mutableListOf<String>()
    private val manifest = mutableMapOf<String, String>()
    private var tocName = ""

    private var chapters = mutableListOf<String>()

    private val ns: String? = null

    fun parseBook(parser: XmlPullParser, inputStream: InputStream?): Book{
        // Create input stream that supports reset, so we can use it multiple times.
        val baos = ByteArrayOutputStream()
        IOUtils.copy(inputStream, baos)
        val bytes = baos.toByteArray()
        val byteStream = ByteArrayInputStream(bytes)

        val containerStream = getFileStreamFromZip(CONTAINER_FILE_PATH, byteStream)
        parser.setInput(containerStream, null)
        parser.nextTag()
        opfPath = parseContainerFile(parser)
        basePath = opfPath.split("/").first()


        byteStream.reset()
        val opfStream = getFileStreamFromZip(opfPath, byteStream)
        parser.setInput(opfStream, null)
        parser.nextTag()
        parseOpfFile(parser)


        if(spine.isNotEmpty()){
            val firstChapterPath = manifest[spine[1]]
            if(firstChapterPath != null) {
                val fullPath = "$basePath/$firstChapterPath"
                println("Fisrt chapter path: $fullPath")

                byteStream.reset()
                val stream = getFileStreamFromZip(fullPath, byteStream)
                if(stream != null) println(readZipEntry(stream))

                byteStream.reset()
                val chapterStream = getFileStreamFromZip(fullPath, byteStream)
                parser.setInput(chapterStream, null)
                parser.nextTag()
                parseChapterHtml(parser)
            }
        }
        return Book("Placeholder title", chapters)
    }


    fun printContentsFromZip(inputStream: InputStream?){
        var zipStream: ZipInputStream? = null

        val textBuilder = StringBuilder()
        try {
            zipStream = ZipInputStream(BufferedInputStream(inputStream))
            var zipEntry = zipStream.nextEntry

            while (zipEntry != null){
                textBuilder.append(zipEntry.name)
                textBuilder.append("\n")

                zipStream.closeEntry()
                zipEntry = zipStream.nextEntry
            }
        }catch (e: IOException){
            println("Error opening file: $e")
        }finally {
            zipStream?.close()
        }

        println(textBuilder.toString())

    }

    private fun getFileStreamFromZip(fileName: String, inputStream: InputStream?): InputStream?{
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

    // Taken from: https://stackoverflow.com/a/36310757/10244759
    private fun readZipEntry(zipInputStream: InputStream): String{
        val stringBuilder = StringBuilder()
        val buffer = ByteArray(1024)
        var read = zipInputStream.read(buffer, 0, buffer.size)
        while (read != -1){
            stringBuilder.append(String(buffer, 0, read))
            read = zipInputStream.read(buffer, 0, buffer.size)

        }
        return stringBuilder.toString()
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
     *  Gets path of table of contents file, manifest items and spine
     *  Tag hierarchy:
     *  package -> metadata
     *             manifest -> item (id. href, media-type)
     *             spine (toc) -> itemref (idref)
     */
    private fun parseOpfFile(parser: XmlPullParser){
        parser.require(XmlPullParser.START_TAG, ns, XML_ELEMENT_PACKAGE)
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when(parser.name){
                XML_ELEMENT_MANIFEST -> readManifest(parser)
                XML_ELEMENT_SPINE -> readSpine(parser)
                else -> skip(parser)
            }
        }
    }

    private fun readManifest(parser: XmlPullParser){
        parser.require(XmlPullParser.START_TAG, ns, XML_ELEMENT_MANIFEST)
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
        tocName = parser.getAttributeValue(null, XML_ATTRIBUTE_TOC)
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if(parser.name == XML_ELEMENT_ITEMREF) readSpineItem(parser)
            else skip(parser)
        }
    }

    private fun readSpineItem(parser: XmlPullParser) {
        parser.require(XmlPullParser.START_TAG, ns, XML_ELEMENT_ITEMREF)
        val idref = parser.getAttributeValue(null, XML_ATTRIBUTE_IDREF)
        parser.nextTag()
        spine.add(idref)
        parser.require(XmlPullParser.END_TAG, ns, XML_ELEMENT_ITEMREF)
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
        println("Parsing body tag")
        chapters.add(getInnerXml(parser))
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
    @Throws(XmlPullParserException::class, IOException::class)
    fun getInnerXml(parser: XmlPullParser):String {
        val sb = StringBuilder()
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> {
                    depth--
                    if (depth > 0) sb.append("</" + parser.name + ">")

                }
                XmlPullParser.START_TAG -> {
                    depth++
                    sb.append("<" + parser.name + ">")
                }
                else -> {
                    sb.append(parser.text)
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
        const val XML_ELEMENT_MANIFEST = "manifest"
        const val XML_ELEMENT_MANIFESTITEM = "item"
        const val XML_ELEMENT_SPINE = "spine"
        const val XML_ATTRIBUTE_TOC = "toc"
        const val XML_ELEMENT_ITEMREF = "itemref"
        const val XML_ATTRIBUTE_IDREF = "idref"
    }
}