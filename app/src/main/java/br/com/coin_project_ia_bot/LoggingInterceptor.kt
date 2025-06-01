package br.com.coin_project_ia_bot

import okhttp3.Interceptor
import okhttp3.Response
import android.util.Log

class LoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val responseBody = response.peekBody(Long.MAX_VALUE).string()
        return response
    }
}
