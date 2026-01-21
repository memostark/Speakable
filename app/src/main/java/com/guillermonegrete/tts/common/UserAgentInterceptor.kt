package com.guillermonegrete.tts.common

import com.guillermonegrete.tts.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

class UserAgentInterceptor: Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        request = request.newBuilder()
            .header("User-Agent", "Speakable/${BuildConfig.VERSION_NAME}")
            .build()

        return chain.proceed(request)
    }
}
