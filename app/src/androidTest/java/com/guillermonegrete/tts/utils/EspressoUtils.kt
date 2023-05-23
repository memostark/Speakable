package com.guillermonegrete.tts.utils

import android.text.Spannable
import android.text.style.BackgroundColorSpan
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import com.google.android.material.tabs.TabLayout
import org.hamcrest.CoreMatchers
import org.hamcrest.Description
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

/**
 * Performs a click in the given coordinates
 */
fun clickIn(x: Int, y: Int): ViewAction {
    return GeneralClickAction(
        Tap.SINGLE, { view ->
            val screenPos = IntArray(2)
            view?.getLocationOnScreen(screenPos)

            val screenX = (screenPos[0] + x).toFloat()
            val screenY = (screenPos[1] + y).toFloat()

            floatArrayOf(screenX, screenY)
        },
        Press.FINGER,
        InputDevice.SOURCE_MOUSE,
        MotionEvent.BUTTON_PRIMARY)
}

/**
 * Taken from: https://stackoverflow.com/a/34795431/10244759
 */
fun atPosition(position: Int, itemMatcher: Matcher<View?>): Matcher<View?> {
    return object : BoundedMatcher<View?, RecyclerView>(RecyclerView::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("has item at position $position: ")
            itemMatcher.describeTo(description)
        }

        override fun matchesSafely(view: RecyclerView): Boolean {
            val viewHolder = view.findViewHolderForAdapterPosition(position)
                ?: return false // has no item on such position
            return itemMatcher.matches(viewHolder.itemView)
        }
    }
}

/**
 * Matches that the [TextView] has the background color of the [color] parameter
 */
fun withBackgroundSpan(color: Int, start: Int, end: Int): Matcher<View?> {

    val span = BackgroundColorSpan(color)

    return object : BoundedMatcher<View?, TextView>(TextView::class.java) {

        override fun describeTo(description: Description) {
            description.appendText("with background color span: ").appendValue(span.backgroundColor)
        }

        override fun matchesSafely(foundView: TextView): Boolean {
            val text = (foundView.text as? Spannable) ?: return false

            text.getSpans(0, text.length, BackgroundColorSpan::class.java).map { viewSpan ->
                if (viewSpan.backgroundColor == span.backgroundColor &&
                    start == text.getSpanStart(viewSpan) &&
                    end == text.getSpanEnd(viewSpan)
                ) return true
            }
            return false
        }
    }
}
