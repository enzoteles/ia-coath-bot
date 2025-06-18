package br.com.coin_project_ia_bot.data.model

data class BacktestTradeResult(
    val symbol: String,
    val result: String, // "GAIN" ou "LOSS"
    val entryPrice: Float,
    val takeProfit: Float,
    val stopLoss: Float
)
