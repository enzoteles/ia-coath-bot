package br.com.coin_project_ia_bot.presentation.fragments.recommend.swing_top_coin

// SwingCoinsViewModel.kt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SwingCoinsViewModel : ViewModel() {

    private val _swingCoins = MutableLiveData<List<SignalTicker>>()
    val swingCoins: LiveData<List<SignalTicker>> = _swingCoins

    fun getTop3ForSwing() {
        viewModelScope.launch(Dispatchers.IO) {
            val candidatos = mutableListOf<SignalTicker>()
            val tickers = RetrofitInstance.api.getTickers()
                .filter { it.symbol.endsWith("USDT") }
                .take(50)

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
                    val change = variationPercent(parsed4h)
                    val bullishCount = countBullishCandles(parsed1d)

                    val isConsistent = isUptrend(parsed4h) && isUptrend(parsed1d)
                    val volumeOK = isVolumeIncreasing(parsed1d)
                    val score = estimateAIScore(rsi, bullishCount, change, extractVolume(parsed1d))

                    if (
                        score >= 7 &&
                        rsi in 55f..70f &&
                        change in 3f..12f &&
                        bullishCount >= 3 &&
                        volumeOK &&
                        isConsistent
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
                            variation2h = change,
                            rsi = rsi,
                            bullishCount = bullishCount,
                            score = score,
                            consistency = estimateConsistencyAI(score, rsi, bullishCount),
                            timestamp = System.currentTimeMillis(),
                            trend = "Confirmação 4h e 1d",
                            oneHourChange = ticker.priceChangePercent.toFloatOrNull() ?: 0f,
                            takeProfitRange = tpPercent.toString(),
                            stopLoss = slPercent.toString(),
                            lastPrice = price,
                            takeProfitPrice = "%.4f".format(takeProfitPrice),
                            stopLossPrice = "%.4f".format(stopLossPrice),
                            investmentPercent = investPercent
                        )

                        candidatos.add(swingSignal)
                    }

                } catch (_: Exception) { }
            }

            _swingCoins.postValue(candidatos.sortedByDescending { it.score }.take(3))
        }
    }
}
