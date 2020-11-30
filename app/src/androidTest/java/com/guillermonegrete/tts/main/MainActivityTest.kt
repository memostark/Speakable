package com.guillermonegrete.tts.main;

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.data.source.FakeWordRepository
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.di.WordRepositorySourceModule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.hamcrest.Matchers.hasToString
import org.hamcrest.Matchers.startsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test

import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@LargeTest
@UninstallModules(WordRepositorySourceModule::class)
@HiltAndroidTest
class MainActivityTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: FakeWordRepository

    @Before
    fun init() {
        hiltRule.inject()
    }


    @Test
    fun playWordInSpanish(){
        val testTranslation = Words("Texto de prueba", "es", "test sentence")
        repository.addTranslation(testTranslation)

        val activityScenario = ActivityScenario.launch(MainActivity::class.java)

        // Insert text and click play button
        onView(withId(R.id.tts_edit_text)).perform(replaceText(testTranslation.word))
        onView(withId(R.id.play_btn)).perform(click())

        // Language detected
        onView(withText(testTranslation.lang)).check(matches(isDisplayed()))

        activityScenario.close()
    }

    @Test
    fun changeLanguageFrom(){
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.pick_language)).check(matches(withText("AUTO DETECT")))
        onView(withId(R.id.pick_language)).perform(click())

        onData(hasToString(startsWith("English")))
            .inAdapterView(withId(R.id.languages_list))
            .perform(click())

        onView(withId(R.id.pick_language)).check(matches(withText("English")))

        activityScenario.close()
    }

}