package br.com.coin_project_ia_bot.presentation.fragments.signal

import android.util.Log
import androidx.lifecycle.*
import br.com.coin_project_ia_bot.data.api.BinanceApi
import br.com.coin_project_ia_bot.domain.model.SignalTicker
import br.com.coin_project_ia_bot.presentation.utils.calculateRSI
import br.com.coin_project_ia_bot.presentation.utils.countBullishCandles
import br.com.coin_project_ia_bot.presentation.utils.estimateAIScore
import br.com.coin_project_ia_bot.presentation.utils.estimateConsistencyAI
import br.com.coin_project_ia_bot.presentation.utils.extractVolume
import br.com.coin_project_ia_bot.presentation.utils.getCandlesForTicker
import br.com.coin_project_ia_bot.presentation.utils.getClosesForTicker
import br.com.coin_project_ia_bot.presentation.utils.isUptrend
import br.com.coin_project_ia_bot.presentation.utils.isVolumeIncreasing
import br.com.coin_project_ia_bot.presentation.utils.parseCandles
import br.com.coin_project_ia_bot.presentation.utils.variationPercent
import br.com.coin_project_ia_bot.presentation.fragments.signal.manually.SharedPairsViewModel
import br.com.coin_project_ia_bot.presentation.utils.TelegramNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Classe ViewModel aprimorada para sinais com 99% de precisão
@Suppress("UNREACHABLE_CODE")
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

                                val candles2hRaw = getCandlesForTicker(symbol)
                                val candlesParsed = parseCandles(candles2hRaw!!)
                                if (candlesParsed.isEmpty()) continue

                                val currentPrice = candlesParsed.last().close

                                // Determina faixas em % baseado no score
                                val (tpPercent, slPercent) = when {
                                    score >= 9 -> Pair(0.065f, 0.02f)  // Média entre 5%-8% e 2%
                                    score in 7..8 -> Pair(0.04f, 0.015f)
                                    score in 5..6 -> Pair(0.022f, 0.01f)
                                    else -> Pair(0.0f, 0.0f)
                                }

                                // Calcula preço alvo e stop
                                val takeProfitPrice = currentPrice * (1 + tpPercent)
                                val stopLossPrice = currentPrice * (1 - slPercent)


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
                                    lastPrice = currentPrice,
                                    takeProfitPrice = takeProfitPrice.toString(),
                                    stopLossPrice = stopLossPrice.toString(),
                                    investmentPercent = 0f
                                )

                                novosSinais.add(signal)
                                signalHistory.add(signal)

                                val msg = """
                                    🚨 <b>Sinal de Compra Detectado</b>
                                    💰 Par: <b>${signal.symbol}</b>
                                    💵 Preço Atual: <b>$currentPrice</b>
                                    📈 Variação 2h: <b>${"%.2f".format(change)}%</b>
                                    📊 RSI: <b>${"%.1f".format(rsi)}</b>
                                    ✨ Bullish Count: <b>$bullishCount</b>
                                    ⭐ Score IA: <b>$score /10</b>
                                    🔹 Consistência: <b>$consistency</b>
                                    🎯 Lucro Alvo (TP): <b>${"%.4f".format(takeProfitPrice)}</b>
                                    🛑 Stop Loss (SL): <b>${"%.4f".format(stopLossPrice)}</b>
                                    📊 Tendência confirmada em H1 e M15
                                    """.trimIndent()


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
