package br.com.coin_project_ia_bot.domain.model

data class TradeParameters(
    val symbol: String,
    val takeProfitPercent: Float,
    val stopLossPercent: Float
)