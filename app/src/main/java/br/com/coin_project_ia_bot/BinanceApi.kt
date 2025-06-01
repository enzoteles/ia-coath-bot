package br.com.coin_project_ia_bot

import retrofit2.Response
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

    /*@GET("api/v3/klines")
    suspend fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 10
    ): Response<List<List<String>>>*/


}
