package com.guillermonegrete.tts.data

import android.os.Parcelable
import com.guillermonegrete.tts.db.Words
import kotlinx.parcelize.Parcelize
import java.lang.Exception

sealed class Result<out T> {

    data class Success<out T>(val data: T): Result<T>()
    // In case you use Java you need to explicitly use <out T>
    // If you use Kotlin only code you can simply use:
    // data class Error(val exception: Exception): Result<Nothing>()
    data class Error<out T>(val exception: Exception): Result<T>()
}

/**
 * Same as [Result] but with an additional loading state.
 */
sealed class LoadResult<out T>{

    data class Success<out T>(val data: T): LoadResult<T>()
    data class Error<out T>(val throwable: Throwable): LoadResult<T>()
    data object Loading : LoadResult<Nothing>()
}

/**
 * Similar to [LoadResult] but specific for dialogs with an additional state for then the dialog is hidden/empty.
 */
@Parcelize
sealed class DialogState<out T>: Parcelable{

    data object Empty: DialogState<Nothing>()
    data class Success<out T: Parcelable>(val data: T): DialogState<T>()
    data class Error<out T>(val exception: Exception): DialogState<T>()
    data object Loading : DialogState<Nothing>()
}

@Parcelize
sealed class DialogStateList<out T>: Parcelable{

    data object Empty: DialogStateList<Nothing>()
    data class Success<out T: Parcelable>(val data: List<T>): DialogStateList<T>()
    data class Error<out T>(val exception: Exception): DialogStateList<T>()
    data object Loading : DialogStateList<Nothing>()
}

/**
 * Represents the state when audio is being played (e.g. TTS)
 */
sealed class PlayAudioState {

    data object Playing : PlayAudioState()
    data object Stopped : PlayAudioState()
    data class Error(val exception: Exception): PlayAudioState()
    /**
     * State that represents when some TTS engines are initializing or latency from requesting the audio to an API.
     * It can be ignored.
     */
    data object Loading : PlayAudioState()
}

/**
 * Represents the result when trying to get information a word from either local (e.g. database) or remote (e.g. web) sources
 */
sealed class WordResult {
    data class Local(val word: Words): WordResult()
    data class Remote(val translation: Translation): WordResult()
    data class Error(val exception: Exception): WordResult()
}
