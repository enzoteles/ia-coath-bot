package br.com.coin_project_ia_bot.presentation.fragments.recommend.swing_top_coin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.coin_project_ia_bot.RetrofitInstance
import br.com.coin_project_ia_bot.domain.model.Candle
import br.com.coin_project_ia_bot.domain.model.SignalTicker
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

class SwingCoinsViewModel : ViewModel() {

    private val _swingCoins = MutableLiveData<List<SignalTicker>>()
    val swingCoins: LiveData<List<SignalTicker>> = _swingCoins

    fun getTop3ForSwing() {
        viewModelScope.launch(Dispatchers.IO) {
            val candidatos = mutableListOf<SignalTicker>()
            val tickers = try {
                RetrofitInstance.api.getTickers()
                    .filter { it.symbol.endsWith("USDT") }
                    .take(80)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }

            for (ticker in tickers) {
                try {
                    val symbol = ticker.symbol
                    val candles4h = getCandlesForTicker(symbol, "4h") ?: continue
                    val candles1d = getCandlesForTicker(symbol, "1d") ?: continue

                    val parsed4h = parseCandles(candles4h)
                    val parsed1d = parseCandles(candles1d)

                    if (parsed4h.size < 20 || parsed1d.size < 20) continue

                    val closes = getClosesForTicker(symbol, "1d")
                    val rsi = calculateRSI(closes) ?: continue
                    val change4h = variationPercent(parsed4h)
                    val change1d = variationPercent(parsed1d)
                    val bullishCount = countBullishCandles(parsed1d)

                    val isConsistent = isUptrend(parsed4h) && isUptrend(parsed1d)
                    val volumeOK = isVolumeIncreasing(parsed1d)
                    val dailyVolume = extractVolume(parsed1d)

                    val atr = calculateATR(parsed1d)
                    val atrPercent = if (parsed1d.last().close != 0f) atr / parsed1d.last().close else 0f

                    val score = estimateAIScore(rsi, bullishCount, change1d, dailyVolume)

                    // ✅ Filtros adicionais para confiabilidade
                    if (
                        score >= 7 &&
                        rsi in 55f..70f &&
                        change1d in 3f..12f &&
                        bullishCount >= 3 &&
                        volumeOK &&
                        isConsistent &&
                        atrPercent >= 0.02f && // Pelo menos 2% de range diário (evita ativos sem volatilidade)
                        dailyVolume >= 1_000_000f // Pelo menos 1 milhão USDT de volume diário
                    ) {
                        val price = parsed1d.last().close
                        val (tpPercent, slPercent, investPercent) = when {
                            score >= 9 -> Triple(0.12f, 0.035f, 0.25f)
                            score == 8 -> Triple(0.09f, 0.03f, 0.15f)
                            else -> Triple(0.07f, 0.025f, 0.10f)
                        }

                        val takeProfitPrice = price * (1 + tpPercent)
                        val stopLossPrice = price * (1 - slPercent)

                        val swingSignal = SignalTicker(
                            symbol = symbol,
                            variation2h = change4h,
                            rsi = rsi,
                            bullishCount = bullishCount,
                            score = score,
                            consistency = estimateConsistencyAI(score, rsi, bullishCount),
                            timestamp = System.currentTimeMillis(),
                            trend = "Tendência Confirmada 4h + 1d",
                            oneHourChange = ticker.priceChangePercent.toFloatOrNull() ?: 0f,
                            takeProfitRange = "%.2f".format(tpPercent * 100) + "%",
                            stopLoss = "%.2f".format(slPercent * 100) + "%",
                            lastPrice = price,
                            takeProfitPrice = "%.4f".format(takeProfitPrice),
                            stopLossPrice = "%.4f".format(stopLossPrice),
                            investmentPercent = investPercent
                        )

                        candidatos.add(swingSignal)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // ✅ Classificação: Score > RSI > Change > Volume
            val topSorted = candidatos.sortedWith(
                compareByDescending<SignalTicker> { it.score }
                    .thenByDescending { it.rsi }
                    .thenByDescending { it.variation2h }
                    .thenByDescending { it.oneHourChange }
            ).take(3)

            _swingCoins.postValue(topSorted)
        }
    }

    private fun calculateATR(candles: List<Candle>, period: Int = 14): Float {
        if (candles.size < period + 1) return 0f
        var atr = 0f
        for (i in 1..period) {
            val high = candles[candles.size - i].high
            val low = candles[candles.size - i].low
            val closePrev = candles[candles.size - i - 1].close
            val tr = maxOf(high - low, abs(high - closePrev), abs(low - closePrev))
            atr += tr
        }
        return atr / period
    }
}
