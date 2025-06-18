package br.com.coin_project_ia_bot.data.model

data class OrderBookResponse(
    val lastUpdateId: Long,
    val bids: List<List<String>>,
    val asks: List<List<String>>
)