package com.guillermonegrete.tts.utils

import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior

fun <T: View> createBackPressedCallback(behavior: BottomSheetBehavior<T>): OnBackPressedCallback {
    return object : OnBackPressedCallback(false) {
        override fun handleOnBackStarted(backEvent: BackEventCompat) {
            behavior.startBackProgress(backEvent)
        }

        override fun handleOnBackProgressed(backEvent: BackEventCompat) {
            behavior.updateBackProgress(backEvent)
        }

        override fun handleOnBackPressed() {
            behavior.handleBackInvoked()
        }

        override fun handleOnBackCancelled() {
            behavior.cancelBackProgress()
        }
    }
}
