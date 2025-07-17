package br.com.coin_project_ia_bot.presentation.fragments.multi_tff.strategy

import br.com.coin_project_ia_bot.presentation.utils.TickerAnalysis

object MarketConditionDetector {

    fun detectarCondicao(analises: List<TickerAnalysis>): String {
        val mediaVariação = analises.mapNotNull {
            it.ticker.priceChangePercent.toFloatOrNull()
        }.average()

        return when {
            mediaVariação >= 2.5 -> "Alta"
            mediaVariação <= -2.5 -> "Queda"
            else -> "Lateral"
        }
    }
}
