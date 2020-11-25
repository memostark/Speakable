package com.guillermonegrete.tts.main;

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.data.source.FakeWordRepository
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.di.WordRepositorySourceModule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
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
        onView(withId(R.id.play_btn)).perform(ViewActions.click())

        // Language detected
        onView(withText(testTranslation.lang)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        activityScenario.close()
    }

}