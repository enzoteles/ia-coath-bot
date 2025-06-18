package br.com.coin_project_ia_bot.data.api

data class CoinGeckoNewsResponse(
    val status_updates: List<CoinGeckoUpdate>?
)

data class CoinGeckoUpdate(
    val description: String,
    val category: String?,
    val created_at: String
)
