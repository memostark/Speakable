package com.guillermonegrete.tts.textprocessing

import android.os.Bundle
import androidx.fragment.app.testing.launchFragment
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.utils.EspressoIdlingResource
import org.hamcrest.CoreMatchers.*
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@MediumTest
@RunWith(AndroidJUnit4::class)
class TextInfoDialogTest {

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

        val bundle = Bundle()
        val inputText = "prueba"
        bundle.putString(TextInfoDialog.TEXT_KEY, inputText)

        val scenario = launchFragment<TextInfoDialog>(bundle,  R.style.ProcessTextStyle_White)

        scenario.onFragment { fragment ->
            assertNotNull(fragment.dialog)
            assertTrue(fragment.requireDialog().isShowing)
        }

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

        val bundle = Bundle()
        val inputText = "oración de prueba"
        bundle.putString(TextInfoDialog.TEXT_KEY, inputText)
        val scenario = launchFragment<TextInfoDialog>(bundle,  R.style.ProcessTextStyle_White)

        scenario.onFragment { fragment ->
            assertNotNull(fragment.dialog)
            assertTrue(fragment.requireDialog().isShowing)
        }

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
        onView(withId(R.id.text_language_code)).check(matches(withText("es")))

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

        val scenario = launchFragment<TextInfoDialog>(bundle,  R.style.ProcessTextStyle_White)

        scenario.onFragment { fragment ->
            assertNotNull(fragment.dialog)
            assertTrue(fragment.requireDialog().isShowing)
        }

        // Check pre-set language
        onView(withId(R.id.text_language_code)).check(matches(isDisplayed()))
        onView(withId(R.id.text_language_code)).check(matches(withText("ES")))

        // Save and edit icon should be visible
        onView(withId(R.id.save_icon)).check(matches(isDisplayed()))
        onView(withId(R.id.edit_icon)).check(matches(isDisplayed()))
    }

}