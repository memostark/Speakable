package com.guillermonegrete.tts.importtext.visualize.epub

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

abstract class PullParser(val parser: XmlPullParser) {

    protected val ns: String? = null

    @Throws(IOException::class, XmlPullParserException::class)
    protected fun readTagText(tag: String): String {
        parser.require(XmlPullParser.START_TAG, ns, tag)

        val result = readText()
        parser.require(XmlPullParser.END_TAG, ns, tag)

        return result
    }

    @Throws(IOException::class, XmlPullParserException::class)
    protected fun readText(): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    @Throws(XmlPullParserException::class, IOException::class)
    protected fun skip() {
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
}