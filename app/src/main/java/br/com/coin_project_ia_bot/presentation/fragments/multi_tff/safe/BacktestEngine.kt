package br.com.coin_project_ia_bot.presentation.fragments.multi_tff.safe

import br.com.coin_project_ia_bot.RetrofitInstance
import br.com.coin_project_ia_bot.data.model.BacktestTradeResult
import br.com.coin_project_ia_bot.domain.model.Candle
import br.com.coin_project_ia_bot.presentation.fragments.multi_tff.safe.PerformanceTracker.registrarPerformance
import br.com.coin_project_ia_bot.presentation.fragments.multi_tff.safe.SafetyFilters.isHealthyCandle
import br.com.coin_project_ia_bot.presentation.fragments.multi_tff.safe.SafetyFilters.isNearSupportOrResistance
import br.com.coin_project_ia_bot.presentation.fragments.multi_tff.safe.ScoreCalculator.adjustScoreBasedOnHistory
import br.com.coin_project_ia_bot.presentation.fragments.multi_tff.safe.ScoreCalculator.estimateAIScoreWithEMA
import br.com.coin_project_ia_bot.presentation.fragments.multi_tff.safe.TAUtils.calculateATR
import br.com.coin_project_ia_bot.presentation.fragments.multi_tff.safe.TAUtils.calculateDynamicStopLoss
import br.com.coin_project_ia_bot.presentation.fragments.multi_tff.safe.TAUtils.calculateEMA
import br.com.coin_project_ia_bot.presentation.utils.TrendAIAnalyzer
import br.com.coin_project_ia_bot.presentation.utils.calculateRSI
import br.com.coin_project_ia_bot.presentation.utils.countBullishCandles
import br.com.coin_project_ia_bot.presentation.utils.getCandlesForTicker
import br.com.coin_project_ia_bot.presentation.utils.parseCandles
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BacktestEngine {
    suspend fun runBacktest(
        days: Int,
        saldo: Double,
        riskPerTrade: Double,
        maxEntriesPerDay: Int,
        maxConsecutiveLosses: Int,
        minCandlesBetweenSamePair: Int,
        tradesExecutados: MutableList<BacktestTradeResult>,
        performancePorPar: MutableMap<String, Pair<Int, Int>>
    ): Pair<BacktestResult, List<BacktestTradeResult>> {

        val tickers = RetrofitInstance.api.getTickers()
        val usdtPairs = tickers.filter { it.symbol.endsWith("USDT") }.take(80)

        var acertos = 0
        var erros = 0
        var lucroTotal = 0.0
        var perdasSeguidas = 0
        val entradasPorDia = mutableMapOf<String, Int>()
        val ultimoCandlePorPar = mutableMapOf<String, Int>()

        val btcCandlesRaw = getCandlesForTicker("BTCUSDT", interval = "1h", limit = days * 24) ?: return Pair(
            BacktestResult(0, 0, 0, 0.0, 0),
            emptyList()
        )
        val btcCandles = parseCandles(btcCandlesRaw)

        for (ticker in usdtPairs) {
            val historicalCandlesRaw = getCandlesForTicker(ticker.symbol, interval = "1h", limit = days * 24) ?: continue
            val historicalCandles = parseCandles(historicalCandlesRaw)

            for (i in 30 until historicalCandles.size - 10) {
                val closes = historicalCandles.subList(0, i).map { it.close }
                val partialCandles = historicalCandles.subList(0, i)
                val rsi = calculateRSI(closes)
                val bullishCount = countBullishCandles(partialCandles)
                val currentPrice = historicalCandles[i].close

                val safeEndIndex = minOf(i, btcCandles.size)
                val btcTrend = calcularTendenciaBTC(btcCandles.subList(0, safeEndIndex))
                if (btcTrend != "ALTISTA") continue

                val candleTimestamp = historicalCandles[i].openTime
                val dataDoCandle = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(candleTimestamp))
                val entradasHoje = entradasPorDia[dataDoCandle] ?: 0
                if (entradasHoje >= maxEntriesPerDay) continue

                val ultimoCandleEntrada = ultimoCandlePorPar[ticker.symbol] ?: -999
                if ((i - ultimoCandleEntrada) < minCandlesBetweenSamePair) continue

                if (!isHealthyCandle(historicalCandles[i])) continue

                if (isNearSupportOrResistance(partialCandles, currentPrice)) continue

                val atr = calculateATR(partialCandles)
                if (atr > (currentPrice * 0.03f)) continue

                val buyPressure = getOrderBookPressure(ticker.symbol)
                if (buyPressure < 120f) continue

                if (!isMultiTimeframeConfluence(ticker.symbol)) continue

                val rawScore = estimateAIScoreWithEMA(closes, rsi, bullishCount)
                val finalScore = adjustScoreBasedOnHistory(ticker.symbol, rawScore, performancePorPar)

                if (finalScore >= 8 && rsi != null && rsi in 50f..68f) {
                    val atrStopDistance = calculateDynamicStopLoss(partialCandles)
                    val slPrice = currentPrice - atrStopDistance
                    val tpPercent = 3.0f
                    val tpPrice = currentPrice * (1 + tpPercent / 100)

                    val futureCandles = historicalCandles.subList(i, (i + 10).coerceAtMost(historicalCandles.size))
                    val result = simulatePriceMovement(futureCandles, currentPrice, tpPrice, slPrice)

                    val valorInvestido = saldo * (riskPerTrade / 100)
                    val ganhoRealPercentual = when (result) {
                        TradeResult.GAIN -> tpPercent
                        TradeResult.LOSS -> -riskPerTrade
                        else -> 0.0
                    }
                    lucroTotal += saldo * (ganhoRealPercentual.toFloat() / 100)

                    when (result) {
                        TradeResult.GAIN -> {
                            acertos++
                            perdasSeguidas = 0
                            registrarPerformance(ticker.symbol, true, performancePorPar)
                            tradesExecutados.add(BacktestTradeResult(ticker.symbol, "GAIN", currentPrice, tpPrice, slPrice))
                        }
                        TradeResult.LOSS -> {
                            erros++
                            perdasSeguidas++
                            registrarPerformance(ticker.symbol, false, performancePorPar)
                            tradesExecutados.add(BacktestTradeResult(ticker.symbol, "LOSS", currentPrice, tpPrice, slPrice))

                            if (perdasSeguidas >= maxConsecutiveLosses) {
                                return Pair(
                                    BacktestResult(
                                        totalTrades = acertos + erros,
                                        wins = acertos,
                                        losses = erros,
                                        profitUSDT = lucroTotal,
                                        accuracy = if (acertos + erros > 0) acertos * 100 / (acertos + erros) else 0
                                    ),
                                    tradesExecutados
                                )
                            }
                        }
                        else -> {}
                    }

                    entradasPorDia[dataDoCandle] = entradasHoje + 1
                    ultimoCandlePorPar[ticker.symbol] = i
                }
            }
        }

        return Pair(
            BacktestResult(
                totalTrades = acertos + erros,
                wins = acertos,
                losses = erros,
                profitUSDT = lucroTotal,
                accuracy = if (acertos + erros > 0) acertos * 100 / (acertos + erros) else 0
            ),
            tradesExecutados
        )
    }

    private fun calcularTendenciaBTC(candles: List<Candle>): String {
        if (candles.size < 21) return "NEUTRA"

        val closes = candles.map { it.close }
        val ema9 = calculateEMA(closes, 9)
        val ema21 = calculateEMA(closes, 21)
        val rsi = calculateRSI(closes)
        val bullishCandles = candles.takeLast(10).count { it.close > it.open }

        var score = 0
        if (ema9.last() > ema21.last()) score++
        if (rsi != null && rsi > 50f) score++
        if (bullishCandles >= 6) score++

        return when {
            score >= 2 -> "ALTISTA"
            score == 1 -> "NEUTRA"
            else -> "BAIXISTA"
        }
    }

    private suspend fun getOrderBookPressure(symbol: String): Float {
        val response = RetrofitInstance.api.getOrderBook(symbol, 10)
        if (response.isSuccessful) {
            val book = response.body() ?: return 0f
            val buy = book.bids.sumOf { it[1].toDoubleOrNull() ?: 0.0 }
            val sell = book.asks.sumOf { it[1].toDoubleOrNull() ?: 0.0 }
            return if (sell == 0.0) 0f else ((buy / sell) * 100).toFloat()
        }
        return 0f
    }

    private suspend fun isMultiTimeframeConfluence(symbol: String): Boolean {
        val h1Candles = getCandlesForTicker(symbol, interval = "1h", limit = 50) ?: return false
        val h4Candles = getCandlesForTicker(symbol, interval = "4h", limit = 50) ?: return false
        val h1Trend = calcularTendenciaGeral(parseCandles(h1Candles))
        val h4Trend = calcularTendenciaGeral(parseCandles(h4Candles))
        return h1Trend == "ALTISTA" && h4Trend == "ALTISTA"
    }

    private fun calcularTendenciaGeral(candles: List<Candle>): String {
        if (candles.size < 20) return "NEUTRA"

        val ultimas = candles.takeLast(20)
        val primeiroClose = ultimas.first().close
        val ultimoClose = ultimas.last().close
        val movimentoPercentual = ((ultimoClose - primeiroClose) / primeiroClose) * 100

        val bullishCount = ultimas.count { it.close > it.open }
        val bearishCount = ultimas.count { it.open > it.close }

        return when {
            movimentoPercentual > 1.5f && bullishCount >= 12 -> "ALTISTA"
            movimentoPercentual < -1.5f && bearishCount >= 12 -> "BAIXISTA"
            else -> "NEUTRA"
        }
    }

    private fun simulatePriceMovement(candles: List<Candle>, entryPrice: Float, tp: Float, sl: Float): TradeResult {
        for (candle in candles.takeLast(20)) {
            if (candle.high >= tp) return TradeResult.GAIN
            if (candle.low <= sl) return TradeResult.LOSS
        }
        return TradeResult.NONE
    }


}
