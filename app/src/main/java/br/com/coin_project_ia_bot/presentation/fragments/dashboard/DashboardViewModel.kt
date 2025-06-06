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
                        val analysis = analyzeTicker(ticker, closes, parseCandles(candles))
                        if (analysis.score >= 7) {
                            analyses.add(analysis)
                        }
                    }
                }

                val sorted = analyses.sortedByDescending { it.score }
                _analyzedTickers.value = sorted

            } catch (e: Exception) {
                Log.e("ViewModel", "Erro: ${e.message}")
            }
        }
    }
}
