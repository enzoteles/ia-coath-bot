package br.com.coin_project_ia_bot.domain.model

data class Recommendation(
    val symbol: String,
    val changePercent: Float,
    val reason: String
)
