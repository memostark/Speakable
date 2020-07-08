package com.guillermonegrete.tts.importtext.visualize.epub

import com.guillermonegrete.tts.importtext.epub.Book
import com.guillermonegrete.tts.importtext.epub.EPUBMetadata
import com.guillermonegrete.tts.importtext.epub.SpineItem
import org.xmlpull.v1.XmlPullParser

class OPFParser(parser: XmlPullParser): PullParser(parser) {

    private var title = "Untitled file"

    private val manifest = mutableMapOf<String, String>()
    private val spineIdRefs = mutableListOf<SpineItem>()

    /**
     *  Gets path of table of contents file, manifest items and spine
     *  Tag hierarchy:
     *  package -> metadata -> dc:title
     *             manifest -> item (id, href, media-type)
     *             spine (toc) -> itemref (idref)
     */
    fun parse(): Book{
        parser.require(XmlPullParser.START_TAG, ns, XML_ELEMENT_PACKAGE)

        var metadata: EPUBMetadata? = null

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when(parser.name){
                XML_ELEMENT_METADATA -> metadata = readMetadata()
                XML_ELEMENT_MANIFEST -> readManifest(parser)
                XML_ELEMENT_SPINE -> readSpine(parser)
                else -> skip()
            }
        }

        return Book(
            metadata ?: EPUBMetadata("", "", "", ""),
            "",
            spineIdRefs,
            manifest
        )
    }

    private fun readMetadata(): EPUBMetadata {
        parser.require(XmlPullParser.START_TAG, ns, XML_ELEMENT_METADATA)

        var title = ""; var author = ""; var language = ""; var cover = ""

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue

            when(parser.name){
                XML_ELEMENT_DCTITLE -> title = readTagText(XML_ELEMENT_DCTITLE)
                XML_ELEMENT_CREATOR -> author = readTagText(XML_ELEMENT_CREATOR)
                XML_ELEMENT_LANGUAGE -> language = readTagText(XML_ELEMENT_LANGUAGE)
                XML_ELEMENT_META -> cover = readMetaItem("cover")
                else -> skip()
            }
        }

        return EPUBMetadata(title, author, language, cover)
    }

    private fun readMetaItem(name: String): String{
        parser.require(XmlPullParser.START_TAG, ns, XML_ELEMENT_META)

        val id = parser.getAttributeValue(null, "name")

        return if(id == name){
            val content = parser.getAttributeValue(null, "content")
            parser.nextTag()
            parser.require(XmlPullParser.END_TAG, ns, XML_ELEMENT_META)
            content

        }else{
            skip()
            ""
        }
    }

    private fun readManifest(parser: XmlPullParser){
        parser.require(XmlPullParser.START_TAG, ns, XML_ELEMENT_MANIFEST)

        manifest.clear()

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if(parser.name == XML_ELEMENT_MANIFESTITEM) readManifestItem(parser)
            else skip()
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

        spineIdRefs.clear()

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if(parser.name == XML_ELEMENT_ITEMREF) readSpineItem(parser)
            else skip()
        }
    }

    private fun readSpineItem(parser: XmlPullParser) {
        parser.require(XmlPullParser.START_TAG, ns, XML_ELEMENT_ITEMREF)
        val idRef = parser.getAttributeValue(null, XML_ATTRIBUTE_IDREF)

        parser.nextTag()

        val href = manifest[idRef] ?: ""
        spineIdRefs.add(SpineItem(idRef, href))
        parser.require(XmlPullParser.END_TAG, ns, XML_ELEMENT_ITEMREF)
    }

    companion object{
        const val XML_ELEMENT_PACKAGE = "package"
        const val XML_ELEMENT_METADATA = "metadata"

        const val XML_ELEMENT_DCTITLE = "dc:title"
        const val XML_ELEMENT_CREATOR = "dc:creator"
        const val XML_ELEMENT_LANGUAGE = "dc:language"
        const val XML_ELEMENT_META = "meta"

        const val XML_ELEMENT_MANIFEST = "manifest"
        const val XML_ELEMENT_MANIFESTITEM = "item"
        const val XML_ELEMENT_SPINE = "spine"
        const val XML_ELEMENT_ITEMREF = "itemref"
        const val XML_ATTRIBUTE_IDREF = "idref"
    }
}