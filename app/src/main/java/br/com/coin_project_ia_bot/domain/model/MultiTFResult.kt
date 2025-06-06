package br.com.coin_project_ia_bot.domain.model

data class MultiTFResult(
    val symbol: String,
    val oneHourChange: Float,
    val trend: String,
    val consistency: String,
    val score: Int,
    val rsi: Float?,
    val bullishCount: Int
)

