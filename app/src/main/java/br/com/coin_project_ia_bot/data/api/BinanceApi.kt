package br.com.coin_project_ia_bot.data.api

import br.com.coin_project_ia_bot.data.model.Ticker
import retrofit2.http.GET
import retrofit2.http.Query

interface BinanceApi {
    @GET("api/v3/ticker/24hr")
    suspend fun getTickers(): List<Ticker>

    @GET("api/v3/klines")
    suspend fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int
    ): List<List<String>>

}
