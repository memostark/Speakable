package com.guillermonegrete.tts.webreader

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
import com.guillermonegrete.tts.di.TestApplicationModuleBinds
import com.guillermonegrete.tts.launchFragmentInHiltContainer
import com.guillermonegrete.tts.utils.EspressoIdlingResource
import com.guillermonegrete.tts.utils.clickIn
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@MediumTest
@RunWith(AndroidJUnit4::class)
@UninstallModules(TestApplicationModuleBinds::class)
@HiltAndroidTest
class WebReaderFragmentTest{

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var server: MockWebServer

    private val moshi: Moshi = Moshi.Builder().build()
    private val responseAdapter: JsonAdapter<GoogleTranslateResponse> = moshi.adapter(GoogleTranslateResponse::class.java)

    @Before
    fun setup(){
        server = MockWebServer()
        server.start(8081)
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

    @Test
    fun when_word_tapped_then_translation_shown(){
        val body = readPage()
        server.enqueue(MockResponse().setBody(body))

        val args = bundleOf("link" to server.url("/").toString())
        launchFragmentInHiltContainer<WebReaderFragment>(args, R.style.AppTheme)

        onView(withId(R.id.paragraphs_list)).check(matches(isDisplayed()))

        val response = GoogleTranslateResponse(listOf(Sentence("My", "Mi")), "es")
        val jsonResponse = responseAdapter.toJson(response)
        server.enqueue(MockResponse().setBody(jsonResponse))

        // The regular click() is performed in the center of the view, this makes the click position vary and sometimes empty text is returned
        // Click on the (0, 0) position to ensure the first word is clicked
        clickParagraphList(0, clickIn(0, 0))

        Thread.sleep(500) // it's necessary to wait for the single tap confirmed event in the ParagraphAdapter
        composeTestRule.onNodeWithTag("web_reader_bar").assertIsNotDisplayed()

        onView(withId(R.id.translated_text)).check(matches(isDisplayed()))
        onView(withId(R.id.translated_text)).check(matches(withText("My")))

        onView(withId(R.id.more_info_btn)).perform(click())
        onView(withId(R.id.info_webview)).check(matches(isDisplayed()))
    }

    @Test
    fun when_sentence_long_pressed_then_highlight(){
        val body = readPage()
        server.enqueue(MockResponse().setBody(body))

        val args = bundleOf("link" to server.url("/").toString())
        launchFragmentInHiltContainer<WebReaderFragment>(args, R.style.AppTheme)

        onView(withId(R.id.paragraphs_list)).check(matches(isDisplayed()))

        // Highlight first sentence
        clickParagraphList(0, longClick())

        server.dispatcher = sentenceDispatcher()

        // translate selection then move to the next one, until all the sentences were translated
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
    fun when_sentence_double_tapped_then_show_expanded_item(){
        val body = readPage()
        server.enqueue(MockResponse().setBody(body))

        val args = bundleOf("link" to server.url("/").toString())
        launchFragmentInHiltContainer<WebReaderFragment>(args, R.style.AppTheme)

        onView(withId(R.id.paragraphs_list)).check(matches(isDisplayed()))

        // Double click and verify item expanded
        clickParagraphList(0, doubleClick())

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

    private fun readPage() =
        InstrumentationRegistry.getInstrumentation()
            .context.assets.open("test_page.html").bufferedReader().use { it.readText() }

    private fun translateSelectionAndReturn(expectedTranslation: String){
        composeTestRule.onNodeWithContentDescription("Translate").performClick()

        onView(withId(R.id.translated_text)).check(matches(isDisplayed()))
        onView(withId(R.id.translated_text)).check(matches(withText(expectedTranslation)))
        Thread.sleep(500) // Wait for the sheet to be fully visible, otherwise the press back exits the app. This is the best solution, using idle resource freezes the test

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

        private const val FIRST_PARAGRAPH = FIRST_SENTENCE + SECOND_SENTENCE
        private const val FIRST_PARAGRAPH_TRANS = FIRST_SENTENCE_TRANS + SECOND_SENTENCE_TRANS
        val paragraphTranslationResponse = GoogleTranslateResponse(listOf(Sentence(FIRST_PARAGRAPH_TRANS, FIRST_PARAGRAPH)), "es")
    }
}
