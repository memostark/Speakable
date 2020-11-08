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
import com.guillermonegrete.tts.utils.EspressoIdlingResource
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
//        launch
        val bundle = Bundle()
        val inputText = "test"
        bundle.putString(TextInfoDialog.TEXT_KEY, inputText)

        val scenario = launchFragment<TextInfoDialog>(bundle,  R.style.ProcessTextStyle_White)

        scenario.onFragment { fragment ->
            assertNotNull(fragment.dialog)
            assertTrue(fragment.requireDialog().isShowing)
        }

        // Specific container of word layout
        onView(withId(R.id.testLayout)).check(matches(isDisplayed()))

        onView(withId(R.id.text_tts)).check(matches(isDisplayed()))
        onView(withId(R.id.text_tts)).check(matches(withText(inputText)))
    }

    @Test
    fun sentenceInput_showSentenceDialogLayout(){
//        launch
        val bundle = Bundle()
        val inputText = "test sentence"
        bundle.putString(TextInfoDialog.TEXT_KEY, inputText)
        val scenario = launchFragment<TextInfoDialog>(bundle,  R.style.ProcessTextStyle_White)

        scenario.onFragment { fragment ->
            assertNotNull(fragment.dialog)
            assertTrue(fragment.requireDialog().isShowing)
        }

        // Specific container of word layout
        onView(withId(R.id.sentence_root)).check(matches(isDisplayed()))

        onView(withId(R.id.text_tts)).check(matches(isDisplayed()))
        onView(withId(R.id.text_tts)).check(matches(withText(inputText)))
    }

}