package com.guillermonegrete.tts.textprocessing

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.guillermonegrete.tts.common.compose.SPINNER_LIST_TAG
import com.guillermonegrete.tts.common.compose.SPINNER_TEXT_TAG
import com.guillermonegrete.tts.common.compose.StringList
import com.guillermonegrete.tts.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test

class TextInfoScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun when_sentence_dialog_displayed_then_verify_text() {

        composeTestRule.setContent {
            AppTheme {
                val transState = remember { mutableStateOf("Translation") }
                SentenceDialog(
                    true,
                    "Original text",
                    transState,
                    StringList(listOf("en", "sp")),
                    StringList(listOf("de", "es", "it")),
                    0,
                )
            }
        }

        composeTestRule.onNodeWithText("Original text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Translation").assertIsDisplayed()
        val spinners = getSpinners()
        spinners.onFirst().assertTextEquals("en")
        spinners[1].assertTextEquals("de")

        // Change language from
        composeTestRule.onNodeWithText("en").performClick()
        composeTestRule.onNodeWithTag(SPINNER_LIST_TAG).onChildren().assertCountEquals(2)
        composeTestRule.onNodeWithText("sp").performClick()
        getSpinners().onFirst().assertTextEquals("sp")

        // Change language to
        composeTestRule.onNodeWithText("de").performClick()
        composeTestRule.onNodeWithTag(SPINNER_LIST_TAG).onChildren().assertCountEquals(3)
        composeTestRule.onNodeWithText("es").performClick()
        getSpinners()[1].assertTextEquals("es")
    }

    private fun getSpinners() = composeTestRule.onAllNodesWithTag(SPINNER_TEXT_TAG, useUnmergedTree = true)

}
