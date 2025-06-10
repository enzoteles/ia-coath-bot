package br.com.coin_project_ia_bot.presentation.fragments.signal

import br.com.coin_project_ia_bot.data.api.BinanceApi
import br.com.coin_project_ia_bot.domain.model.Candle

class CoinAnalyzer(private val api: BinanceApi) {

    suspend fun getCandles(symbol: String, interval: String, limit: Int): List<Candle> {
        return try {
            api.getKlines(symbol, interval, limit).map {
                Candle(
                    open = it[1].toFloat(),
                    close = it[4].toFloat(),
                    high = it[2].toFloat(),
                    low = it[3].toFloat(),
                    volume = it[5].toFloat()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isUptrend(candles: List<Candle>): Boolean {
        return candles.all { it.close > it.open }
    }

    fun variationPercent(candles: List<Candle>): Float {
        if (candles.isEmpty()) return 0f
        val first = candles.first().open
        val last = candles.last().close
        return ((last - first) / first) * 100
    }

    fun isVolumeIncreasing(candles: List<Candle>): Boolean {
        val volumes = candles.map { it.volume }
        return volumes == volumes.sorted()
    }
}
