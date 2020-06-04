package com.guillermonegrete.tts.data

import java.lang.Exception

sealed class Result<out T> {

    data class Success<out T>(val data: T): Result<T>()
    // In case you use Java you need to explicitly use <out T>
    // If you use Kotlin only code you can simply use:
    // data class Error(val exception: Exception): Result<Nothing>()
    data class Error<out T>(val exception: Exception): Result<T>()
}