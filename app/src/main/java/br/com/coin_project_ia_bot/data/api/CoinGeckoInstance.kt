package br.com.coin_project_ia_bot.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object CoinGeckoInstance {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.coingecko.com/api/v3/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: CoinGeckoApiService = retrofit.create(CoinGeckoApiService::class.java)
}
