package com.guillermonegrete.tts.webreader

import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
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
    fun given_default_layout_when_toggle_button_then_paragraph_layout(){
        val body = readPage()
        server.enqueue(MockResponse().setBody(body))

        val args = bundleOf("link" to server.url("/").toString())
        launchFragmentInHiltContainer<WebReaderFragment>(args, R.style.AppTheme)

        onView(withId(R.id.paragraphs_list)).check(matches(isDisplayed()))

        val response = GoogleTranslateResponse(listOf(Sentence("My", "Mi")), "es")
        val jsonResponse = responseAdapter.toJson(response)
        server.enqueue(MockResponse().setBody(jsonResponse))

        onView(withId(R.id.paragraphs_list))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click())
            )

        Thread.sleep(500) // it's necessary to wait for the single tap confirmed event in the ParagraphAdapter
        onView(withId(R.id.menu_bar)).check(matches(not(isDisplayed())))

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
        onView(withId(R.id.paragraphs_list))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, longClick())
            )

        server.dispatcher = sentenceDispatcher()

        // translate selection then move to the next one, until all the sentences were translated
        for (response in sentencesTranslationResponses){
            val translation = response.sentences.first().trans
            translateSelectionAndReturn(translation)
            onView(withId(R.id.next_selection)).perform(click())
        }

        // Verify the last next_selection click didn't change the selection (the last sentence was selected so no change)
        val translation = sentencesTranslationResponses.last().sentences.first().trans
        translateSelectionAndReturn(translation)

        // Verify previous_selection works
        onView(withId(R.id.previous_selection)).perform(click())
        val secondTranslation = sentencesTranslationResponses[1].sentences.first().trans
        translateSelectionAndReturn(secondTranslation)
    }

    private fun readPage() =
        InstrumentationRegistry.getInstrumentation()
            .context.assets.open("test_page.html").bufferedReader().use { it.readText() }

    private fun translateSelectionAndReturn(expectedTranslation: String){
        onView(withId(R.id.translate)).perform(click())

        onView(withId(R.id.translated_text)).check(matches(isDisplayed()))
        onView(withId(R.id.translated_text)).check(matches(withText(expectedTranslation)))

        Espresso.pressBack()

        onView(withId(R.id.translated_text)).check(matches(not(isDisplayed())))
        Thread.sleep(500) // wait for the sheet to settle, this is the best solution using idle resource freezes the test
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

    companion object{
        const val FIRST_SENTENCE = "My First Heading\n"
        const val FIRST_SENTENCE_TRANS = "Primer encabezado"
        const val SECOND_SENTENCE = "Body first paragraph"
        const val SECOND_SENTENCE_TRANS = "Cuerpo del primer parrafo"
        const val THIRD_SENTENCE = "My second paragraph."
        const val THIRD_SENTENCE_TRANS = "Mi segundo parrafo"

        val sentencesTranslationResponses = listOf(
            GoogleTranslateResponse(listOf(Sentence(FIRST_SENTENCE_TRANS, FIRST_SENTENCE)), "en"),
            GoogleTranslateResponse(listOf(Sentence(SECOND_SENTENCE_TRANS, SECOND_SENTENCE)), "en"),
            GoogleTranslateResponse(listOf(Sentence(THIRD_SENTENCE_TRANS, THIRD_SENTENCE)), "en"),
        )
    }
}
