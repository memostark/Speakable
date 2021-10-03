package com.guillermonegrete.tts.textprocessing

import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.content.edit
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.data.Segment
import com.guillermonegrete.tts.data.Translation
import com.guillermonegrete.tts.data.source.FakeWordRepository
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.di.WordRepositorySourceModule
import com.guillermonegrete.tts.launchFragmentInHiltContainer
import com.guillermonegrete.tts.utils.EspressoIdlingResource
import com.guillermonegrete.tts.utils.selectTabAtPosition
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.hamcrest.CoreMatchers.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject


@MediumTest
@RunWith(AndroidJUnit4::class)
@UninstallModules(WordRepositorySourceModule::class)
@HiltAndroidTest
class TextInfoDialogTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: FakeWordRepository

    @Inject
    internal lateinit var preferences: SharedPreferences

    @Before
    fun init() {
        // Populate @Inject fields in test class
        hiltRule.inject()
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
    fun wordInput_showWordDialogLayout(){
        repository.addRemoteWords(Words("prueba", "es", "test"))

        val inputText = "prueba"
        val bundle = Bundle().apply {
            putString(TextInfoDialog.TEXT_KEY, inputText)
            putString(TextInfoDialog.ACTION_KEY, TextInfoDialog.NO_SERVICE)
        }

        launchFragmentInHiltContainer<TextInfoDialog>(bundle,  R.style.ProcessTextStyle_White)

        // Specific container of word layout
        onView(withId(R.id.testLayout)).check(matches(isDisplayed()))

        // Check view with input text
        onView(withId(R.id.text_tts)).check(matches(isDisplayed()))
        onView(withId(R.id.text_tts)).check(matches(withText(inputText)))

        // Default language preference is "Auto detect"
        onView(withId(R.id.spinner_language_from))
            .check(matches(withSpinnerText(containsString("Auto detect"))))

        // Because is in "Auto detect" this view shows the detected language
        onView(withId(R.id.text_language_code)).check(matches(isDisplayed()))
        onView(withId(R.id.text_language_code)).check(matches(withText("es")))

        // Play button is visible
        onView(withId(R.id.play_icons_container)).check(matches(isDisplayed()))
        onView(withId(R.id.play_tts_icon)).check(matches(isDisplayed()))

        // Save button is visible
        onView(withId(R.id.save_icon)).check(matches(isDisplayed()))
    }

    @Test
    fun sentenceInput_showSentenceDialogLayout(){
        repository.addTranslation(Words("oración de prueba", "en", "test sentence"))

        val inputText = "oración de prueba"
        val bundle = Bundle().apply {
            putString(TextInfoDialog.TEXT_KEY, inputText)
            putString(TextInfoDialog.ACTION_KEY, TextInfoDialog.NO_SERVICE)
        }
        launchFragmentInHiltContainer<TextInfoDialog>(bundle,  R.style.ProcessTextStyle_White)

        // Specific container of word layout
        onView(withId(R.id.sentence_root)).check(matches(isDisplayed()))

        // Check view with input text
        onView(withId(R.id.text_tts)).check(matches(isDisplayed()))
        onView(withId(R.id.text_tts)).check(matches(withText(inputText)))

        // Default language preference is "Auto detect"
        onView(withId(R.id.spinner_language_from))
            .check(matches(withSpinnerText(containsString("Auto detect"))))

        // Because is in "Auto detect" this view shows the detected language
        onView(withId(R.id.text_language_code)).check(matches(isDisplayed()))
        onView(withId(R.id.text_language_code)).check(matches(withText("en")))

        // Play button is visible
        onView(withId(R.id.play_icons_container)).check(matches(isDisplayed()))
        onView(withId(R.id.play_tts_icon)).check(matches(isDisplayed()))
    }

    @Test
    fun extraWord_showDictWithWordLayout(){

        val inputWord = Words("prueba", "ES", "test")

        val bundle = Bundle().apply {
            putString(TextInfoDialog.TEXT_KEY, inputWord.word)
            putParcelable(TextInfoDialog.WORD_KEY, inputWord)
            putString(TextInfoDialog.ACTION_KEY, TextInfoDialog.NO_SERVICE)
        }

        launchFragmentInHiltContainer<TextInfoDialog>(bundle,  R.style.ProcessTextStyle_White)

        // Check pre-set language
        onView(withId(R.id.text_language_code)).check(matches(isDisplayed()))
        onView(withId(R.id.text_language_code)).check(matches(withText("ES")))

        // Save and edit icon should be visible
        onView(withId(R.id.save_icon)).check(matches(isDisplayed()))
        onView(withId(R.id.edit_icon)).check(matches(isDisplayed()))
    }

    @Test
    fun given_translation_fragment_when_spinner_changed_then_new_translation(){
        repository.addTranslation(Translation(listOf(Segment("nachweisen", "prueba")), "de"))
        // From english to german
        // Initial language has to be different than the selected language, otherwise spinner doesn't call onChange
        preferences.edit(commit = true) {
            putInt(TextInfoDialog.LANGUAGE_PREFERENCE, 15) // 15 is the english position
        }

        startWordLayout()

        onView(withId(R.id.pager_menu_dots)).perform(selectTabAtPosition(1))

        onView(withId(R.id.translation_text)).check(matches(isDisplayed()))
        onView(withId(R.id.translation_text)).check(matches(withText("test")))

        // Wait tab sliding animation
        Thread.sleep(1000)
        onView(withId(R.id.translate_to_spinner)).perform(click())


        onData(allOf(`is`(instanceOf(String::class.java)), `is`("German"))).perform(click())
        onView(withId(R.id.translate_to_spinner)).check(matches(withSpinnerText("German")))

        onView(withId(R.id.translation_text)).check(matches(withText("nachweisen")))
    }

    private fun startWordLayout(){
        repository.addRemoteWords(Words("prueba", "es", "test"))

        val inputText = "prueba"
        val bundle = Bundle().apply {
            putString(TextInfoDialog.TEXT_KEY, inputText)
            putString(TextInfoDialog.ACTION_KEY, TextInfoDialog.NO_SERVICE)
        }

        launchFragmentInHiltContainer<TextInfoDialog>(bundle,  R.style.ProcessTextStyle_White)
    }

}