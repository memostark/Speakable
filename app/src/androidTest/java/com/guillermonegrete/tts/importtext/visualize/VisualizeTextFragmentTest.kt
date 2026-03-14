package com.guillermonegrete.tts.importtext.visualize

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.di.TestApplicationModuleBinds
import com.guillermonegrete.tts.importtext.createEpubFile
import com.guillermonegrete.tts.launchFragmentInHiltContainer
import com.guillermonegrete.tts.utils.EspressoIdlingResource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.hamcrest.CoreMatchers.anything
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@UninstallModules(TestApplicationModuleBinds::class)
@HiltAndroidTest
class VisualizeTextFragmentTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    var testFolder = TemporaryFolder()

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @Test
    fun give_epub_file_when_load_then_book_layout() {
        val file = testFolder.createEpubFile()
        val bundle = bundleOf(
            VisualizeTextFragment.EPUB_URI to file.toUri(),
            VisualizeTextFragment.FILE_ID to -1
        )
        launchFragmentInHiltContainer<VisualizeTextFragment>(themeResId = R.style.AppTheme, intentExtras = bundle, intentAction = VisualizeTextFragment.SHOW_EPUB)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val firstChapterLabel = context.resources.getString(R.string.reader_current_chapter_label, 1, 3)
        onView(withId(R.id.reader_current_chapter)).check(matches(withText(firstChapterLabel)))
        onView(withId(R.id.show_toc_btn)).check(matches(isDisplayed()))

        // Test swiping chapters
        onView(withId(R.id.text_reader_viewpager)).perform(swipeLeft())
        onView(withId(R.id.text_reader_viewpager)).perform(swipeLeft())


        var expectedChapterLabel = context.resources.getString(R.string.reader_current_chapter_label, 2, 3)
        onView(withId(R.id.reader_current_chapter)).check(matches(withText(expectedChapterLabel)))

        onView(withId(R.id.text_reader_viewpager)).perform(swipeRight())
        onView(withId(R.id.reader_current_chapter)).check(matches(withText(firstChapterLabel)))

        // Enable split mode
        onView(withId(R.id.brightness_settings_btn)).perform(click())
        onView(withId(R.id.split_page_btn)).perform(click())

        Espresso.pressBack()

        onView(withId(R.id.arrow_btn)).perform(click())

        // Use table of contents and pick first chapter
        onView(withId(R.id.text_reader_viewpager)).perform(swipeLeft()) // navigate to second chapter

        onView(withId(R.id.show_toc_btn)).perform(click())
        composeTestRule.onNodeWithTag(TOC_BTN_TAG).performClick()
        onData(anything())
            .atPosition(0)
            .perform(click())
        onView(withId(R.id.reader_current_chapter)).check(matches(withText(firstChapterLabel)))
    }

    @Test
    fun give_text_input_when_load_then_text_layout() {
        val bundle = bundleOf(VisualizeTextFragment.IMPORTED_TEXT to "Input text")
        launchFragmentInHiltContainer<VisualizeTextFragment>(themeResId = R.style.AppTheme, intentExtras = bundle)

        // These views are only visible for epub files
        onView(withId(R.id.reader_current_chapter)).check(matches(not(isDisplayed())))
        onView(withId(R.id.show_toc_btn)).check(matches(not(isDisplayed())))
    }

}
