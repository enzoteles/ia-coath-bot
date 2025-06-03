package br.com.coin_project_ia_bot.domain.model

import br.com.coin_project_ia_bot.presentation.fragments.trend.TrendDirection

data class TrendAnalysisResult(
    val symbol: String,
    val trend1h: TrendDirection,
    val trend15m: TrendDirection,
    val explanation: String
)