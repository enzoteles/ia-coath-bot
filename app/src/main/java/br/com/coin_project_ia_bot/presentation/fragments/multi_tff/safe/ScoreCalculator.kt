package br.com.coin_project_ia_bot.presentation.fragments.multi_tff.safe

import br.com.coin_project_ia_bot.data.model.Ticker

object ScoreCalculator {

    fun estimateAIScoreWithEMA(
        closes: List<Float>,
        rsi: Float?,
        bullishCount: Int
    ): Int {
        var score = 0
        val change = if (closes.size >= 2) {
            ((closes.last() - closes[closes.size - 2]) / closes[closes.size - 2]) * 100
        } else 0f

        if (change in 2f..5f) score += 3
        if (change > 5f) score += 4
        if (rsi != null && rsi in 55f..68f) score += 2
        if (bullishCount >= 3) score += 2

        val ema9 = TAUtils.calculateEMA(closes, 9)
        val ema21 = TAUtils.calculateEMA(closes, 21)
        if (ema9.isNotEmpty() && ema21.isNotEmpty() && ema9.last() > ema21.last()) {
            score += 2
        }

        return score.coerceAtMost(10)
    }

    fun adjustScoreBasedOnHistory(
        symbol: String,
        originalScore: Int,
        performancePorPar: Map<String, Pair<Int, Int>>
    ): Int {
        val (acertos, total) = performancePorPar[symbol] ?: Pair(0, 0)
        if (total < 5) return originalScore
        val taxaAcerto = (acertos.toFloat() / total) * 100

        return when {
            taxaAcerto >= 70 -> originalScore + 1
            taxaAcerto < 50 -> originalScore - 1
            else -> originalScore
        }.coerceIn(0, 10)
    }

    fun estimateConsistencyAI(score: Int, rsi: Float?, bullishCount: Int): String {
        return when {
            score >= 9 && rsi!! in 60f..68f && bullishCount >= 5 -> "🚀 Forte Tendência Confirmada"
            score in 8..8 && rsi!! in 55f..65f -> "📈 Boa Tendência"
            else -> "🔍 Potencial Emergente"
        }
    }

    fun estimateConsistency(ticker: Ticker): String {
        val change = ticker.priceChangePercent.toFloatOrNull() ?: 0f
        return when {
            change > 5 -> "Tendência Forte ↑"
            change > 1 -> "Tendência Moderada ↑"
            change < -1 -> "Tendência Baixa ↓"
            else -> "Estável"
        }
    }


}