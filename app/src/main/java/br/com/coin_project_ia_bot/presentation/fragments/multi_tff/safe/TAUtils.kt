package br.com.coin_project_ia_bot.presentation.fragments.multi_tff.safe

import br.com.coin_project_ia_bot.domain.model.Candle
import kotlin.math.abs

object TAUtils {
    fun calculateEMA(closes: List<Float>, period: Int): List<Float> {
        val ema = mutableListOf<Float>()
        val k = 2f / (period + 1)
        var previousEMA = closes.take(period).average().toFloat()
        for (price in closes.drop(period)) {
            val currentEMA = (price - previousEMA) * k + previousEMA
            ema.add(currentEMA)
            previousEMA = currentEMA
        }
        return ema
    }

    fun calculateATR(candles: List<Candle>, period: Int = 14): Float {
        if (candles.size < period + 1) return 0f
        var atr = 0f
        for (i in 1..period) {
            val high = candles[candles.size - i].high
            val low = candles[candles.size - i].low
            val closePrev = candles[candles.size - i - 1].close
            val tr = maxOf(high - low, abs(high - closePrev), abs(low - closePrev))
            atr += tr
        }
        return atr / period
    }

     fun calculateDynamicStopLoss(candles: List<Candle>, atrMultiplier: Float = 1.5f): Float {
        val atr = calculateATR(candles)
        return atr * atrMultiplier
    }

    fun calculateDynamicATRLimit(candles: List<Candle>): Float {
        val avgClose = candles.map { it.close }.average().toFloat()
        val atr = calculateATR(candles)
        return (atr / avgClose).coerceAtMost(0.05f) // nunca deixar passar de 5%
    }

}