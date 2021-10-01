package com.guillermonegrete.tts.importtext

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.android.material.tabs.TabLayout
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.data.source.DefaultFileRepository
import com.guillermonegrete.tts.di.WordRepositorySourceModule
import com.guillermonegrete.tts.importtext.visualize.VisualizeTextActivity
import com.guillermonegrete.tts.launchFragmentInHiltContainer
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
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
class ImportTextFragmentTest{

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: DefaultFileRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        Intents.init()
    }

    @After
    fun tearDown(){
        Intents.release()
    }

    @Test
    fun given_one_recent_file_when_clicked_then_navigate_to_visualizer(){

        launchFragmentInHiltContainer<ImportTextFragment>(bundleOf(), R.style.AppTheme)

        onView(withId(R.id.recent_files_list))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        intended(hasComponent(VisualizeTextActivity::class.java.name))
    }

    @Test
    fun when_file_added_then_navigate_to_visualizer(){

        launchFragmentInHiltContainer<ImportTextFragment>(bundleOf(), R.style.AppTheme)

        // Mock receiving the intent with the uri of the epub
        val resultData = Intent()
        resultData.data = Uri.parse("content://com.android.providers.downloads.documents/document/raw%3A%2Fstorage%2Femulated%2F0%2FDownload%2F%D7%94%D7%A9%D7%95%D7%A2%D7%9C%20%D7%95%D7%94%D7%A9%D7%A4%D7%9F.epub")
        resultData.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        val result = Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)

        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(result)

        onView(withId(R.id.pick_file_fab)).perform(click())
        onView(withId(R.id.pick_epub_file_btn)).perform(click())

        intended(hasComponent(VisualizeTextActivity::class.java.name))
    }

    @Test
    fun given_in_files_tab_when_add_text_then_navigate_to_visualizer(){

        launchFragmentInHiltContainer<ImportTextFragment>(bundleOf(), R.style.AppTheme)

        onView(withId(R.id.import_tab_layout)).perform(selectTabAtPosition(1))

        // This can cause the tab to suddenly change, making the test fail
        // A fix hasn't been found yet
//        onView(withId(R.id.import_text_edit)).perform(typeText("New text to import"), closeSoftKeyboard())
        Thread.sleep(500)
        onView(withId(R.id.visualize_btn)).check(matches(isDisplayed())).perform(click())

        intended(hasComponent(VisualizeTextActivity::class.java.name))
    }

    private fun selectTabAtPosition(position: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return allOf(isDisplayed(), isAssignableFrom(TabLayout::class.java))
            }

            override fun getDescription(): String {
                return "selecting tab at index $position"
            }

            override fun perform(uiController: UiController, view: View) {
                if (view is TabLayout) {
                    val tab = view.getTabAt(position)
                    tab?.select()
                }
            }
        }
    }
}
