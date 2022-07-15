package com.guillermonegrete.tts.importtext

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
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
import androidx.test.platform.app.InstrumentationRegistry
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.data.source.DefaultFileRepository
import com.guillermonegrete.tts.db.BookFile
import com.guillermonegrete.tts.di.WordRepositorySourceModule
import com.guillermonegrete.tts.importtext.visualize.VisualizeTextActivity
import com.guillermonegrete.tts.launchFragmentInHiltContainer
import com.guillermonegrete.tts.utils.selectTabAtPosition
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import org.junit.rules.TemporaryFolder
import java.io.File

@MediumTest
@RunWith(AndroidJUnit4::class)
@UninstallModules(WordRepositorySourceModule::class)
@HiltAndroidTest
class ImportTextFragmentTest{

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: DefaultFileRepository

    @get:Rule
    var testFolder = TemporaryFolder()

    @Before
    fun setUp() {
        hiltRule.inject()
        Intents.init()
    }

    @After
    fun tearDown(){
        Intents.release()
    }

    /**
     * Create an epub file in a temporary directory copied from the assets.
     */
    private fun createEpubFile(): File{
        val context = InstrumentationRegistry.getInstrumentation().context

        val tempFile = testFolder.newFile("copied_file.epub")
        context.assets.open("test_epub.epub").use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output, 1024)
            }
        }

        return tempFile
    }

    @Test
    fun given_one_recent_file_when_clicked_then_navigate_to_visualizer(){

        val tempFile = testFolder.newFile("copied_file.epub")

        runBlocking {
            repository.saveFile(BookFile(
                tempFile.toUri().toString(),
                "Test book",
                ImportedFileType.EPUB
            ))
        }

        launchFragmentInHiltContainer<ImportTextFragment>(bundleOf(), R.style.AppTheme)

        onView(withId(R.id.recent_files_list))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        intended(hasComponent(VisualizeTextActivity::class.java.name))
    }

    @Test
    fun when_file_added_then_navigate_to_visualizer(){

        launchFragmentInHiltContainer<ImportTextFragment>(bundleOf(), R.style.AppTheme)

        // Mock receiving the intent with the uri of the epub
        val copiedFile = createEpubFile()
        val resultData = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE, copiedFile.toUri())
        resultData.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION and Intent.FLAG_GRANT_WRITE_URI_PERMISSION and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
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
}
