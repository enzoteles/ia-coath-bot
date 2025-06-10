package br.com.coin_project_ia_bot.data.repository

import br.com.coin_project_ia_bot.data.api.BinanceApi
import br.com.coin_project_ia_bot.data.model.Ticker
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BinanceApiImpl : BinanceApi {

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.binance.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(BinanceApi::class.java)

    override suspend fun getTickers(): List<Ticker> {
        return service.getTickers()
    }

    override suspend fun getKlines(symbol: String, interval: String, limit: Int): List<List<String>> {
        return service.getKlines(symbol, interval, limit)
    }
}
