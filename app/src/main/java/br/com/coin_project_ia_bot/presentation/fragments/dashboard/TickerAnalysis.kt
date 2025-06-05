package br.com.coin_project_ia_bot.presentation.fragments.dashboard

import br.com.coin_project_ia_bot.Ticker
import br.com.coin_project_ia_bot.domain.model.Candle

data class TickerAnalysis(
    val ticker: Ticker,
    val score: Int,
    val rsi: Float?,
    val bullishCount: Int,
    val change: Float
)

fun analyzeTicker(
    ticker: Ticker,
    closes: List<Float>,
    candles: List<List<String>>?
): TickerAnalysis {
    val change = ticker.priceChangePercent.toFloatOrNull() ?: return TickerAnalysis(ticker, 0, null, 0, 0f)
    val volume = ticker.volume.toFloatOrNull() ?: return TickerAnalysis(ticker, 0, null, 0, change)
    val price = ticker.lastPrice.toFloatOrNull() ?: return TickerAnalysis(ticker, 0, null, 0, change)
    val high = ticker.highPrice.toFloatOrNull() ?: 0f
    val low = ticker.lowPrice.toFloatOrNull() ?: 0f
    val priceRange = if (price != 0f) (high - low) / price else 0f

    val rsi = calculateRSI(closes)
    val bullishCount = countBullishCandles(parseCandles(candles!!) )

    var score = 0

    // Variação positiva
    if (change >= 2f && change <= 5f) score += 3
    else if (change > 5f) score += 4

    // Volume relevante
    if (volume > 1_000_000f) score += 2
    else if (volume > 500_000f) score += 1

    // RSI mostrando força compradora
    if (rsi != null) {
        if (rsi in 55f..70f) score += 1
        else if (rsi > 70f) score += 2
    }

    // Candles de alta
    if (bullishCount >= 3) score += 1
    if (bullishCount >= 5) score += 2

    // Amplitude de preço
    if (priceRange > 0.03f) score += 1

    return TickerAnalysis(
        ticker = ticker,
        score = score.coerceAtMost(10),
        rsi = rsi,
        bullishCount = bullishCount,
        change = change
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
    return rawKlines.mapNotNull { kline ->
        try {
            if (kline.size >= 6) {
                val open = kline[1].toFloat()
                val high = kline[2].toFloat()
                val low = kline[3].toFloat()
                val close = kline[4].toFloat()
                val volume = kline[5].toFloat()

                Candle(open, high, low, close, volume)
            } else null
        } catch (e: Exception) {
            null // ignora candles malformados
        }
    }
}





