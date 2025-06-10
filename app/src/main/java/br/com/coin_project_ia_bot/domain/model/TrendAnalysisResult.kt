package br.com.coin_project_ia_bot.domain.model

import br.com.coin_project_ia_bot.presentation.utils.TrendDirection

data class TrendAnalysisResult(
    val symbol: String,
    val trend1h: TrendDirection,
    val trend15m: TrendDirection,
    val explanation: String
)