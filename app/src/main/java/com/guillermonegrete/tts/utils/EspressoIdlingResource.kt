package com.guillermonegrete.tts.utils

/**
 * Taken from Google's Android Architecture samples:
 * https://github.com/android/architecture-samples/blob/main/app/src/main/java/com/example/android/architecture/blueprints/todoapp/util/EspressoIdlingResource.kt
 */
object EspressoIdlingResource {

    private const val RESOURCE = "GLOBAL"

    @JvmField
    val countingIdlingResource = SimpleCountingIdlingResource(RESOURCE)

    fun increment() {
        countingIdlingResource.increment()
    }

    fun decrement() {
        if (!countingIdlingResource.isIdleNow) {
            countingIdlingResource.decrement()
        }
    }
}

inline fun <T> wrapEspressoIdlingResource(function: () -> T): T {
    // Espresso does not work well with coroutines yet. See
    // https://github.com/Kotlin/kotlinx.coroutines/issues/982
    EspressoIdlingResource.increment() // Set app as busy.
    return try {
        function()
    } finally {
        EspressoIdlingResource.decrement() // Set app as idle.
    }
}