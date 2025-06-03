package br.com.coin_project_ia_bot.presentation.fragments.trend

import br.com.coin_project_ia_bot.BinanceApi
import br.com.coin_project_ia_bot.domain.model.TrendAnalysisResult

class TrendAIAnalyzer(private val api: BinanceApi) {

    suspend fun analyze(symbol: String): TrendAnalysisResult {
        val candles1h = api.getKlines(symbol, "1h", 20)
        val candles15m = api.getKlines(symbol, "15m", 20)

        val trend1h = classifyTrend(candles1h)
        val trend15m = classifyTrend(candles15m)

        val explanation = buildExplanation(trend1h, trend15m)

        return TrendAnalysisResult(
            symbol = symbol,
            trend1h = trend1h,
            trend15m = trend15m,
            explanation = explanation
        )
    }

    private fun classifyTrend(candles: List<List<String>>): TrendDirection {
        val closes = candles.mapNotNull { it[4].toFloatOrNull() } // close price
        if (closes.size < 5) return TrendDirection.UNKNOWN

        val diffs = closes.zipWithNext { a, b -> b - a }
        val positives = diffs.count { it > 0 }
        val negatives = diffs.count { it < 0 }

        return when {
            positives >= 4 -> TrendDirection.UP
            negatives >= 4 -> TrendDirection.DOWN
            else -> TrendDirection.SIDEWAYS
        }
    }

    private fun buildExplanation(t1h: TrendDirection, t15m: TrendDirection): String {
        return when {
            t1h == TrendDirection.UP && t15m == TrendDirection.UP -> "Alta consistente nos dois timeframes."
            t1h == TrendDirection.UP && t15m == TrendDirection.SIDEWAYS -> "Tendência de alta no geral, mas com pausa no curto prazo."
            t1h == TrendDirection.DOWN && t15m == TrendDirection.DOWN -> "Queda consistente nos dois timeframes."
            else -> "Tendência indefinida."
        }
    }
}

enum class TrendDirection {
    UP, DOWN, SIDEWAYS, UNKNOWN
}
