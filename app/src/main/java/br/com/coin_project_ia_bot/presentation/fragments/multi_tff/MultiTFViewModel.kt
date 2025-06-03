package br.com.coin_project_ia_bot.presentation.fragments.multi_tff

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.coin_project_ia_bot.RetrofitInstance
import br.com.coin_project_ia_bot.Ticker
import br.com.coin_project_ia_bot.domain.model.MultiTFResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MultiTFViewModel : ViewModel() {

    val multiTFData = MutableLiveData<List<MultiTFResult>>()

    fun startAutoUpdate(intervalMillis: Long) {
        viewModelScope.launch {
            while (true) {
                val result = fetchMultiTFAnalysis()
                multiTFData.postValue(result)
                delay(intervalMillis)
            }
        }
    }

    private suspend fun fetchMultiTFAnalysis(): List<MultiTFResult> {
        val tickers = RetrofitInstance.api.getTickers()
        val usdtPairs = tickers.filter { it.symbol.endsWith("USDT") }

        return usdtPairs.map { ticker ->
            val oneHourChange = ticker.priceChangePercent.toFloatOrNull() ?: 0f
            val consistency = estimateConsistency(ticker)
            MultiTFResult(ticker.symbol, oneHourChange, consistency)
        }.sortedByDescending { it.oneHourChange }
    }

    private fun estimateConsistency(ticker: Ticker): String {
        val change = ticker.priceChangePercent.toFloatOrNull() ?: 0f
        return when {
            change > 5 -> "Tendência Forte ↑"
            change > 1 -> "Tendência Moderada ↑"
            change < -1 -> "Tendência Baixa ↓"
            else -> "Estável"
        }
    }
}
