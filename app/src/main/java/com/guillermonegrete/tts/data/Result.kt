package com.guillermonegrete.tts.data

import com.guillermonegrete.tts.db.Words
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
    data class Error<out T>(val exception: Exception): LoadResult<T>()
    object Loading : LoadResult<Nothing>()
}

/**
 * Represents the state when audio is being played (e.g. TTS)
 */
sealed class PlayAudioState {

    object Playing : PlayAudioState()
    object Stopped : PlayAudioState()
    data class Error(val exception: Exception): PlayAudioState()
    /**
     * State that represents when some TTS engines are initializing or latency from requesting the audio to an API.
     * It can be ignored.
     */
    object Loading : PlayAudioState()
}

/**
 * Represents the result when trying to get information a word from either local (e.g. database) or remote (e.g. web) sources
 */
sealed class WordResult {
    data class Local(val word: Words): WordResult()
    data class Remote(val translation: Translation): WordResult()
    data class Error(val exception: Exception): WordResult()
}
