package br.com.coin_project_ia_bot.presentation.fragments.multi_tff.safe

import br.com.coin_project_ia_bot.RetrofitInstance
import br.com.coin_project_ia_bot.domain.model.MultiTFResult
import br.com.coin_project_ia_bot.presentation.utils.calculateRSI
import br.com.coin_project_ia_bot.presentation.utils.countBullishCandles
import br.com.coin_project_ia_bot.presentation.utils.getCandlesForTicker
import br.com.coin_project_ia_bot.presentation.utils.getClosesForTicker
import br.com.coin_project_ia_bot.presentation.utils.parseCandles

object SignalAnalyzer {
    suspend fun fetchSafeAnalysis(
        saldoDisponivel: Double,
        riscoMaximo: Double,
        performancePorPar: Map<String, Pair<Int, Int>>
    ): List<MultiTFResult> {
        val tickers = RetrofitInstance.api.getTickers().filter { it.symbol.endsWith("USDT") }.take(100)
        val btcTrend = TrendAnalyzer.getBTCTendency()
        if (btcTrend != "ALTISTA") return emptyList()

        val results = mutableListOf<MultiTFResult>()

        for (ticker in tickers) {
            val closes = getClosesForTicker(ticker.symbol)
            val candlesRaw = getCandlesForTicker(ticker.symbol)
            if (closes.isEmpty() || candlesRaw.isNullOrEmpty()) continue
            val parsedCandles = parseCandles(candlesRaw)

            val rsi = calculateRSI(closes) ?: continue
            val bullishCount = countBullishCandles(parsedCandles)
            val price = ticker.lastPrice.toFloatOrNull() ?: continue

            if (!SafetyFilters.isVolumeExploding(parsedCandles)) continue
            if (SafetyFilters.isNearSupportOrResistance(parsedCandles, price)) continue

            val atr = TAUtils.calculateATR(parsedCandles)
            val dynamicATRLimit = TAUtils.calculateDynamicATRLimit(parsedCandles)
            if (atr > price * dynamicATRLimit) continue

            val buyPressure = MarketDepthAnalyzer.getOrderBookPressure(ticker.symbol)
            if (buyPressure < 120f) continue

            if (MarketDepthAnalyzer.detectWall(ticker.symbol)) continue
            if (MarketDepthAnalyzer.detectSpoofing(ticker.symbol)) continue
            if (MarketDepthAnalyzer.detectLiquidityVoid(ticker.symbol)) continue

            val resistanceLevel = parsedCandles.takeLast(30).maxOfOrNull { it.high } ?: continue
            val breakoutIndex = parsedCandles.size - 2
            if (!BacktestUtils.isBreakoutConfirmed(parsedCandles, breakoutIndex, resistanceLevel)) continue

            if (MarketDepthAnalyzer.checkNegativeNews(ticker.symbol)) continue

            val score = ScoreCalculator.estimateAIScoreWithEMA(closes, rsi, bullishCount)
            val finalScore = ScoreCalculator.adjustScoreBasedOnHistory(ticker.symbol, score, performancePorPar)

            if (finalScore >= 8 && rsi in 50f..68f) {
                val (tpPercent, slPercent) = BacktestUtils.determineSafeRiskReward(finalScore)
                val tpPrice = price * (1 + tpPercent / 100)
                val slPrice = price * (1 - slPercent / 100)

                val valorEntrada = RiskManager.calcularEntradaIdeal(
                    saldoTotal = saldoDisponivel,
                    riscoPercentual = riscoMaximo,
                    distanciaStop = slPercent.toDouble()
                )

                results.add(
                    MultiTFResult(
                        symbol = ticker.symbol,
                        oneHourChange = ticker.priceChangePercent.toFloatOrNull() ?: 0f,
                        trend = ScoreCalculator.estimateConsistencyAI(finalScore, rsi, bullishCount),
                        consistency = ScoreCalculator.estimateConsistency(ticker),
                        score = finalScore,
                        rsi = rsi,
                        bullishCount = bullishCount,
                        takeProfit = "TP: +${"%.2f".format(tpPercent)}%",
                        stopLoss = "SL: -${"%.2f".format(slPercent)}%",
                        lastPrice = price,
                        takeProfitValue = tpPrice,
                        stopLossValue = slPrice,
                        idealEntryValue = valorEntrada
                    )
                )
            }
        }

        return results.sortedByDescending { it.score }
    }
}
