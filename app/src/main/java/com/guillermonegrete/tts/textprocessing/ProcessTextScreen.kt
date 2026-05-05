package com.guillermonegrete.tts.textprocessing

import android.os.Parcelable
import com.guillermonegrete.tts.data.Translation
import com.guillermonegrete.tts.importtext.visualize.model.SplitPageSpan
import kotlinx.parcelize.Parcelize

@Parcelize
data class SentenceDialogUIState(
    val isLoading: Boolean = false,
    val translation: Translation? = null,
    val highlights: SplitPageSpan? = null,
    val selectedWord: WordState? = null,
    val hasError: String? = null,
): Parcelable {
    class Builder(origin: SentenceDialogUIState) {
        private var isLoading = origin.isLoading
        private var translation = origin.translation
        private var highlights = origin.highlights
        private var selectedWord = origin.selectedWord
        private var hasError = origin.hasError

        // also performs operations on 'this' and returns 'this'
        fun isLoading(value: Boolean) = this.also { isLoading = value }
        fun translation(value: Translation?) = this.also { translation = value }
        fun highlights(value: SplitPageSpan?) = this.also { highlights = value }
        fun selectedWord(value: WordState?) = this.also { selectedWord = value }
        fun hasError(value: String?) = this.also { hasError = value }

        fun build() = SentenceDialogUIState(isLoading, translation, highlights, selectedWord, hasError)
    }
}

@Parcelize
data class SentenceEditingUIState @JvmOverloads constructor(
    val isEditing: Boolean = false,
    val isDeleteDialogShown: Boolean = false,
): Parcelable
