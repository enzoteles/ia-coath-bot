package br.com.coin_project_ia_bot.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface CoinGeckoApiService {
    @GET("coins/{id}/status_updates")
    suspend fun getStatusUpdates(@Path("id") coinId: String): Response<CoinGeckoNewsResponse>
}
