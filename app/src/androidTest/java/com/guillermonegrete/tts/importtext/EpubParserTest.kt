package com.guillermonegrete.tts.importtext

import android.util.Xml
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guillermonegrete.tts.importtext.epub.NavPoint
import com.guillermonegrete.tts.importtext.epub.TableOfContents
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

    @Test
    fun parse_table_of_contents(){
        xmlParser.setInput(StringReader(TABLE_OF_CONTENTS_XML))
        xmlParser.nextTag()

        val result = epubParser.parseTableOfContents(xmlParser)
        val expected = TableOfContents(listOf(
            NavPoint("Volume 1", "volume1.html"),
            NavPoint("Chapter 1", "volume1/chapter001.html"),
            NavPoint("Chapter 2", "volume1/chapter002.html"),
            NavPoint("Section 1", "volume1/chapter002.html#Section_1"),
            NavPoint("Volume 2", "volume2.html")
        ))

        println("Assert $expected, $result")
        assertTOC(expected, result)

    }

    private fun assertTOC(expected: TableOfContents, actual: TableOfContents){
        assertEquals(expected.navPoints, actual.navPoints)
    }

    companion object{
        const val TABLE_OF_CONTENTS_XML =
                """<ncx>
                        <head>
                            <meta name="dtb:uid" content="ae60509a-b048-5f93-abd0-5333f347e4c1"/>
                            <meta name="dtb:depth" content="3"/>
                            <meta name="dtb:totalPageCount" content="0"/>
                            <meta name="dtb:maxPageNumber" content="0"/>
                        </head>
                        <docTitle><text>Tax Guide</text></docTitle>
                        <docAuthor><text>IRS</text></docAuthor>
                        <navMap>
                            <navPoint id="116f4d31" playOrder="1">
                                <navLabel><text>Volume 1</text></navLabel>
                                <content src="volume1.html"/>
                                <navPoint id="1563d3d9-33c5-472e-bcf4-587923f3137b" playOrder="2">
                                    <navLabel><text>Chapter 1</text></navLabel>
                                    <content src="volume1/chapter001.html"/>
                                </navPoint>
                                <navPoint id="1563d3d9" playOrder="3">
                                    <navLabel><text>Chapter 2</text></navLabel>
                                    <content src="volume1/chapter002.html"/>
                                    <navPoint id="1563d3d9" playOrder="4">
                                        <navLabel><text>Section 1</text></navLabel>
                                        <content src="volume1/chapter002.html#Section_1"/>
                                    </navPoint>
                                </navPoint>
                            </navPoint>
                            <navPoint id="1563d3d9" playOrder="5">
                                <navLabel><text>Volume 2</text></navLabel>
                                <content src="volume2.html"/>
                            </navPoint>
                        </navMap>
                    </ncx>"""
    }
}