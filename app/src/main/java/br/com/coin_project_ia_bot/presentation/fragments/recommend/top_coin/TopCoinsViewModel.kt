package br.com.coin_project_ia_bot.presentation.fragments.recommend.top_coin

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

class TopCoinsViewModel : ViewModel() {

    private val _topCoins = MutableLiveData<List<SignalTicker>>()
    val topCoins: LiveData<List<SignalTicker>> = _topCoins

    fun getTop3ForInvestment() {
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

                    val candlesRaw = getCandlesForTicker(symbol) ?: continue
                    val candlesH1Raw = getCandlesForTicker(symbol, "1h") ?: continue
                    val candlesM15Raw = getCandlesForTicker(symbol, "15m") ?: continue

                    val parsed = parseCandles(candlesRaw)
                    val parsedH1 = parseCandles(candlesH1Raw)
                    val parsedM15 = parseCandles(candlesM15Raw)

                    if (parsed.size < 20 || parsedH1.size < 10 || parsedM15.size < 10) continue

                    val closes = getClosesForTicker(symbol)
                    val rsi = calculateRSI(closes) ?: continue
                    val change = variationPercent(parsed)
                    val bullishCount = countBullishCandles(parsed)
                    val volumeOK = isVolumeIncreasing(parsed)
                    val trendH1 = isUptrend(parsedH1)
                    val trendM15 = isUptrend(parsedM15)
                    val dailyVolume = extractVolume(parsed)

                    val atr = calculateATR(parsed)
                    val atrPercent = if (parsed.last().close != 0f) atr / parsed.last().close else 0f

                    // Nova Proteção: Candle atual precisa ser saudável
                    if (!isHealthyCandle(parsed.last())) continue

                    val score = estimateAIScore(rsi, bullishCount, change, dailyVolume)

                    if (
                        rsi in 55f..70f &&
                        change in 3f..10f &&
                        bullishCount >= 3 &&
                        volumeOK &&
                        trendH1 && trendM15 &&
                        score >= 8 &&
                        dailyVolume >= 1_000_000f &&
                        atrPercent >= 0.01f // Pelo menos 1% de range médio nas últimas 14 velas
                    ) {
                        val price = parsed.last().close
                        val (tpPercent, slPercent, investPercent) = when {
                            score >= 9 -> Triple(0.07f, 0.02f, 0.25f)
                            score == 8 -> Triple(0.05f, 0.015f, 0.15f)
                            else -> Triple(0.03f, 0.01f, 0.10f)
                        }

                        val takeProfitPrice = price * (1 + tpPercent)
                        val stopLossPrice = price * (1 - slPercent)

                        val tickerSignal = SignalTicker(
                            symbol = symbol,
                            variation2h = change,
                            rsi = rsi,
                            bullishCount = bullishCount,
                            score = score,
                            consistency = estimateConsistencyAI(score, rsi, bullishCount),
                            timestamp = System.currentTimeMillis(),
                            trend = "Confirmação H1 + M15 + Volume + ATR",
                            oneHourChange = ticker.priceChangePercent.toFloatOrNull() ?: 0f,
                            takeProfitRange = "%.2f".format(tpPercent * 100) + "%",
                            stopLoss = "%.2f".format(slPercent * 100) + "%",
                            lastPrice = price,
                            takeProfitPrice = "%.4f".format(takeProfitPrice),
                            stopLossPrice = "%.4f".format(stopLossPrice),
                            investmentPercent = investPercent
                        )

                        candidatos.add(tickerSignal)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Critério de desempate melhorado
            val topSorted = candidatos.sortedWith(
                compareByDescending<SignalTicker> { it.score }
                    .thenByDescending { it.rsi }
                    .thenByDescending { it.variation2h }
                    .thenByDescending { it.oneHourChange }
            ).take(3)

            _topCoins.postValue(topSorted)
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

    private fun isHealthyCandle(candle: Candle): Boolean {
        val totalRange = candle.high - candle.low
        val bodySize = abs(candle.close - candle.open)
        return totalRange > 0f && (bodySize / totalRange) >= 0.3f
    }
}
