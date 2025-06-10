package br.com.coin_project_ia_bot.presentation.fragments.pump

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.coin_project_ia_bot.data.api.BinanceApi
import br.com.coin_project_ia_bot.RetrofitInstance
import br.com.coin_project_ia_bot.domain.model.PumpTicker
import br.com.coin_project_ia_bot.presentation.MainActivity.Companion.USDT
import br.com.coin_project_ia_bot.presentation.fragments.signal.CoinAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val QTD_COIN = 150
class PumpViewModel(private val api: BinanceApi) : ViewModel() {

    private val analyzer = CoinAnalyzer(api)
    private val _pumpList = MutableLiveData<List<PumpTicker>>()
    val pumpList: LiveData<List<PumpTicker>> = _pumpList

    fun fetchPumpCoins() {
        viewModelScope.launch(Dispatchers.IO) {

            val tickers = RetrofitInstance.api.getTickers()
                .filter { it.symbol.endsWith(USDT) }
                .take(QTD_COIN)

            val results = mutableListOf<PumpTicker>()
            for (ticker in tickers) {
                try {
                    val candles = analyzer.getCandles(ticker.symbol, "1m", 120)
                    val change = analyzer.variationPercent(candles)
                    if (change >= 10f) {
                        results.add(PumpTicker(ticker.symbol, change))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            _pumpList.postValue(results)
        }
    }
}
