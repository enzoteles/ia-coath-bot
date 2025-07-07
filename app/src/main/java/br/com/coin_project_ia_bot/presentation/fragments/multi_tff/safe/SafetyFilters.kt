package br.com.coin_project_ia_bot.presentation.fragments.multi_tff.safe

import br.com.coin_project_ia_bot.domain.model.Candle

object SafetyFilters {
    fun isHealthyCandle(candle: Candle): Boolean {
        val totalRange = candle.high - candle.low
        val bodySize = kotlin.math.abs(candle.close - candle.open)
        return totalRange > 0f && (bodySize / totalRange) >= 0.3f
    }

    fun isNearSupportOrResistance(candles: List<Candle>, currentPrice: Float): Boolean {
        val recentHighs = candles.takeLast(50).map { it.high }
        val recentLows = candles.takeLast(50).map { it.low }
        val resistance = recentHighs.maxOrNull() ?: return false
        val support = recentLows.minOrNull() ?: return false
        val distRes = ((resistance - currentPrice) / currentPrice) * 100
        val distSup = ((currentPrice - support) / currentPrice) * 100
        return distRes <= 1.0f || distSup <= 1.0f
    }

    fun isVolumeExploding(candles: List<Candle>): Boolean {
        if (candles.size < 21) return false
        val recentVolumes = candles.takeLast(21).dropLast(1).map { it.volume }
        val avgVolume = recentVolumes.average().toFloat()
        return candles.last().volume >= avgVolume * 1.5f
    }
}