package br.com.coin_project_ia_bot.presentation.fragments.dashboard

import android.util.Log
import br.com.coin_project_ia_bot.RetrofitInstance
import br.com.coin_project_ia_bot.Ticker
import br.com.coin_project_ia_bot.domain.model.Candle

data class TickerAnalysis(
    val ticker: Ticker,
    val score: Float, // alterado para Float para melhor precisão
    val rsi: Float?,
    val bullishCount: Int,
    val change: Float,
    val consistency: String? = null
)

fun analyzeTicker(
    ticker: Ticker,
    closes: List<Float>,
    candles: List<Candle>
): TickerAnalysis {
    val change = ticker.priceChangePercent.toFloatOrNull() ?: return TickerAnalysis(ticker, 0f, null, 0, 0f)
    val volume = ticker.volume.toFloatOrNull() ?: return TickerAnalysis(ticker, 0f, null, 0, change)
    val price = ticker.lastPrice.toFloatOrNull() ?: return TickerAnalysis(ticker, 0f, null, 0, change)
    val high = ticker.highPrice.toFloatOrNull() ?: 0f
    val low = ticker.lowPrice.toFloatOrNull() ?: 0f
    val priceRange = if (price != 0f) (high - low) / price else 0f

    val rsi = calculateRSI(closes)
    val bullishCount = countBullishCandles(candles)
    val consistency = calculateConsistency(rsi, bullishCount, change, volume)

    var score = 0f

    // Pesos inteligentes para aumentar confiabilidade
    if (change in 2f..5f) score += 2.5f
    else if (change > 5f) score += 3.5f

    if (volume > 1_000_000f) score += 2.5f
    else if (volume > 500_000f) score += 1.5f

    if (rsi != null) {
        if (rsi in 55f..70f) score += 1.5f
        else if (rsi > 70f) score += 2.5f
    }

    if (bullishCount >= 3) score += 1.0f
    if (bullishCount >= 5) score += 2.0f

    if (priceRange > 0.03f) score += 0.5f

    return TickerAnalysis(
        ticker = ticker,
        score = score.coerceAtMost(10f),
        rsi = rsi,
        bullishCount = bullishCount,
        change = change,
        consistency = consistency
    )
}

fun calculateRSI(closes: List<Float>, period: Int = 14): Float? {
    if (closes.size <= period) return null

    var gain = 0f
    var loss = 0f

    for (i in 1..period) {
        val change = closes[i] - closes[i - 1]
        if (change >= 0) gain += change else loss -= change
    }

    val averageGain = gain / period
    val averageLoss = loss / period
    if (averageLoss == 0f) return 100f

    val rs = averageGain / averageLoss
    return 100 - (100 / (1 + rs))
}

fun countBullishCandles(candles: List<Candle>, limit: Int = 5): Int {
    return candles.takeLast(limit).count { it.close > it.open }
}

fun parseCandles(rawKlines: List<List<String>>): List<Candle> {
    return rawKlines.mapIndexedNotNull { index, kline ->
        try {
            if (kline.size >= 6) {
                val open = kline[1].toFloatOrNull()
                val high = kline[2].toFloatOrNull()
                val low = kline[3].toFloatOrNull()
                val close = kline[4].toFloatOrNull()
                val volume = kline[5].toFloatOrNull()

                val isValid = open != null && high != null && low != null && close != null && volume != null &&
                        volume > 1000f &&
                        high >= low && open != close &&
                        close > 0 && open > 0

                val isStrongCandle = open != null && close != null &&
                        kotlin.math.abs(close - open) > (0.005f * close)

                if (isValid && isStrongCandle) {
                    Candle(open!!, high!!, low!!, close!!, volume!!)
                } else {
                    Log.w("parseCandles", "Candle inválido[$index]: $kline")
                    null
                }
            } else {
                Log.w("parseCandles", "Candle com tamanho inválido[$index]: $kline")
                null
            }
        } catch (e: Exception) {
            Log.e("parseCandles", "Erro ao processar candle[$index]: ${e.message}")
            null
        }
    }
}

suspend fun getCandlesForTicker(symbol: String): List<List<String>>? {
    return try {
        RetrofitInstance.api.getKlines(symbol, "1h", 50)
    } catch (e: Exception) {
        null
    }
}

suspend fun getClosesForTicker(symbol: String): List<Float> {
    return try {
        RetrofitInstance.api.getKlines(symbol, "1h", 50)
            .mapNotNull { (it.getOrNull(4) as? String)?.toFloatOrNull() }
    } catch (e: Exception) {
        emptyList()
    }
}

fun calculateConsistency(
    rsi: Float?,
    bullishCount: Int,
    change: Float,
    volume: Float
): String {
    return when {
        (rsi != null && rsi in 55f..70f) &&
                bullishCount >= 3 &&
                change in 2f..5f &&
                volume > 500_000 -> "Alta Consistência ✅"

        (rsi != null && rsi in 50f..55f) &&
                bullishCount >= 2 &&
                change > 1.5f &&
                volume > 250_000 -> "Consistência Moderada ⚠️"

        else -> "Baixa Consistência ⚠️"
    }
}
