package com.guillermonegrete.tts.webreader

import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
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
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
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

        Thread.sleep(1000)
        onView(withId(R.id.menu_bar)).check(matches(not(isDisplayed())))

        onView(withId(R.id.translated_text)).check(matches(isDisplayed()))
        onView(withId(R.id.translated_text)).check(matches(withText("My")))

        onView(withId(R.id.more_info_btn)).perform(click())
    }

    @Test
    fun when_sentence_long_pressed_then_highlight(){
        val body = readPage()
        server.enqueue(MockResponse().setBody(body))

        val args = bundleOf("link" to server.url("/").toString())
        launchFragmentInHiltContainer<WebReaderFragment>(args, R.style.AppTheme)

        onView(withId(R.id.paragraphs_list)).check(matches(isDisplayed()))

        onView(withId(R.id.paragraphs_list))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, longClick())
            )

        val response = GoogleTranslateResponse(listOf(Sentence("My", "My")), "en")
        val jsonResponse = responseAdapter.toJson(response)
        server.enqueue(MockResponse().setBody(jsonResponse))
        onView(withId(R.id.translate)).perform(click())

        onView(withId(R.id.translated_text)).check(matches(isDisplayed()))
        onView(withId(R.id.translated_text)).check(matches(withText("My")))
    }

    private fun readPage() =
        InstrumentationRegistry.getInstrumentation()
            .context.assets.open("test_page.html").bufferedReader().use { it.readText() }
}