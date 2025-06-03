package br.com.coin_project_ia_bot.domain.model

data class Candle(
    val open: Float,
    val close: Float,
    val high: Float,
    val low: Float,
    val volume: Float
)
