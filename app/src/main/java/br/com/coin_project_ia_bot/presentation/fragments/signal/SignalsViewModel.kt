package br.com.coin_project_ia_bot.presentation.fragments.signal

import androidx.lifecycle.*
import br.com.coin_project_ia_bot.BinanceApi
import br.com.coin_project_ia_bot.domain.model.SignalTicker
import br.com.coin_project_ia_bot.presentation.fragments.signal.manually.SharedPairsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Atualizando SignalViewModel para usar o sharedViewModel
class SignalsViewModel(
    private val api: BinanceApi,
    private val sharedPairs: SharedPairsViewModel
) : ViewModel() {

    private val _signals = MutableLiveData<List<SignalTicker>>()
    val signals: LiveData<List<SignalTicker>> = _signals

    private val analyzer = CoinAnalyzer(api)

    fun fetchSignals() {
        viewModelScope.launch(Dispatchers.IO) {
            val sinais = mutableListOf<SignalTicker>()
            val pares = sharedPairs.selectedPairs.value?.toList().orEmpty()

            for (symbol in pares) {
                val candles2h = analyzer.getCandles(symbol, "1m", 120)
                val change = analyzer.variationPercent(candles2h)
                if (change in 3f..5f && analyzer.isVolumeIncreasing(candles2h)) {
                    val h1 = analyzer.getCandles(symbol, "1h", 3)
                    val m15 = analyzer.getCandles(symbol, "15m", 4)
                    if (analyzer.isUptrend(h1) && analyzer.isUptrend(m15)) {
                        val signal = SignalTicker(symbol, change)
                        sinais.add(signal)

                        val msg = """
                            🚨 <b>Sinal de Compra Detectado</b>
                            💰 Par: <b>${signal.symbol}</b>
                            📈 Variação 2h: <b>%.2f%%</b>
                            🔎 Tendência confirmada em H1 e M15
                        """.trimIndent().format(signal.variation2h)

                        TelegramNotifier.sendMessage(msg)
                    }
                }
            }
            _signals.postValue(sinais)
        }
    }
}
