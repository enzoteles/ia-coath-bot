package br.com.coin_project_ia_bot.presentation.fragments.multi_tff.safe

import br.com.coin_project_ia_bot.domain.model.Candle

object BacktestUtils {
    fun simulatePriceMovement(candles: List<Candle>, entryPrice: Float, tp: Float, sl: Float): TradeResult {
        for (candle in candles.takeLast(20)) {
            if (candle.high >= tp) return TradeResult.GAIN
            if (candle.low <= sl) return TradeResult.LOSS
        }
        return TradeResult.NONE
    }

    fun determineSafeRiskReward(score: Int): Pair<Float, Float> {
        return when {
            score >= 9 -> Pair(4.5f, 1.5f)
            score in 8..8 -> Pair(3.0f, 1.2f)
            else -> Pair(0f, 0f)
        }
    }

    fun isBreakoutConfirmed(candles: List<Candle>, currentIndex: Int, breakoutLevel: Float): Boolean {
        if (currentIndex + 1 >= candles.size) return false
        val breakoutCandle = candles[currentIndex]
        val nextCandle = candles[currentIndex + 1]
        val brokeAbove = breakoutCandle.close > breakoutLevel
        val bodySize = kotlin.math.abs(nextCandle.close - nextCandle.open)
        val totalRange = nextCandle.high - nextCandle.low
        val isStrongBullish = nextCandle.close > nextCandle.open && totalRange > 0f && (bodySize / totalRange) > 0.6f
        return brokeAbove && isStrongBullish
    }
}