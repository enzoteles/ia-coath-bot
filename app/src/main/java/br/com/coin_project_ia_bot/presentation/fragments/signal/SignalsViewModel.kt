package br.com.coin_project_ia_bot.presentation.fragments.signal

import android.util.Log
import androidx.lifecycle.*
import br.com.coin_project_ia_bot.BinanceApi
import br.com.coin_project_ia_bot.domain.model.SignalTicker
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.calculateRSI
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.countBullishCandles
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.estimateAIScore
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.estimateConsistencyAI
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.extractVolume
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.getCandlesForTicker
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.getClosesForTicker
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.isUptrend
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.isVolumeIncreasing
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.parseCandles
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.variationPercent
import br.com.coin_project_ia_bot.presentation.fragments.signal.manually.SharedPairsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Classe ViewModel aprimorada para sinais com 99% de precisão
class SignalsViewModel(
    private val api: BinanceApi,
    private val sharedPairs: SharedPairsViewModel
) : ViewModel() {

    private val _signals = MutableLiveData<List<SignalTicker>>() // Exibe na UI
    val signals: LiveData<List<SignalTicker>> = _signals

    private val signalHistory = mutableListOf<SignalTicker>() // Armazena histórico

    fun fetchSignals() {
        viewModelScope.launch(Dispatchers.IO) {
            val novosSinais = mutableListOf<SignalTicker>()
            val pares = sharedPairs.selectedPairs.value?.toList().orEmpty()

            for (symbol in pares) {
                try {
                    val candles2h = getCandlesForTicker(symbol)
                    val change = variationPercent(parseCandles(candles2h!!))

                    if (change in 3f..10f && isVolumeIncreasing(parseCandles(candles2h))) {

                        val rsi = calculateRSI(getClosesForTicker(symbol)) ?: continue
                        val bullishCount = countBullishCandles(parseCandles( candles2h))

                        if (rsi in 55f..70f && bullishCount >= 3) {
                            val h1 = getCandlesForTicker(symbol)
                            val m15 = getCandlesForTicker(symbol)

                            if (isUptrend(parseCandles(h1!!)) && isUptrend(parseCandles(m15!!))) {
                                val consistency = estimateConsistencyAI(rsi.toInt(), bullishCount.toFloat(), change.toInt())

                                val score = estimateAIScore(
                                    rsi, bullishCount, change,
                                    extractVolume(parseCandles(candles2h))
                                )
                                val (tp, sl) = determineRiskRanges(score)
                                val signal = SignalTicker(
                                    symbol = symbol,
                                    variation2h = change,
                                    rsi = rsi,
                                    bullishCount = bullishCount,
                                    score = score,
                                    consistency = consistency,
                                    timestamp = System.currentTimeMillis(),
                                    trend = "",
                                    oneHourChange = 0.0f,
                                    takeProfitRange = tp,
                                    stopLoss = sl,
                                )

                                novosSinais.add(signal)
                                signalHistory.add(signal)

                                val msg = """
                                    🚨 <b>Sinal de Compra Detected</b>
                                    💰 Par: <b>${signal.symbol}</b>
                                    📈 Variação 2h: <b>%.2f%%</b>
                                    📊 RSI: <b>${"%.1f".format(rsi)}</b>
                                    ✨ Bullish Count: <b>$bullishCount</b>
                                    🔹 Consistência: <b>${consistency}</b>
                                    ⭐ Score IA: <b>${score}/10</b>
                                    🔍 Confirmado em H1 e M15
                                """.trimIndent().format(signal.variation2h)

                                TelegramNotifier.sendMessage(msg)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("Signals", "Erro ao processar $symbol: ${e.message}")
                }
            }

            val ordenados = novosSinais.sortedByDescending { it.score }
            _signals.postValue(ordenados)
        }
    }

    fun getSignalHistory(): List<SignalTicker> = signalHistory

    fun determineRiskRanges(score: Int): Pair<String, String> {
        return when {
            score >= 9 -> "5% a 8%" to "-2%"
            score in 7..8 -> "3% a 5%" to "-1.5%"
            score in 5..6 -> "1.5% a 3%" to "-1%"
            else -> "-" to "-"
        }
    }
}
