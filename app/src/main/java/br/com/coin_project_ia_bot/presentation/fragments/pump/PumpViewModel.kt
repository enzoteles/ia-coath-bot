package br.com.coin_project_ia_bot.presentation.fragments.pump

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.coin_project_ia_bot.BinanceApi
import br.com.coin_project_ia_bot.domain.model.PumpTicker
import br.com.coin_project_ia_bot.presentation.fragments.signal.CoinAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PumpViewModel(private val api: BinanceApi) : ViewModel() {

    private val analyzer = CoinAnalyzer(api)
    private val _pumpList = MutableLiveData<List<PumpTicker>>()
    val pumpList: LiveData<List<PumpTicker>> = _pumpList

    fun fetchPumpCoins(pairs: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val results = mutableListOf<PumpTicker>()
            for (symbol in pairs) {
                try {
                    val candles = analyzer.getCandles(symbol, "1m", 120)
                    val change = analyzer.variationPercent(candles)
                    if (change >= 10f) {
                        results.add(PumpTicker(symbol, change))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            _pumpList.postValue(results)
        }
    }
}
