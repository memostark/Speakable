package com.guillermonegrete.tts.importtext

import android.util.Xml
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

@RunWith(AndroidJUnit4::class)
class EpubParserTest {

    private val xmlParser: XmlPullParser = Xml.newPullParser()
    private lateinit var epubParser: EpubParser

    @Before
    fun setUp(){
        epubParser = EpubParser()

        xmlParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
    }

    @Test
    fun gets_inner_html_with_self_closing_tags(){
        xmlParser.setInput(StringReader("<body><div><br/>Text input<br/></div></body>"))
        xmlParser.nextTag()

        val result = epubParser.getInnerXml(xmlParser)
        val expected = "<div><br/>Text input<br/></div>"
        assertEquals(expected, result)
    }
}