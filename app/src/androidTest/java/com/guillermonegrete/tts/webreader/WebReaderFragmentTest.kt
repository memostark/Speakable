package com.guillermonegrete.tts.webreader

import androidx.core.os.bundleOf
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.di.WordRepositorySourceModule
import com.guillermonegrete.tts.launchFragmentInHiltContainer
import com.guillermonegrete.tts.utils.EspressoIdlingResource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@UninstallModules(WordRepositorySourceModule::class)
@HiltAndroidTest
class WebReaderFragmentTest{

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

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
        val args = bundleOf("link" to "https://en.wikipedia.org/wiki/Wiktionary")
        launchFragmentInHiltContainer<WebReaderFragment>(args, R.style.AppTheme)

        onView(withId(R.id.paragraphs_list)).check(matches(not(isDisplayed())))
        onView(withId(R.id.list_toggle)).perform(click())
        onView(withId(R.id.body_text)).check(matches(not(isDisplayed())))
    }
}