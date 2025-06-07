package br.com.coin_project_ia_bot.presentation.fragments.dashboard

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.coin_project_ia_bot.RetrofitInstance
import br.com.coin_project_ia_bot.Ticker
import br.com.coin_project_ia_bot.presentation.MainActivity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val _analyzedTickers = MutableLiveData<List<TickerAnalysis>>()
    val analyzedTickers: LiveData<List<TickerAnalysis>> = _analyzedTickers

    fun fetchAndScoreTickers(minScore: Int = 7) {
        viewModelScope.launch {
            try {
                val tickers = RetrofitInstance.api.getTickers()
                    .filter { it.symbol.endsWith("USDT") }

                val analyses = coroutineScope {
                    tickers.map { ticker ->
                        async {
                            try {
                                val closes = getClosesForTicker(ticker.symbol)
                                val candles = getCandlesForTicker(ticker.symbol)
                                if (closes != null && candles != null) {
                                    val parsedCandles = parseCandles(candles)
                                    val analysis = analyzeTicker(ticker, closes, parsedCandles)
                                    if (analysis.score >= minScore) analysis else null
                                } else null
                            } catch (e: Exception) {
                                Log.w("TickerFail", "Erro ao analisar ${ticker.symbol}: ${e.message}")
                                null
                            }
                        }
                    }.awaitAll().filterNotNull()
                }

                _analyzedTickers.value = analyses.sortedByDescending { it.score }

            } catch (e: Exception) {
                Log.e("ViewModel", "Falha geral: ${e.message}")
            }
        }
    }
}
