package br.com.coin_project_ia_bot.presentation.fragments.recommend.top_coin

import br.com.coin_project_ia_bot.RetrofitInstance
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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TopCoinsViewModel : ViewModel() {

    private val _topCoins = MutableLiveData<List<SignalTicker>>()
    val topCoins: LiveData<List<SignalTicker>> = _topCoins

    fun getTop3ForInvestment() {
        viewModelScope.launch(Dispatchers.IO) {
            val candidatos = mutableListOf<SignalTicker>()
            val tickers = RetrofitInstance.api.getTickers()
                .filter { it.symbol.endsWith("USDT") }
                .take(50)

            for (ticker in tickers) {
                try {
                    val symbol = ticker.symbol
                    val candles = getCandlesForTicker(symbol)
                    val parsed = parseCandles(candles ?: continue)
                    if (parsed.size < 20) continue

                    val rsi = calculateRSI(getClosesForTicker(symbol)) ?: continue
                    val change = variationPercent(parsed)
                    val bullishCount = countBullishCandles(parsed)
                    val volumeOK = isVolumeIncreasing(parsed)
                    val trendH1 = isUptrend(parseCandles(getCandlesForTicker(symbol, "1h") ?: continue))
                    val trendM15 = isUptrend(parseCandles(getCandlesForTicker(symbol, "15m") ?: continue))
                    val score = estimateAIScore(rsi, bullishCount, change, extractVolume(parsed))

                    if (
                        rsi in 55f..70f &&
                        change in 3f..10f &&
                        bullishCount >= 3 &&
                        volumeOK &&
                        trendH1 && trendM15 &&
                        score >= 8
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
                            trend = "Confirmação H1 e M15",
                            oneHourChange = ticker.priceChangePercent.toFloatOrNull() ?: 0f,
                            takeProfitRange = tpPercent.toString(),
                            stopLoss = slPercent.toString(),
                            lastPrice = price,
                            takeProfitPrice = "%.4f".format(takeProfitPrice),
                            stopLossPrice = "%.4f".format(stopLossPrice),
                            investmentPercent = investPercent
                        )

                        candidatos.add(tickerSignal)
                    }

                } catch (_: Exception) { }
            }

            _topCoins.postValue(candidatos.sortedByDescending { it.score }.take(3))
        }
    }
}
