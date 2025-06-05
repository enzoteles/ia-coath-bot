package br.com.coin_project_ia_bot.presentation.fragments.dashboard

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.coin_project_ia_bot.RetrofitInstance
import br.com.coin_project_ia_bot.Ticker
import br.com.coin_project_ia_bot.presentation.MainActivity
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {
    val tickersLiveData = MutableLiveData<List<Ticker>>()
    private val _analyzedTickers = MutableLiveData<List<TickerAnalysis>>()
    val analyzedTickers: LiveData<List<TickerAnalysis>> = _analyzedTickers


    fun fetchAndScoreTickers() {
        viewModelScope.launch {
            try {
                val tickers = RetrofitInstance.api.getTickers()
                    .filter { it.symbol.endsWith(MainActivity.USDT) }

                val analyses = mutableListOf<TickerAnalysis>()

                for (ticker in tickers) {
                    val closes = getClosesForTicker(ticker.symbol)
                    val candles = getCandlesForTicker(ticker.symbol)

                    if (closes != null && candles != null) {
                        val analysis = analyzeTicker(ticker, closes, candles)
                        if (analysis.score > 0) {
                            analyses.add(analysis)
                        }
                    }
                }

                val sorted = analyses.sortedByDescending { it.score }

                _analyzedTickers.value = sorted // LiveData<List<TickerAnalysis>>

            } catch (e: Exception) {
                Log.e("ViewModel", "Erro: ${e.message}")
            }
        }
    }


    private fun calculateScore(ticker: Ticker): Int {
        val change = ticker.priceChangePercent.toFloatOrNull() ?: return 0
        val volume = ticker.volume.toFloatOrNull() ?: return 0
        val price = ticker.lastPrice.toFloatOrNull() ?: return 0

        val priceRange = (ticker.highPrice.toFloatOrNull() ?: 0f) -
                (ticker.lowPrice.toFloatOrNull() ?: 0f)

        // Regras para pontuar
        return when {
            change >= 2 && change <= 5 && volume > 500000 -> 5
            change > 5 && priceRange / price > 0.03 -> 10 // possível pump
            else -> 0
        }
    }

    fun analyzeAllTickers() {
        viewModelScope.launch {
            val tickers = RetrofitInstance.api.getTickers()
            val usdtTickers = tickers.filter { it.symbol.endsWith("USDT") }

            val analyzed = usdtTickers.mapNotNull { ticker ->
                val closes = getClosesForTicker(ticker.symbol)
                val candles = getCandlesForTicker(ticker.symbol)
                if (closes != null && candles != null) {
                    analyzeTicker(ticker, closes, candles)
                } else null
            }.sortedByDescending { it.score }

            _analyzedTickers.postValue(analyzed)
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

    private suspend fun getCandlesForTicker(symbol: String): List<List<String>>? {
        return try {
            RetrofitInstance.api.getKlines(symbol, "1h", 50)
        } catch (e: Exception) {
            null
        }
    }
}
