package com.guillermonegrete.tts.importtext.visualize.epub

import com.guillermonegrete.tts.importtext.epub.NavPoint
import com.guillermonegrete.tts.importtext.epub.TableOfContents
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

class NCXParser(parser: XmlPullParser): PullParser(parser) {

    private val navPoints = mutableListOf<NavPoint>()

    fun parse(): TableOfContents{
        navPoints.clear()

        parser.require(XmlPullParser.START_TAG, ns, "ncx")
        while (parser.next() != XmlPullParser.END_TAG){
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if(parser.name == "navMap") readMapTag(parser)
            else skip()
        }
        return TableOfContents(navPoints)
    }

    private fun readMapTag(parser: XmlPullParser){
        parser.require(XmlPullParser.START_TAG, ns, "navMap")
        while (parser.next() != XmlPullParser.END_TAG){
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when(parser.name){
                "navPoint" -> readPoint(parser)
                else -> skip()
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
                else -> skip() // Don't read nested nav points, implement later
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
}