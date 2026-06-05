package com.guillermonegrete.tts.common.notes

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.guillermonegrete.tts.common.compose.CONFIRM_BTN_TAG
import com.guillermonegrete.tts.db.NoteType
import com.guillermonegrete.tts.db.WebLink
import com.guillermonegrete.tts.db.WebLinkDAO
import com.guillermonegrete.tts.di.TestApplicationModuleBinds
import com.guillermonegrete.tts.ui.theme.AppTheme
import com.guillermonegrete.tts.ui.theme.RedNoteHighlight
import com.guillermonegrete.tts.ui.theme.YellowNoteHighlight
import com.guillermonegrete.tts.webreader.db.Note
import com.guillermonegrete.tts.webreader.db.NoteDAO
import com.guillermonegrete.tts.webreader.toHex
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@UninstallModules(TestApplicationModuleBinds::class)
@HiltAndroidTest
class NotesListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var noteDAO: NoteDAO

    @Inject
    lateinit var webLinkDAO: WebLinkDAO

    @Before
    fun setup(){
        hiltRule.inject()
    }

    @Test
    fun verify_notes_displayed() = runTest {

        val id = webLinkDAO.upsert(WebLink("dummy_url")).toInt()
        val note1 = Note("First note", "Dummy original", 5, 10, YellowNoteHighlight.toHex(), linkId = id)
        val note2 = Note("Second note", "Other original text", 5, 10, RedNoteHighlight.toHex(), linkId = id)
        noteDAO.upsert(note1)
        noteDAO.upsert(note2)

        val viewModel = NotesListViewModel(id = id, NoteType.WEB_LINK, noteDAO)
        composeTestRule.setContent {
            AppTheme {
                NotesListScreen(viewModel)
            }
        }

        // Verify notes displayed
        val items = composeTestRule.onAllNodesWithContentDescription("Options", substring = true)
        items.assertCountEquals(2)
        composeTestRule.onNodeWithText(note1.text).assertIsDisplayed()
        composeTestRule.onNodeWithText(note1.originalText).assertIsDisplayed()
        composeTestRule.onNodeWithText(note2.text).assertIsDisplayed()
        composeTestRule.onNodeWithText(note2.originalText).assertIsDisplayed()

        // Delete note
        items.onFirst().performClick()
        composeTestRule.onNodeWithTag(DELETE_NOTE_BTN_TAG).performClick()
        composeTestRule.onNodeWithTag(CONFIRM_BTN_TAG).performClick()

        composeTestRule.onAllNodesWithContentDescription("Options", substring = true).assertCountEquals(1)
        composeTestRule.onNodeWithText(note1.text).assertIsNotDisplayed()
        composeTestRule.onNodeWithText(note1.originalText).assertIsNotDisplayed()
    }
}
