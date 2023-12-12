package com.guillermonegrete.tts.webreader

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.data.source.remote.GoogleTranslateResponse
import com.guillermonegrete.tts.data.source.remote.Sentence
import com.guillermonegrete.tts.db.WebLink
import com.guillermonegrete.tts.db.WebLinkDAO
import com.guillermonegrete.tts.di.TestApplicationModuleBinds
import com.guillermonegrete.tts.launchFragmentInHiltContainer
import com.guillermonegrete.tts.ui.theme.YellowNoteHighlight
import com.guillermonegrete.tts.utils.EspressoIdlingResource
import com.guillermonegrete.tts.utils.atPosition
import com.guillermonegrete.tts.utils.clickIn
import com.guillermonegrete.tts.utils.clickPercent
import com.guillermonegrete.tts.utils.hasNoBackgroundSpan
import com.guillermonegrete.tts.utils.withBackgroundSpan
import com.guillermonegrete.tts.webreader.db.Note
import com.guillermonegrete.tts.webreader.db.NoteDAO
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.QueueDispatcher
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import javax.inject.Inject


@MediumTest
@RunWith(AndroidJUnit4::class)
@UninstallModules(TestApplicationModuleBinds::class)
@HiltAndroidTest
class WebReaderFragmentTest{

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val externalFilesPath = context.getExternalFilesDir(null)?.absolutePath.toString()

    private lateinit var server: MockWebServer

    private val moshi: Moshi = Moshi.Builder().build()
    private val responseAdapter: JsonAdapter<GoogleTranslateResponse> = moshi.adapter(GoogleTranslateResponse::class.java)

    @Inject
    lateinit var linkDAO: WebLinkDAO

    @Inject
    lateinit var noteDAO: NoteDAO

    @Before
    fun setup(){
        server = MockWebServer()
        server.start(8081)
        hiltRule.inject()
    }

    @After
    fun teardown(){
        server.shutdown()
    }

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    // region Remote web page tests

    @Test
    fun when_word_tapped_then_translation_shown(){
        setRemotePage()

        onView(withId(R.id.paragraphs_list)).check(matches(isDisplayed()))

        setFirstWordResponse()

        tapParagraphItemStart(0)

        composeTestRule.onNodeWithTag("web_reader_bar").assertIsNotDisplayed()

        onView(withId(R.id.translated_text)).check(matches(isDisplayed()))
        onView(withId(R.id.translated_text)).check(matches(withText("My")))
        // Add note button is not visible because the page is not saved
        onView(withId(R.id.add_note_btn)).check(matches(not(isDisplayed())))

        showWebBottomSheet()
    }

    @Test
    fun when_sentence_double_tapped_then_highlight_and_navigate(){
        setRemotePage()

        onView(withId(R.id.paragraphs_list)).check(matches(isDisplayed()))

        // Highlight first sentence
        clickParagraphList(0, doubleClick())

        server.dispatcher = sentenceDispatcher()

        // Translate selection then navigate to the next one, until all the sentences are translated
        for (response in sentencesTranslationResponses){
            val translation = response.sentences.first().trans
            translateSelectionAndReturn(translation)
            composeTestRule.onNodeWithContentDescription("next selection").performClick()
        }

        // Verify the last next_selection click didn't change the selection (the last sentence was selected so no change)
        val translation = sentencesTranslationResponses.last().sentences.first().trans
        translateSelectionAndReturn(translation)

        // Verify previous_selection works
        composeTestRule.onNodeWithContentDescription("previous selection").performClick()
        val secondTranslation = sentencesTranslationResponses[1].sentences.first().trans
        translateSelectionAndReturn(secondTranslation)
    }

    @Test
    fun when_sentence_swiped_then_show_expanded_item(){
        setRemotePage()

        onView(withId(R.id.paragraphs_list)).check(matches(isDisplayed()))

        // Swipe paragraph and verify it expanded
        clickParagraphList(1, swipeRight())

        onView(withId(R.id.toggle_paragraph)).check(matches(isDisplayed()))

        // Click translate and verify correct translation
        server.enqueue(MockResponse().setBody(responseAdapter.toJson(paragraphTranslationResponse)))
        composeTestRule.onNodeWithContentDescription("Translate").performClick()

        onView(withId(R.id.translated_paragraph)).check(matches(isDisplayed()))
        onView(withId(R.id.translated_paragraph)).check(matches(withText(FIRST_PARAGRAPH_TRANS)))

        // Unselect paragraph and verify back to normal
        onView(withId(R.id.toggle_paragraph)).perform(click())
        onView(withId(R.id.toggle_paragraph)).check(doesNotExist())
    }

    @Test
    fun given_sentence_selected_when_word_tapped_then_big_sheet_shown(){
        setRemotePage()

        // Highlight first sentence
        clickParagraphList(0, doubleClick())

        server.dispatcher = sentenceDispatcher()

        // Translate
        composeTestRule.onNodeWithContentDescription("Translate").performClick()
        Thread.sleep(500)

        // Tap first word
        server.dispatcher = QueueDispatcher()
        setFirstWordResponse()
        tapParagraphItemStart(0)

        // Verify sheet is correct
        onView(withId(R.id.word_translation)).check(matches(isDisplayed()))
        onView(withId(R.id.word_translation)).check(matches(withText("My")))
        // Add note button is not visible because the page is not saved
        onView(withId(R.id.add_note_btn)).check(matches(not(isDisplayed())))
    }

    // endregion

    // region Local page tests

    @Test
    fun given_local_page_when_word_tapped_and_note_button_clicked_then_note_saved() {
        setLocalPage()

        val response = GoogleTranslateResponse(listOf(Sentence("My", "Mi")), "es")
        val jsonResponse = responseAdapter.toJson(response)
        server.enqueue(MockResponse().setBody(jsonResponse))

        tapParagraphItemStart(0)

        onView(withId(R.id.add_note_btn)).perform(click())

        composeTestRule.onNodeWithText("Save").performClick()
        // Verify highlight was added to word "Mi" at the start of the paragraph
        val color = YellowNoteHighlight.toArgb()
        onView(withId(R.id.paragraphs_list))
            .check(matches(atPosition(0, withBackgroundSpan(color, 0 , 2))))
    }

    @Test
    fun given_saved_note_when_edited_sheet_updated() {
        setLocalPage(language = "en", initialNote = DEFAULT_NOTE)

        // Click note and open note dialog
        tapParagraphItemStart(1)
        onView(withId(R.id.add_note_btn)).perform(click())

        // Modify the text, color and save
        // performTextInput() is inconsistent, sometimes adds the text at the end and other times it replaces text so use this method instead
        composeTestRule.onNodeWithTag(NOTE_TEXT_TAG).performTextReplacement("note text new")
        val colorBtnPos = 1
        composeTestRule.onNodeWithTag(colorBtnPos.toString()).performClick()
        composeTestRule.onNodeWithTag(ACCEPT_BTN_TAG).performClick()

        // Verify bottom sheet note text changed
        onView(withId(R.id.translated_text)).check(matches(isDisplayed()))
        onView(withId(R.id.translated_text)).check(matches(withText("note text new")))

        // Verify highlight color for the note changed
        val color = COLORS[colorBtnPos].toArgb()
        onView(withId(R.id.paragraphs_list))
            .check(matches(atPosition(1, withBackgroundSpan(color, 0 , 2))))

        showWebBottomSheet()
    }

    @Test
    fun given_saved_note_when_delete_sheet_updated() {
        // Initial data with saved link and note at the start of the 1st paragraph
        val note = Note("note text", "original text", 37, 5, YellowNoteHighlight.toHex(), DEFAULT_LINK_ID)
        setLocalPage(initialNote = note)

        // Click note
        tapParagraphItemStart(1)
        // Text is a sentence so this shouldn't be visible
        onView(withId(R.id.more_info_btn)).check(matches(not(isDisplayed())))
        // Open note dialog
        onView(withId(R.id.add_note_btn)).perform(click())

        // Delete note
        composeTestRule.onNodeWithTag(DELETE_BTN_TAG).performClick()
        Thread.sleep(300) // wait for the sheet to hide

        // Verify note removed
        onView(withId(R.id.paragraphs_list))
            .check(matches(atPosition(1, hasNoBackgroundSpan())))
        // Verify sheet hidden removed
        onView(withId(R.id.add_note_btn)).check(matches(not(isDisplayed())))
    }

    @Test
    fun given_selected_sentence_when_word_and_note_clicked_then_sheet_updated(){
        setLocalPage(language = "en", initialNote = DEFAULT_NOTE)

        server.dispatcher = sentenceDispatcher()

        // For this test the 2nd paragraph will be used
        val listPos = 1
        // Highlight sentence
        clickParagraphList(listPos, doubleClick())

        composeTestRule.onNodeWithContentDescription("Translate").performClick()
        clickParagraphList(listPos, clickPercent(0.9f, 0f))
        Thread.sleep(500)

        onView(withId(R.id.word_translation)).check(matches(isDisplayed()))
        onView(withId(R.id.word_translation)).check(matches(withText("parrafo")))

        // Add note
        onView(withId(R.id.add_word_note_btn)).perform(click())
        updateWordNote("New note text", 1, 10, 19, listPos)

        // Show more info
        onView(withId(R.id.more_info_word_btn)).perform(click())
        onView(withId(R.id.info_webview)).check(matches(isDisplayed()))

        // Close web view sheet and translation sheet
        Espresso.pressBack()
        Espresso.pressBack()
        Thread.sleep(500)

        composeTestRule.onNodeWithContentDescription("Translate").performClick()
        Thread.sleep(500)

        // Tap note
        tapParagraphItemStart(listPos)

        // Modify note
        onView(withId(R.id.add_word_note_btn)).perform(click())
        updateWordNote("Modified note text", 3, 0, 2, listPos)

        // Show more info
        onView(withId(R.id.more_info_word_btn)).perform(click())
        onView(withId(R.id.info_webview)).check(matches(isDisplayed()))
    }

    @Test
    fun given_selected_sentence_then_delete_new_and_old_notes() {
        setLocalPage(language = "en", initialNote = DEFAULT_NOTE)

        server.dispatcher = sentenceDispatcher()

        // For this test the 2nd paragraph will be used
        val listPos = 1
        // Highlight sentence
        clickParagraphList(listPos, doubleClick())

        // Translate sentence and tap last word within the sentence
        composeTestRule.onNodeWithContentDescription("Translate").performClick()
        clickParagraphList(listPos, clickPercent(0.9f, 0f))
        Thread.sleep(500)

        // Verify word translation shown
        onView(withId(R.id.word_translation)).check(matches(isDisplayed()))
        onView(withId(R.id.word_translation)).check(matches(withText("parrafo")))

        // Add new note
        onView(withId(R.id.add_word_note_btn)).perform(click())
        updateWordNote("New note text", 1, 10, 19, listPos)

        // Delete new note
        onView(withId(R.id.add_word_note_btn)).perform(click())
        composeTestRule.onNodeWithTag(DELETE_BTN_TAG).performClick()

        // Verify word layout removed from sheet
        onView(withId(R.id.word_translation)).check(matches(not(isDisplayed())))

        // Tap old note
        tapParagraphItemStart(listPos)

        // Delete old note
        onView(withId(R.id.add_word_note_btn)).perform(click())
        composeTestRule.onNodeWithTag(DELETE_BTN_TAG).performClick()

        // Verify word layout removed from sheet
        onView(withId(R.id.word_translation)).check(matches(not(isDisplayed())))
    }

    // endregion

    private fun setRemotePage() {
        val body = InstrumentationRegistry.getInstrumentation()
            .context.assets.open("test_page.html").bufferedReader().use { it.readText() }
        server.enqueue(MockResponse().setBody(body))

        val args = bundleOf("link" to server.url("/").toString())
        launchFragmentInHiltContainer<WebReaderFragment>(args, R.style.AppTheme)
    }

    private fun setLocalPage(language: String? = null, initialNote: Note? = null) {
        val url = server.url("/").toString()
        val uuid = UUID.fromString(default_uuid)
        val link = WebLink(url, language = language, uuid = uuid, id = DEFAULT_LINK_ID)
        runBlocking {
            linkDAO.upsert(link)
        }

        createXmlFile()

        if(initialNote != null) {
            runBlocking {
                noteDAO.upsert(initialNote)
            }
        }

        val args = bundleOf("link" to server.url("/").toString())
        launchFragmentInHiltContainer<WebReaderFragment>(args, R.style.AppTheme)
    }

    private fun setFirstWordResponse() {
        val response = GoogleTranslateResponse(listOf(Sentence("My", "Mi")), "es")
        val jsonResponse = responseAdapter.toJson(response)
        server.enqueue(MockResponse().setBody(jsonResponse))
    }

    private fun translateSelectionAndReturn(expectedTranslation: String){
        composeTestRule.onNodeWithContentDescription("Translate").performClick()

        Thread.sleep(500) // Wait for the sheet to be fully visible, otherwise the press back exits the app. This is the best solution, using idle resource freezes the test
        onView(withId(R.id.translated_text)).check(matches(isDisplayed()))
        onView(withId(R.id.translated_text)).check(matches(withText(expectedTranslation)))

        Espresso.pressBack()

        onView(withId(R.id.translated_text)).check(matches(not(isDisplayed())))
    }

    private fun sentenceDispatcher(): Dispatcher {
        return object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val text = request.requestUrl?.queryParameter("q")
                println(text)
                val response = when (text) {
                    FIRST_SENTENCE -> sentencesTranslationResponses.first()
                    SECOND_SENTENCE -> sentencesTranslationResponses[1]
                    THIRD_SENTENCE -> sentencesTranslationResponses.last()
                    LAST_WORD -> lastWordTransResponse
                    else -> throw IllegalArgumentException()
                }
                val body = responseAdapter.toJson(response)
                return MockResponse().setBody(body)
            }
        }
    }

    private fun clickParagraphList(position: Int, action: ViewAction){
        onView(withId(R.id.paragraphs_list))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(position, action)
            )
    }

    private fun tapParagraphItemStart(itemPos: Int) {
        // The regular click() is performed in the center of the view, this makes the click position vary and sometimes empty text is returned
        // Click on the (0, 0) position to ensure the first word is clicked
        onView(withId(R.id.paragraphs_list))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(itemPos, clickIn(0, 0))
            )
        Thread.sleep(500) // it's necessary to wait for the single tap confirmed event in the ParagraphAdapter
    }

    private fun showWebBottomSheet() {
        onView(withId(R.id.more_info_btn)).perform(click())
        onView(withId(R.id.info_webview)).check(matches(isDisplayed()))
    }

    private fun updateWordNote(
        text: String,
        colorIndex: Int,
        spanStart: Int,
        spanEnd: Int,
        listPos: Int = 0
    ){
        // Modify the text, color and save
        composeTestRule.onNodeWithTag(NOTE_TEXT_TAG).performTextReplacement(text)
        composeTestRule.onNodeWithTag(colorIndex.toString()).performClick()
        composeTestRule.onNodeWithTag(ACCEPT_BTN_TAG).performClick()

        // Verify bottom sheet note text changed
        onView(withId(R.id.word_translation)).check(matches(isDisplayed()))
        onView(withId(R.id.word_translation)).check(matches(withText(text)))

        val modifiedColor = COLORS[colorIndex].toArgb()
        onView(withId(R.id.paragraphs_list))
            .check(matches(atPosition(listPos, withBackgroundSpan(modifiedColor, spanStart, spanEnd))))
    }

    /**
     * Create a local copy of the page using the default folder location
     */
    private fun createXmlFile() {
        val folder = File(externalFilesPath, default_uuid)
        if(!folder.exists()) folder.mkdir()

        val tempFile = File(folder, "content.xml")
        tempFile.deleteOnExit()

        val context = InstrumentationRegistry.getInstrumentation().context
        context.assets.open("test_page.html").use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output, 1024)
            }
        }
    }

    companion object{
        const val FIRST_SENTENCE = "My First Heading\n"
        private const val FIRST_SENTENCE_TRANS = "Primer encabezado"
        const val SECOND_SENTENCE = "Body first paragraph"
        private const val SECOND_SENTENCE_TRANS = "Cuerpo del primer parrafo"
        const val THIRD_SENTENCE = "My second paragraph."
        private const val THIRD_SENTENCE_TRANS = "Mi segundo parrafo"

        val sentencesTranslationResponses = listOf(
            GoogleTranslateResponse(listOf(Sentence(FIRST_SENTENCE_TRANS, FIRST_SENTENCE)), "en"),
            GoogleTranslateResponse(listOf(Sentence(SECOND_SENTENCE_TRANS, SECOND_SENTENCE)), "en"),
            GoogleTranslateResponse(listOf(Sentence(THIRD_SENTENCE_TRANS, THIRD_SENTENCE)), "en"),
        )

        const val LAST_WORD = "paragraph"
        val lastWordTransResponse = GoogleTranslateResponse(listOf(Sentence("parrafo", LAST_WORD)), "en")

        private const val FIRST_PARAGRAPH = FIRST_SENTENCE + SECOND_SENTENCE
        private const val FIRST_PARAGRAPH_TRANS = FIRST_SENTENCE_TRANS + SECOND_SENTENCE_TRANS
        val paragraphTranslationResponse = GoogleTranslateResponse(listOf(Sentence(FIRST_PARAGRAPH_TRANS, FIRST_PARAGRAPH)), "es")

        const val default_uuid = "7e57d235-3553-4a57-bd35-37af9d5b1ffb"
        const val DEFAULT_LINK_ID = 2

        val DEFAULT_NOTE = Note("note text", "My", 37, 2, YellowNoteHighlight.toHex(), DEFAULT_LINK_ID)
    }
}
