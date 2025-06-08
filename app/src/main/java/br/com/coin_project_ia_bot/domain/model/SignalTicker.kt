package br.com.coin_project_ia_bot.domain.model

data class SignalTicker(
    val symbol: String,
    val variation2h: Float,
    val rsi: Float?,
    val bullishCount: Int,
    val score: Int,
    val consistency: String,
    val trend: String,
    val oneHourChange: Float,
    val takeProfitRange: String, // ex: "3% a 5%"
    val stopLoss: String,        // ex: "-1.5%"
    val timestamp: Long
)
