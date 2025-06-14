package br.com.coin_project_ia_bot.presentation.fragments.multi_tff.safe

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.coin_project_ia_bot.RetrofitInstance
import br.com.coin_project_ia_bot.data.model.Ticker
import br.com.coin_project_ia_bot.domain.model.Candle
import br.com.coin_project_ia_bot.domain.model.MultiTFResult
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.calculateRSI
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.countBullishCandles
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.getCandlesForTicker
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.getClosesForTicker
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.parseCandles
import br.com.coin_project_ia_bot.presentation.fragments.signal.manually.SharedPairsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SafeTradeViewModel : ViewModel() {

    val safeSignals = MutableLiveData<List<MultiTFResult>>()
    private val saldoDisponivel = 500.0
    private val riscoMaximo = 2.0

    fun startAutoUpdate(intervalMillis: Long) {
        viewModelScope.launch {
            while (true) {
                val result = fetchSafeAnalysis()
                safeSignals.postValue(result)
                delay(intervalMillis)
            }
        }
    }

    suspend fun runBacktest(): BacktestResult {
        val tickers = RetrofitInstance.api.getTickers()
        val usdtPairs = tickers.filter { it.symbol.endsWith("USDT") }.take(100)

        var acertos = 0
        var erros = 0
        var lucroTotal = 0.0

        for (ticker in usdtPairs) {
            val closes = getClosesForTicker(ticker.symbol)
            val candlesRaw = getCandlesForTicker(ticker.symbol)
            if (closes.isEmpty() || candlesRaw.isNullOrEmpty()) continue

            val parsedCandles = parseCandles(candlesRaw)
            val rsi = calculateRSI(closes)
            val bullishCount = countBullishCandles(parsedCandles)

            val lastCandle = parsedCandles.lastOrNull() ?: continue
            val candleOpen = lastCandle.open
            val candleClose = lastCandle.close
            val variationPercent = if (candleOpen != 0f)
                ((candleClose - candleOpen) / candleOpen) * 100
            else 0f
            if (variationPercent > 6) continue

            val score = estimateAIScore(ticker, rsi, bullishCount)
            val price = ticker.lastPrice.toFloatOrNull() ?: continue

            if (score >= 8 && rsi!! in 50f..68f && volumeIncreasing(ticker) &&
                !isNearResistance(parsedCandles, price) && !isNearSupport(parsedCandles, price)) {

                val (tpPercent, slPercent) = determineSafeRiskReward(score)
                val tpPrice = price * (1 + tpPercent / 100)
                val slPrice = price * (1 - slPercent / 100)

                val simulado = simulatePriceMovement(parsedCandles, price, tpPrice, slPrice)

                when (simulado) {
                    TradeResult.GAIN -> {
                        acertos++
                        lucroTotal += saldoDisponivel * (tpPercent / 100)
                    }
                    TradeResult.LOSS -> {
                        erros++
                        lucroTotal -= saldoDisponivel * (slPercent / 100)
                    }
                    else -> {}
                }
            }
        }

        return BacktestResult(
            totalTrades = acertos + erros,
            wins = acertos,
            losses = erros,
            profitUSDT = lucroTotal,
            accuracy = if (acertos + erros > 0) acertos * 100 / (acertos + erros) else 0
        )
    }



    private fun simulatePriceMovement(candles: List<Candle>, entryPrice: Float, tp: Float, sl: Float): TradeResult {
        for (candle in candles.takeLast(20)) {
            if (candle.high >= tp) return TradeResult.GAIN
            if (candle.low <= sl) return TradeResult.LOSS
        }
        return TradeResult.NONE
    }

    private fun determineSafeRiskReward(score: Int): Pair<Float, Float> {
        return when {
            score >= 9 -> Pair(4.5f, 1.5f)
            score in 8..8 -> Pair(3.0f, 1.2f)
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
        if (rsi != null && rsi in 55f..68f) score += 2
        if (bullishCount >= 3) score += 2

        return score.coerceAtMost(10)
    }

    suspend fun fetchSafeAnalysis(): List<MultiTFResult> {
        val tickers = RetrofitInstance.api.getTickers()
        val usdtPairs = tickers.filter { it.symbol.endsWith("USDT") }.take(80)

        val btcTrend = getBTCTendency()
        if (btcTrend != "ALTISTA") return emptyList()

        val results = mutableListOf<MultiTFResult>()

        for (ticker in usdtPairs) {
            val closes = getClosesForTicker(ticker.symbol)
            val candlesRaw = getCandlesForTicker(ticker.symbol)
            if (closes.isEmpty() || candlesRaw.isNullOrEmpty()) continue

            val parsedCandles = parseCandles(candlesRaw)
            val rsi = calculateRSI(closes)
            val bullishCount = countBullishCandles(parsedCandles)

            val lastCandle = parsedCandles.lastOrNull() ?: continue
            val candleOpen = lastCandle.open
            val candleClose = lastCandle.close
            val variationPercent = if (candleOpen != 0f)
                ((candleClose - candleOpen) / candleOpen) * 100
            else 0f
            if (variationPercent > 6) continue

            val score = estimateAIScore(ticker, rsi, bullishCount)
            val price = ticker.lastPrice.toFloatOrNull() ?: continue

            if (score >= 8 && rsi!! in 50f..68f && volumeIncreasing(ticker) &&
                !isNearResistance(parsedCandles, price) && !isNearSupport(parsedCandles, price)) {

                val (tpPercent, slPercent) = determineSafeRiskReward(score)

                val tpPrice = price * (1 + tpPercent / 100)
                val slPrice = price * (1 - slPercent / 100)

                val valorEntrada = RiskManager.calcularEntradaIdeal(
                    saldoTotal = 500.0,
                    riscoPercentual = 2.0,
                    distanciaStop = slPercent.toDouble()
                )

                results.add(
                    MultiTFResult(
                        symbol = ticker.symbol,
                        oneHourChange = ticker.priceChangePercent.toFloatOrNull() ?: 0f,
                        trend = estimateConsistencyAI(score, rsi, bullishCount),
                        consistency = estimateConsistency(ticker),
                        score = score,
                        rsi = rsi,
                        bullishCount = bullishCount,
                        takeProfit = "TP: +${"%.2f".format(tpPercent)}% (≈ ${"%.4f".format(tpPrice)})",
                        stopLoss = "SL: -${"%.2f".format(slPercent)}% (≈ ${"%.4f".format(slPrice)})",
                        lastPrice = price,
                        takeProfitValue = tpPrice,
                        stopLossValue = slPrice,
                        idealEntryValue = valorEntrada
                    )
                )
            }
        }

        return results.sortedWith(compareByDescending<MultiTFResult> {
            when (it.trend) {
                "🚀 Forte Tendência Confirmada" -> 4
                "📈 Boa Tendência" -> 3
                "🔍 Potencial Emergente" -> 2
                else -> 1
            }
        }.thenByDescending { it.score })
    }


    private fun estimateConsistencyAI(score: Int, rsi: Float?, bullishCount: Int): String {
        return when {
            score >= 9 && rsi!! in 60f..68f && bullishCount >= 5 -> "🚀 Forte Tendência Confirmada"
            score in 8..8 && rsi!! in 55f..65f -> "📈 Boa Tendência"
            else -> "🔍 Potencial Emergente"
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

    private fun volumeIncreasing(ticker: Ticker): Boolean {
        return (ticker.quoteVolume.toFloatOrNull() ?: 0f) > 1_000_000f
    }

    private fun isNearResistance(candles: List<Candle>, currentPrice: Float): Boolean {
        val highs = candles.takeLast(20).map { it.high }
        val resistance = highs.maxOrNull() ?: return false
        val threshold = resistance * 0.985f
        return currentPrice >= threshold
    }

    private fun isNearSupport(candles: List<Candle>, currentPrice: Float): Boolean {
        val lows = candles.takeLast(20).map { it.low }
        val support = lows.minOrNull() ?: return false
        val threshold = support * 1.015f
        return currentPrice <= threshold
    }

    private fun getBTCTendency(): String {
        return "ALTISTA"
    }
}

enum class TradeResult { GAIN, LOSS, NONE }

data class BacktestResult(
    val totalTrades: Int,
    val wins: Int,
    val losses: Int,
    val profitUSDT: Double,
    val accuracy: Int
)


