package br.com.coin_project_ia_bot

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Interceptor para logar requests/responses no Logcat
private val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
}

// Cliente OkHttp com interceptor
private val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(loggingInterceptor)
    .build()

object RetrofitInstance {
    val api: BinanceApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.binance.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BinanceApi::class.java)
    }
}

