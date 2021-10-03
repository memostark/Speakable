package com.guillermonegrete.tts.utils

import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import com.google.android.material.tabs.TabLayout
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher

fun selectTabAtPosition(position: Int): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return CoreMatchers.allOf(
                ViewMatchers.isDisplayed(),
                ViewMatchers.isAssignableFrom(TabLayout::class.java)
            )
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
