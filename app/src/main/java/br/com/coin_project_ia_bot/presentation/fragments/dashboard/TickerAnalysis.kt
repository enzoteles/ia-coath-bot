package br.com.coin_project_ia_bot.presentation.fragments.dashboard

import android.util.Log
import br.com.coin_project_ia_bot.RetrofitInstance
import br.com.coin_project_ia_bot.data.model.Ticker
import br.com.coin_project_ia_bot.domain.model.Candle


data class TickerAnalysis(
    val ticker: Ticker,
    val score: Float,
    val rsi: Float?,
    val bullishCount: Int,
    val change: Float,
    val consistency: String
) {
    val symbol: String get() = ticker.symbol
}

fun analyzeTicker(
    ticker: Ticker,
    closes: List<Float>,
    candles: List<Candle>
): TickerAnalysis? {
    val symbol = ticker.symbol

    if (closes.size < 15) {
        Log.w("Analyzer", "Closes insuficientes para $symbol")
        return null
    }

    if (candles.size < 5) {
        Log.w("Analyzer", "Candles insuficientes para $symbol")
        return null
    }

    val change = ticker.priceChangePercent.toFloatOrNull() ?: return null
    val volume = ticker.volume.toFloatOrNull() ?: return null
    val price = ticker.lastPrice.toFloatOrNull() ?: return null
    val high = ticker.highPrice.toFloatOrNull() ?: 0f
    val low = ticker.lowPrice.toFloatOrNull() ?: 0f
    val priceRange = if (price != 0f) (high - low) / price else 0f

    val rsi = calculateRSI(closes)
    val bullishCount = countBullishCandles(candles)
    val consistency = calculateConsistency(rsi, bullishCount, change, volume)

    var score = 0f

    // Variação de preço
    score += when {
        change in 2f..5f -> 2.5f
        change > 5f -> 3.5f
        else -> 0f
    }

    // Volume
    score += when {
        volume > 1_000_000f -> 2.5f
        volume > 500_000f -> 1.5f
        else -> 0f
    }

    // RSI
    if (rsi != null) {
        score += when {
            rsi in 55f..70f -> 1.5f
            rsi > 70f -> 2.5f
            else -> 0f
        }
    }

    // Candles de alta
    score += when {
        bullishCount >= 5 -> 2.0f
        bullishCount >= 3 -> 1.0f
        else -> 0f
    }

    // Amplitude do preço
    if (priceRange > 0.03f) score += 0.5f

    return TickerAnalysis(
        ticker = ticker,
        score = score,
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
                        kotlin.math.abs(close - open) > (0.005f * open)

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

suspend fun getCandlesForTicker(symbol: String, interval: String = "1h", limit: Int = 50): List<List<String>>? {
    return try {
        RetrofitInstance.api.getKlines(symbol, interval, limit)
    } catch (e: Exception) {
        null
    }
}

suspend fun getClosesForTicker(symbol: String, interval: String = "1h", limit: Int = 50): List<Float> {
    return try {
        RetrofitInstance.api.getKlines(symbol, interval, limit)
            .mapNotNull { (it.getOrNull(4))?.toFloatOrNull() }
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

fun estimateAIScore(rsi: Float?, bullishCount: Int, change: Float, volume: Float): Int {
    var score = 0
    if (rsi != null && rsi in 55f..70f) score += 2
    if (bullishCount >= 3) score += 2
    if (change >= 2f) score += 2
    if (volume > 500_000f) score += 2
    return score
}

fun extractVolume(candles: List<Candle>): Float {
    return 10f
}

fun isUptrend(candles: List<Candle>, minUpCandles: Int = 3): Boolean {
    return candles.takeLast(minUpCandles).count { it.close > it.open } >= minUpCandles
}

fun variationPercent(candles: List<Candle>): Float {
    if (candles.size < 2) return 0f
    val first = candles.first().close
    val last = candles.last().close
    return ((last - first) / first) * 100f
}

fun isVolumeIncreasing(candles: List<Candle>): Boolean {
    val volumes = candles.takeLast(10).map { it.volume }
    return volumes.windowed(3, 1).any { it[2] > it[1] && it[1] > it[0] }
}

fun estimateConsistencyAI(score: Int, rsi: Float?, bullishCount: Int): String {
    return when {
        score >= 9 && rsi != null && rsi in 60f..70f && bullishCount >= 5 -> "🚀 Forte Tendência Confirmada"
        score in 7..8 && rsi != null && rsi in 55f..65f -> "📈 Boa Tendência"
        score in 5..6 -> "🔍 Potencial Emergente"
        else -> "⚠️ Baixa Confiabilidade"
    }
}
