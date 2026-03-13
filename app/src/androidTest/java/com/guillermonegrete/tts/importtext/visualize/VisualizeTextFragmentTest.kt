package com.guillermonegrete.tts.importtext.visualize

import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.di.TestApplicationModuleBinds
import com.guillermonegrete.tts.importtext.createEpubFile
import com.guillermonegrete.tts.launchFragmentInHiltContainer
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.hamcrest.CoreMatchers.not
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
    var testFolder = TemporaryFolder()

    @Test
    fun give_epub_file_when_load_then_book_layout() {
        val file = testFolder.createEpubFile()
        val bundle = bundleOf(
            VisualizeTextFragment.EPUB_URI to file.toUri(),
            VisualizeTextFragment.FILE_ID to -1
        )
        launchFragmentInHiltContainer<VisualizeTextFragment>(themeResId = R.style.AppTheme, intentExtras = bundle, intentAction = VisualizeTextFragment.SHOW_EPUB)

        onView(withId(R.id.reader_current_chapter)).check(matches(isDisplayed()))
        onView(withId(R.id.show_toc_btn)).check(matches(isDisplayed()))
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
