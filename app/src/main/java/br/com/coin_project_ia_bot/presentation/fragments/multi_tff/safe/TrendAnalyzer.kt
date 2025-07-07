package br.com.coin_project_ia_bot.presentation.fragments.multi_tff.safe

import br.com.coin_project_ia_bot.domain.model.Candle
import br.com.coin_project_ia_bot.presentation.utils.calculateRSI
import br.com.coin_project_ia_bot.presentation.utils.getCandlesForTicker
import br.com.coin_project_ia_bot.presentation.utils.parseCandles

object TrendAnalyzer {
    fun calcularTendenciaGeral(candles: List<Candle>): String {
        if (candles.size < 20) return "NEUTRA"
        val ultimas = candles.takeLast(20)
        val primeiroClose = ultimas.first().close
        val ultimoClose = ultimas.last().close
        val movimentoPercentual = ((ultimoClose - primeiroClose) / primeiroClose) * 100
        val bullishCount = ultimas.count { it.close > it.open }
        val bearishCount = ultimas.count { it.open > it.close }
        return when {
            movimentoPercentual > 1.5f && bullishCount >= 12 -> "ALTISTA"
            movimentoPercentual < -1.5f && bearishCount >= 12 -> "BAIXISTA"
            else -> "NEUTRA"
        }
    }

    suspend fun isMultiTimeframeConfluence(symbol: String): Boolean {
        val h1Candles = getCandlesForTicker(symbol, interval = "1h", limit = 50) ?: return false
        val h4Candles = getCandlesForTicker(symbol, interval = "4h", limit = 50) ?: return false
        val h1Trend = calcularTendenciaGeral(parseCandles(h1Candles))
        val h4Trend = calcularTendenciaGeral(parseCandles(h4Candles))
        return h1Trend == "ALTISTA" && h4Trend == "ALTISTA"
    }

    suspend fun getBTCTendency(): String {
        val btcCandlesRaw = getCandlesForTicker("BTCUSDT") ?: return "NEUTRA"
        val btcCandles = parseCandles(btcCandlesRaw)
        if (btcCandles.size < 21) return "NEUTRA"
        val closes = btcCandles.map { it.close }
        val ema9 = TAUtils.calculateEMA(closes, 9)
        val ema21 = TAUtils.calculateEMA(closes, 21)
        val rsi = calculateRSI(closes)
        val bullishCandles = btcCandles.takeLast(10).count { it.close > it.open }
        var score = 0
        if (ema9.last() > ema21.last()) score++
        if (rsi != null && rsi > 50f) score++
        if (bullishCandles >= 6) score++
        return when {
            score >= 2 -> "ALTISTA"
            score == 1 -> "NEUTRA"
            else -> "BAIXISTA"
        }
    }
}