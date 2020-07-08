package com.guillermonegrete.tts.importtext.visualize

import android.util.Xml
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guillermonegrete.tts.InstMainCoroutineRule
import com.guillermonegrete.tts.importtext.epub.Book
import com.guillermonegrete.tts.importtext.epub.NavPoint
import com.guillermonegrete.tts.importtext.epub.SpineItem
import com.guillermonegrete.tts.importtext.epub.TableOfContents
import com.guillermonegrete.tts.importtext.visualize.epub.NCXParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.StringReader

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class EpubParserTest {

    private val xmlParser: XmlPullParser = Xml.newPullParser()
    private lateinit var zipFileReader: FakeZipFileReader

    private lateinit var epubParser: EpubParser

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = InstMainCoroutineRule()

    @Before
    fun setUp(){

        epubParser = EpubParser(xmlParser, Dispatchers.Unconfined)
        zipFileReader = FakeZipFileReader()

        xmlParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
    }

    @Test
    fun creates_book_object() = runBlockingTest {
        val contentStream = ByteArrayInputStream(CONTAINER_FILE_XML.toByteArray())
        zipFileReader.addFileStream(EpubParser.CONTAINER_FILE_PATH, contentStream)

        val opfStream = ByteArrayInputStream(OPF_FILE_XML.toByteArray())
        zipFileReader.addFileStream(opfPath, opfStream)

        val tocStream = ByteArrayInputStream(TABLE_OF_CONTENTS_XML.toByteArray())
        zipFileReader.addFileStream("$basePath/$tocPath", tocStream)

        chaptersXml.forEachIndexed { index, s ->
            val chapterStream = ByteArrayInputStream(s.toByteArray())
            zipFileReader.addFileStream(chapterRefs[index], chapterStream)
        }

        val book = epubParser.parseBook(zipFileReader)

        val expectedBook = Book("Hunger: Book One", chapterBodies.first(),
            listOf(
                SpineItem("coverpage-wrapper", "wrap0000.html", chapterBodies.first().length),
                SpineItem("item4", "18291-h@18291-h-0.htm.html", chapterBodies[1].length),
                SpineItem("item5", "18291-h@18291-h-1.htm.html", chapterBodies[2].length)
            ),
            mapOf("item1" to "pgepub.css", "item2" to "0.css", "item3" to "1.css", "item4" to "18291-h@18291-h-0.htm.html", "item5" to "18291-h@18291-h-1.htm.html", "ncx" to "toc.ncx", "item13" to "cover.png", "coverpage-wrapper" to "wrap0000.html"),
            TableOfContents(listOf(
            NavPoint("Volume 1", "volume1.html"),
            NavPoint("Chapter 1", "volume1/chapter001.html"),
            NavPoint("Chapter 2", "volume1/chapter002.html"),
            NavPoint("Section 1", "volume1/chapter002.html#Section_1"),
            NavPoint("Volume 2", "volume2.html")
        )))
        assertBook(expectedBook, book)
    }

    @Test
    fun gets_inner_html_with_self_closing_tags(){
        xmlParser.setInput(StringReader("<body><div src=\"path.jpg\"><br/><img src=\"path.jpg\"/>Text input<br/></div></body>"))
        xmlParser.nextTag()

        val result = epubParser.getInnerXml(xmlParser)
        val expected = "<div><br/><img src=\"/path.jpg\" />Text input<br/></div>"
        assertEquals(expected, result)
    }

    @Test
    fun parses_svg_tag() {
        xmlParser.setInput(StringReader("<body>" +
                "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"100%\" height=\"100%\" viewBox=\"0 0 1200 1600\" preserveAspectRatio=\"none\">" +
                "<image width=\"1200\" height=\"1600\" xlink:href=\"cover_image.jpg\"/>" +
                "</svg>" +
                "<p><span>start</span></p>" +
                "</body>"))
        xmlParser.nextTag()

        val result = epubParser.getInnerXml(xmlParser)

        val expected = """<img src="/cover_image.jpg"/><p><span>start</span></p>""".trimMargin()
        println("Result: $result, expected: $expected")
        assertEquals(expected, result)
    }

    @Test
    fun parse_table_of_contents(){
        xmlParser.setInput(StringReader(TABLE_OF_CONTENTS_XML))
        xmlParser.nextTag()

        val result = NCXParser(xmlParser).parse()
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

    private fun assertBook(expected: Book, actual: Book){
        assertEquals(expected.title, actual.title)
        assertEquals(expected.currentChapter, actual.currentChapter)
        assertEquals(expected.spine, actual.spine)
        assertEquals(expected.manifest, actual.manifest)
        assertTOC(expected.tableOfContents, actual.tableOfContents)
    }

    private fun assertTOC(expected: TableOfContents, actual: TableOfContents){
        assertEquals(expected.navPoints, actual.navPoints)
    }

    companion object{
        const val basePath = "18291"
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

        const val opfPath = "18291/content.opf"
        const val CONTAINER_FILE_XML =
            """<?xml version="1.0" encoding="UTF-8" ?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                  <rootfiles>
                    <rootfile full-path="$opfPath" media-type="application/oebps-package+xml"/>
                  </rootfiles>
                </container>"""
        const val tocPath = "toc.ncx"
        const val OPF_FILE_XML =
            """<package xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:opf="http://www.idpf.org/2007/opf" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.idpf.org/2007/opf" version="2.0" unique-identifier="id">
                  <metadata>
                    <dc:publisher>Project Gutenberg</dc:publisher>
                    <dc:rights>Public Domain in the USA.</dc:rights>
                    <dc:identifier id="id" opf:scheme="URI">http://www.gutenberg.org/ebooks/18291</dc:identifier>
                    <dc:creator opf:file-as="Hamsun, Knut">Knut Hamsun</dc:creator>
                    <dc:title>Hunger: Book One</dc:title>
                    <dc:language xsi:type="dcterms:RFC4646">en</dc:language>
                    <dc:date opf:event="conversion">2018-10-19T08:31:38.121210+00:00</dc:date>
                    <dc:source>18291-h/18291-h.htm</dc:source>
                    <meta content="item13" name="cover"/>
                  </metadata>
                  <manifest>
                    <item href="pgepub.css" id="item1" media-type="text/css"/>
                    <item href="0.css" id="item2" media-type="text/css"/>
                    <item href="1.css" id="item3" media-type="text/css"/>
                    <item href="18291-h@18291-h-0.htm.html" id="item4" media-type="application/xhtml+xml"/>
                    <item href="18291-h@18291-h-1.htm.html" id="item5" media-type="application/xhtml+xml"/>
                    <item href="$tocPath" id="ncx" media-type="application/x-dtbncx+xml"/>
                    <item href="cover.png" id="item13" media-type="image/png"/>
                    <item href="wrap0000.html" id="coverpage-wrapper" media-type="application/xhtml+xml"/>
                  </manifest>
                  <spine toc="ncx">
                    <itemref idref="coverpage-wrapper" linear="no"/>
                    <opf:itemref idref="id_75ee4" linear="yes"/><itemref idref="item4" linear="yes"/>
                    <itemref idref="item5" linear="yes"/>
                  </spine>
                  <guide>
                    <reference href="wrap0000.html" type="cover" title="Cover"/>
                  </guide>
                </package>"""
        private const val CHAPTER_XML_TEMPLATE = """<?xml version='1.0' encoding='utf-8'?>
                <!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.1//EN' 'http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd'>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head>
                    <meta name="generator" content="HTML Tidy for HTML5 for Linux version 5.2.0"/>
                    <meta http-equiv="Content-Type" content="application/xhtml+xml; charset=utf-8"/>
                    <title>The Project Gutenberg eBook</title>
                    
                    
                    <link href="0.css" type="text/css" rel="stylesheet"/>
                    <link href="1.css" type="text/css" rel="stylesheet"/>
                    <link href="pgepub.css" type="text/css" rel="stylesheet"/>
                    <meta content="EpubMaker 0.3.26 &lt;https://github.com/gitenberg-dev/pg-epubmaker&gt;" name="generator"/>
                </head>
                <body dir="rtl" xml:lang="he">{}</body></html>"""
        private val chapterBodies = listOf(
            "Text input",
            "Chapter number 2",
            "Chapter with a longer character length"
        )
        private val chaptersXml  = chapterBodies.map { CHAPTER_XML_TEMPLATE.replace("{}", it) }
        private val chapterRefs = listOf(
            "18291/wrap0000.html",
            "18291/18291-h@18291-h-0.htm.html",
            "18291/18291-h@18291-h-1.htm.html"
        )

    }
}