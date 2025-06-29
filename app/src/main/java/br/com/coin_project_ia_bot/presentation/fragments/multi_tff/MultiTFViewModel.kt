package br.com.coin_project_ia_bot.presentation.fragments.multi_tff

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.coin_project_ia_bot.RetrofitInstance
import br.com.coin_project_ia_bot.data.model.Ticker
import br.com.coin_project_ia_bot.domain.model.MultiTFResult
import br.com.coin_project_ia_bot.presentation.MainActivity
import br.com.coin_project_ia_bot.presentation.MainActivity.Companion.USDT
import br.com.coin_project_ia_bot.presentation.utils.TickerAnalysis
import br.com.coin_project_ia_bot.presentation.utils.analyzeTicker
import br.com.coin_project_ia_bot.presentation.utils.calculateRSI
import br.com.coin_project_ia_bot.presentation.utils.countBullishCandles
import br.com.coin_project_ia_bot.presentation.utils.getCandlesForTicker
import br.com.coin_project_ia_bot.presentation.utils.getClosesForTicker
import br.com.coin_project_ia_bot.presentation.utils.parseCandles
import br.com.coin_project_ia_bot.presentation.fragments.signal.manually.SharedPairsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val QTD_COIN = 60
class MultiTFViewModel(
    private val sharedPairs: SharedPairsViewModel
) : ViewModel() {

    val multiTFData = MutableLiveData<List<MultiTFResult>>()
    private val _investmentSignal = MutableLiveData<String>()
    val investmentSignal: LiveData<String> get() = _investmentSignal

    private val _analyzedTickers = MutableLiveData<List<TickerAnalysis>>()
    val analyzedTickers: LiveData<List<TickerAnalysis>> = _analyzedTickers

    fun startAutoUpdate(intervalMillis: Long) {
        viewModelScope.launch {
            while (true) {
                val result = fetchMultiTFAnalysis()
                multiTFData.postValue(result)
                delay(intervalMillis)
            }
        }
    }

    private suspend fun fetchMultiTFAnalysis(): List<MultiTFResult> {
        val tickers = RetrofitInstance.api.getTickers()
        val usdtPairs = tickers.filter {
            it.symbol.endsWith(USDT)
        }.take(QTD_COIN)

        val results = mutableListOf<MultiTFResult>()

        for (ticker in usdtPairs) {
            val closes = getClosesForTicker(ticker.symbol)
            val candles = getCandlesForTicker(ticker.symbol)

            if (closes.isNotEmpty() && candles!!.isNotEmpty()) {
                val rsi = calculateRSI(closes)
                val bullishCount = countBullishCandles(parseCandles(candles))
                val score = estimateAIScore(ticker, rsi, bullishCount)
                val consistency = estimateConsistency(ticker)
                val (tpPercent, slPercent) = determineRiskReward(score)
                val currentPrice = ticker.lastPrice.toFloatOrNull() ?: 0f

                val takeProfitPrice = currentPrice * (1 + tpPercent / 100)
                val stopLossPrice = currentPrice * (1 - slPercent / 100)

                if (score >= 7) {
                    val trend = estimateConsistencyAI(score, rsi, bullishCount)
                    results.add(
                        MultiTFResult(
                            symbol = ticker.symbol,
                            oneHourChange = ticker.priceChangePercent.toFloatOrNull() ?: 0f,
                            trend = trend,
                            consistency = consistency,
                            score = score,
                            rsi = rsi,
                            bullishCount = bullishCount,
                            takeProfit = "TP: +${"%.2f".format(tpPercent)}% (≈ ${"%.4f".format(takeProfitPrice)})",
                            stopLoss = "SL: -${"%.2f".format(slPercent)}% (≈ ${"%.4f".format(stopLossPrice)})",
                            lastPrice = ticker.lastPrice.toFloatOrNull() ?: 0f,
                            takeProfitValue = 3f,
                            stopLossValue = 3f,
                            idealEntryValue = 3.0
                        )
                    )
                }
            }
        }

        return results.sortedWith(compareByDescending<MultiTFResult> {
            when (it.trend) {
                "🚀 Forte Tendência Confirmada" -> 4
                "📈 Boa Tendência" -> 3
                "🔍 Potencial Emergente" -> 2
                "⚠️ Baixa Confiabilidade" -> 1
                else -> 0
            }
        }.thenByDescending { it.score })
    }

    private fun determineRiskReward(score: Int): Pair<Float, Float> {
        return when {
            score >= 9 -> Pair(6.5f, 2f) // Forte tendência
            score in 7..8 -> Pair(4.0f, 1.5f) // Boa tendência
            score in 5..6 -> Pair(2.0f, 1f) // Potencial emergente
            else -> Pair(0f, 0f)
        }
    }

    private fun estimateAIScore(ticker: Ticker, rsi: Float?, bullishCount: Int): Int {
        val change = ticker.priceChangePercent.toFloatOrNull() ?: return 0
        val volume = ticker.volume.toFloatOrNull() ?: return 0
        var score = 0

        if (change in 2f..5f) score += 3
        if (change > 5f) score += 4
        if (volume > 1_000_000f) score += 2
        if (rsi != null && rsi in 55f..70f) score += 2
        if (bullishCount >= 3) score += 2

        return score.coerceAtMost(10)
    }

    private fun estimateConsistencyAI(score: Int, rsi: Float?, bullishCount: Int): String {
        return when {
            score >= 9 && rsi != null && rsi in 60f..70f && bullishCount >= 5 -> "🚀 Forte Tendência Confirmada"
            score in 7..8 && rsi != null && rsi in 55f..65f -> "📈 Boa Tendência"
            score in 5..6 -> "🔍 Potencial Emergente"
            else -> "⚠️ Baixa Confiabilidade"
        }
    }

    private fun estimateConsistency(ticker: Ticker): String {
        val change = ticker.priceChangePercent.toFloatOrNull() ?: 0f
        return when {
            change > 5 -> "Tendência Forte ↑"
            change > 1 -> "Tendência Moderada ↑"
            change < -1 -> "Tendência Baixa ↓"
            else -> "Estável"
        }
    }

    fun fetchAndScoreTickers() {
        viewModelScope.launch {
            try {
                val tickers = RetrofitInstance.api.getTickers()
                    .filter { it.symbol.endsWith(MainActivity.USDT) }

                val analyses = mutableListOf<TickerAnalysis>()

                for (ticker in tickers) {
                    Log.d("DEBUG", "Processando ${ticker.symbol}")

                    val closes = getClosesForTicker(ticker.symbol)
                    val candlesRaw = getCandlesForTicker(ticker.symbol)
                    val candles = parseCandles(candlesRaw!!)

                    if (closes.isNotEmpty() && candles.isNotEmpty()) {
                        val analysis = analyzeTicker(ticker, closes, candles)
                        if (analysis?.score!! >= 7) {
                            analyses.add(analysis)
                        }
                    } else {
                        Log.w("DEBUG", "Dados insuficientes para ${ticker.symbol}")
                    }
                }

                val sorted = analyses.sortedByDescending { it.score }
                _analyzedTickers.value = sorted

            } catch (e: Exception) {
                Log.e("ViewModel", "Erro na fetchAndScoreTickers", e)
            }
        }
    }

    fun analyzeInvestmentDay(analysisList: List<TickerAnalysis>) {
        if (analysisList.isEmpty()) {
            _investmentSignal.value = "Sem dados suficientes para análise."
            return
        }
    }
}
