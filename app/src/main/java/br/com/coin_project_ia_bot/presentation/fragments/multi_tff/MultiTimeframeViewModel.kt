package br.com.coin_project_ia_bot.presentation.fragments.multi_tff

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.coin_project_ia_bot.RetrofitInstance
import kotlinx.coroutines.launch

class MultiTimeframeViewModel : ViewModel() {

    private val _multiTimeData = MutableLiveData<List<MultiTimeTicker>>()
    val multiTimeData: LiveData<List<MultiTimeTicker>> = _multiTimeData

    private val binanceApi = RetrofitInstance.api

    fun loadMultiTimeData(symbols: List<String> = listOf("BTCUSDT")) {
        viewModelScope.launch {
            val results = mutableListOf<MultiTimeTicker>()

            for (symbol in symbols) {
                try {
                    val intervals = listOf("1m", "5m", "30m", "1h")
                    val klinesMap = mutableMapOf<String, List<List<Any>>>()

                    // Carregar os dados de klines para todos os intervalos
                    for (interval in intervals) {
                        val klines = binanceApi.getKlines(symbol, interval, 4)
                        klinesMap[interval] = klines
                    }

                    val ticker = MultiTimeTicker(
                        symbol = symbol,
                        change1m = calculateChange(klinesMap["1m"] ?: emptyList()),
                        change5m = calculateChange(klinesMap["5m"] ?: emptyList()),
                        change30m = calculateChange(klinesMap["30m"] ?: emptyList()),
                        change1h = calculateChange(klinesMap["1h"] ?: emptyList()),
                        consistentUpTrend = checkConsistentUpTrend(klinesMap["1h"] ?: emptyList())
                    )

                    results.add(ticker)

                } catch (e: Exception) {
                    Log.e("MultiTimeData", "Erro ao carregar dados para $symbol: ${e.message}")
                }
            }

            _multiTimeData.postValue(results)
        }
    }

    fun loadMultiTimeDataFromTopSymbols() {
        viewModelScope.launch {
            try {
                // Busca os top 10 símbolos com maior volume
                val topTickers = binanceApi.getTickers()
                    .filter { it.symbol.endsWith("USDT") && !it.symbol.contains("UP") && !it.symbol.contains("DOWN") }
                    .sortedByDescending { it.quoteVolume.toDoubleOrNull() ?: 0.0 }
                    .take(10)

                val results = mutableListOf<MultiTimeTicker>()

                for (ticker in topTickers) {
                    val symbol = ticker.symbol
                    try {
                        val klines1m = binanceApi.getKlines(symbol, "1m", 4)
                        val klines5m = binanceApi.getKlines(symbol, "5m", 4)
                        val klines15m = binanceApi.getKlines(symbol, "15m", 4)
                        val klines1h = binanceApi.getKlines(symbol, "1h", 4)

                        val scoredTicker = MultiTimeTicker(
                            symbol = symbol,
                            change1m = calculateChange(klines1m),
                            change5m = calculateChange(klines5m),
                            change30m = calculateChange(klines15m),
                            change1h = calculateChange(klines1h),
                            consistentUpTrend = checkConsistentUpTrend(klines1h)
                        )

                        results.add(scoredTicker)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                _multiTimeData.postValue(results)

            } catch (e: Exception) {
                e.printStackTrace()
                _multiTimeData.postValue(emptyList())
            }
        }
    }



    fun calculateChange(klines: List<List<Any>>): Float {
        return try {
            if (klines.isEmpty()) return 0f

            val firstCandle = klines.first()
            val lastCandle = klines.last()

            val openPrice = (firstCandle[1] as String).toFloatOrNull() ?: return 0f
            val closePrice = (lastCandle[4] as String).toFloatOrNull() ?: return 0f

            if (openPrice == 0f) return 0f

            ((closePrice - openPrice) / openPrice) * 100f
        } catch (e: Exception) {
            0f
        }
    }


    fun checkConsistentUpTrend(klines: List<List<Any>>, candlesToCheck: Int = 3): Boolean {
        if (klines.size < candlesToCheck) return false

        return klines.takeLast(candlesToCheck).all { candle ->
            val open = (candle[1] as String).toFloatOrNull() ?: return false
            val close = (candle[4] as String).toFloatOrNull() ?: return false
            close > open
        }
    }

}

data class MultiTimeTicker(
    val symbol: String,
    val change1m: Float,
    val change5m: Float,
    val change30m: Float,
    val change1h: Float,
    val consistentUpTrend: Boolean
)
