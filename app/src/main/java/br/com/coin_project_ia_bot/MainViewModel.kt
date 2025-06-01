package br.com.coin_project_ia_bot

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    val tickersLiveData = MutableLiveData<List<Ticker>>()

    fun fetchAndScoreTickers() {
        viewModelScope.launch {
            val tickers = RetrofitInstance.api.getTickers()

            val scored = tickers
                .filter { it.symbol.endsWith("USDT") } // Só pares com USDT
                .mapNotNull { ticker ->
                    val score = calculateScore(ticker)
                    if (score > 0) Pair(ticker, score) else null
                }
                .sortedByDescending { it.second }

            tickersLiveData.value = scored.map { it.first }
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
}
