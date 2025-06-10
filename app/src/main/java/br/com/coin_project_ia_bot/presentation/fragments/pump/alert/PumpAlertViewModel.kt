package br.com.coin_project_ia_bot.presentation.fragments.pump.alert

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.coin_project_ia_bot.BinanceApi
import br.com.coin_project_ia_bot.presentation.MainActivity
import br.com.coin_project_ia_bot.domain.model.PumpTicker
import br.com.coin_project_ia_bot.presentation.fragments.signal.CoinAnalyzer
import br.com.coin_project_ia_bot.presentation.utils.TelegramNotifier
import br.com.coin_project_ia_bot.presentation.utils.PumpNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// PumpAlertViewModel.kt
class PumpAlertViewModel(private val api: BinanceApi) : ViewModel() {

    private val _alerts = MutableLiveData<List<PumpTicker>>()
    val alerts: LiveData<List<PumpTicker>> = _alerts
    private val analyzer = CoinAnalyzer(api)

    fun checkPumps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pumpPairs = getPumpCandidates()
                val detected = mutableListOf<PumpTicker>()

                for (symbol in pumpPairs) {
                    val candles = analyzer.getCandles(symbol, "1m", 120)
                    val change = CoinAnalyzer(api).variationPercent(candles)

                    if (change > 2f && change < 5f) {
                        detected.add(PumpTicker(symbol, change))

                        val msg = """
                            🚨 <b>PUMP DETECTADO!</b>
                            💰 Par: <b>$symbol</b>
                            📈 Variação 2h: <b>${"%.2f".format(change)}%</b>
                        """.trimIndent()

                        try {
                            TelegramNotifier.sendMessage(msg)
                            // ⏰ Mostrar notificação local
                            PumpNotifier.showPumpNotification(context, symbol, change)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                _alerts.postValue(detected)

            } catch (e: Exception) {
                e.printStackTrace()
                _alerts.postValue(emptyList())
            }
        }
    }

    private suspend fun getPumpCandidates(): List<String> {
        return try {
            val tickers = api.getTickers()
            tickers
                .filter { it.symbol.endsWith(MainActivity.USDT) }
                .filter {
                    val change = it.priceChangePercent.toFloatOrNull() ?: 0f
                    val high = it.highPrice.toFloatOrNull() ?: 0f
                    val low = it.lowPrice.toFloatOrNull() ?: 0f
                    val price = it.lastPrice.toFloatOrNull() ?: 0f
                    val range = high - low

                    change > 5 && price > 0 && (range / price) > 0.03
                }
                .map { it.symbol }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
