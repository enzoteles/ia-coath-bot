package br.com.coin_project_ia_bot.presentation.fragments.multi_tff.safe

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.coin_project_ia_bot.RetrofitInstance
import br.com.coin_project_ia_bot.data.model.BacktestTradeResult
import br.com.coin_project_ia_bot.data.model.Ticker
import br.com.coin_project_ia_bot.domain.model.Candle
import br.com.coin_project_ia_bot.domain.model.MultiTFResult
import br.com.coin_project_ia_bot.data.api.CoinGeckoInstance
import br.com.coin_project_ia_bot.presentation.utils.calculateRSI
import br.com.coin_project_ia_bot.presentation.utils.countBullishCandles
import br.com.coin_project_ia_bot.presentation.utils.getCandlesForTicker
import br.com.coin_project_ia_bot.presentation.utils.getClosesForTicker
import br.com.coin_project_ia_bot.presentation.utils.parseCandles
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

class SafeTradeViewModel : ViewModel() {

    val safeSignals = MutableLiveData<List<MultiTFResult>>()
    val tradesExecutados = mutableListOf<BacktestTradeResult>()
    private val performancePorPar = mutableMapOf<String, Pair<Int, Int>>()
    val entradasPorDia = mutableMapOf<String, Int>()
    val maxEntradasPorDia = 5
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

    private fun registrarPerformance(symbol: String, foiGain: Boolean) {
        val (acertosAnteriores, totalAnterior) = performancePorPar[symbol] ?: Pair(0, 0)
        val novosAcertos = if (foiGain) acertosAnteriores + 1 else acertosAnteriores
        performancePorPar[symbol] = Pair(novosAcertos, totalAnterior + 1)
    }


    fun exportBacktestToJSON(trades: List<BacktestTradeResult>, context: Context): String {
        val json = Gson().toJson(trades)
        val file = File(context.filesDir, "backtest_result.json")
        file.writeText(json)
        return file.absolutePath
    }

    fun exportBacktestToCSV(trades: List<BacktestTradeResult>, context: Context): String {
        val file = File(context.filesDir, "backtest_result.csv")
        val header = "Symbol,Result,EntryPrice,TakeProfit,StopLoss\n"
        val content = trades.joinToString("\n") {
            "${it.symbol},${it.result},${it.entryPrice},${it.takeProfit},${it.stopLoss}"
        }
        file.writeText(header + content)
        return file.absolutePath
    }

    suspend fun runBacktest(): BacktestResult {
        val tickers = RetrofitInstance.api.getTickers()
        val usdtPairs = tickers.filter { it.symbol.endsWith("USDT") }.take(100)

        var acertos = 0
        var erros = 0
        var lucroTotal = 0.0

        val btcTrend = getBTCTendency()
        if (btcTrend != "ALTISTA") {
            return BacktestResult(
                totalTrades = 0,
                wins = 0,
                losses = 0,
                profitUSDT = 0.0,
                accuracy = 0
            )
        }

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
                        registrarPerformance(ticker.symbol, true)
                        tradesExecutados.add(BacktestTradeResult(ticker.symbol, "GAIN", price, tpPrice, slPrice))
                    }
                    TradeResult.LOSS -> {
                        erros++
                        lucroTotal -= saldoDisponivel * (slPercent / 100)
                        registrarPerformance(ticker.symbol, false)
                        tradesExecutados.add(BacktestTradeResult(ticker.symbol, "LOSS", price, tpPrice, slPrice))
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


    suspend fun runMultiDayBacktest(days: Int = 3): BacktestResult {
        val tickers = RetrofitInstance.api.getTickers()
        val usdtPairs = tickers.filter { it.symbol.endsWith("USDT") }.take(50)

        var acertos = 0
        var erros = 0
        var lucroTotal = 0.0

        val btcCandlesRaw = getCandlesForTicker("BTCUSDT", interval = "1h", limit = days * 24) ?: return BacktestResult(0,0,0,0.0,0)
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

                val btcTrend = calcularTendenciaBTC(btcCandles.subList(0, i))
                if (btcTrend != "ALTISTA") continue

                val score = estimateAIScoreSnapshot(closes, rsi, bullishCount)
                val finalScore = adjustScoreBasedOnHistory(ticker.symbol, score)

                if (finalScore >= 8 && rsi != null && rsi in 50f..68f) {
                    val (tpPercent, slPercent) = determineSafeRiskReward(score)
                    val tpPrice = currentPrice * (1 + tpPercent / 100)
                    val slPrice = currentPrice * (1 - slPercent / 100)

                    val futureCandles = historicalCandles.subList(i, (i + 10).coerceAtMost(historicalCandles.size))
                    val result = simulatePriceMovement(futureCandles, currentPrice, tpPrice, slPrice)

                    when (result) {
                        TradeResult.GAIN -> {
                            acertos++
                            lucroTotal += saldoDisponivel * (tpPercent / 100)
                            tradesExecutados.add(BacktestTradeResult(ticker.symbol, "GAIN", currentPrice, tpPrice, slPrice))
                        }
                        TradeResult.LOSS -> {
                            erros++
                            lucroTotal -= saldoDisponivel * (slPercent / 100)
                            tradesExecutados.add(BacktestTradeResult(ticker.symbol, "LOSS", currentPrice, tpPrice, slPrice))
                        }
                        else -> {}
                    }

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

    /**
     * Nesse método temos :
        ✔️ Filtro por Tendência BTC (EMA + RSI + Contagem de Candles)
        ✔️ Controle de máximo de entradas por dia
        ✔️ Delay entre sinais do mesmo par
        ✔️ Filtro de Suporte/Resistência
        ✔️ Filtro de Candle Saudável
        ✔️ Cálculo de ATR para filtrar alta volatilidade
        ✔️ Análise de Book de Ordens (Buy Pressure)
        ✔️ Controle de Drawdown (máximo de perdas seguidas)
        ✔️ Ajuste de Score baseado no histórico de acertos por par
        ✔️ Exportação de Backtest (JSON e CSV)
        ✔️ Auto atualização de sinais na tela
        ✔️ Lógica de Risco por Trade (% do saldo)
        ✔️ Filtro de Confluência Multi-Timeframe (H1 e H4)
     * */

    suspend fun runMultiDayBacktestWithTrades(
        days: Int = 7,
        maxEntradasPorDia: Int = 5,
        riscoPercentualPorTrade: Double = 2.0,
        maxLossesSeguidos: Int = 3,
        minCandlesEntreEntradasMesmoPar: Int = 5
    ): Pair<BacktestResult, List<BacktestTradeResult>> {
        val tickers = RetrofitInstance.api.getTickers()
        val usdtPairs = tickers.filter { it.symbol.endsWith("USDT") }.take(80)

        var acertos = 0
        var erros = 0
        var lucroTotal = 0.0
        var perdasSeguidas = 0
        val tradesExecutados = mutableListOf<BacktestTradeResult>()
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

                // Tendência BTC
                //val btcTrend = calcularTendenciaBTC(btcCandles.subList(0, i))
                val safeEndIndex = minOf(i, btcCandles.size)
                val btcTrend = calcularTendenciaBTC(btcCandles.subList(0, safeEndIndex))
                if (btcTrend != "ALTISTA") continue

                // Limite de entradas por dia
                val candleTimestamp = historicalCandles[i].openTime
                val dataDoCandle = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(candleTimestamp))
                val entradasHoje = entradasPorDia[dataDoCandle] ?: 0
                if (entradasHoje >= maxEntradasPorDia) continue

                // Delay entre sinais no mesmo par
                val ultimoCandleEntrada = ultimoCandlePorPar[ticker.symbol] ?: -999
                if ((i - ultimoCandleEntrada) < minCandlesEntreEntradasMesmoPar) continue

                // Filtro: Candle Saudável
                if (!isHealthyCandle(historicalCandles[i])) continue

                // Filtro: Suporte/Resistência
                if (isNearSupportOrResistance(partialCandles, currentPrice)) continue

                // Filtro: ATR Volatilidade
                val atr = calculateATR(partialCandles)
                if (atr > (currentPrice * 0.03f)) continue  // Exemplo: Bloqueando se ATR > 3% do preço

                // Filtro: Order Book Pressure
                val buyPressure = getOrderBookPressure(ticker.symbol)
                if (buyPressure < 120f) continue  // Exemplo: Só operar se Buy Pressure for maior que 120%

                // Filtro: MultiTimeframe Confluence
                if (!isMultiTimeframeConfluence(ticker.symbol)) continue

                // Score
                val rawScore = estimateAIScoreWithEMA(closes, rsi, bullishCount)
                val finalScore = adjustScoreBasedOnHistory(ticker.symbol, rawScore)

                if (finalScore >= 8 && rsi != null && rsi in 50f..68f) {

                    val atrStopDistance = calculateDynamicStopLoss(partialCandles)  // Exemplo: 1.5x o ATR
                    val slPrice = currentPrice - atrStopDistance
                    val tpPercent = 3.0f  // Exemplo: 3% alvo fixo ou você pode criar uma lógica de Risk:Reward 2:1
                    val tpPrice = currentPrice * (1 + tpPercent / 100)


                    val futureCandles = historicalCandles.subList(i, (i + 10).coerceAtMost(historicalCandles.size))
                    val result = simulatePriceMovement(futureCandles, currentPrice, tpPrice, slPrice)

                    val valorInvestido = saldoDisponivel * (riscoPercentualPorTrade / 100)
                    val ganhoRealPercentual = when (result) {
                        TradeResult.GAIN -> tpPercent
                        TradeResult.LOSS -> -riscoPercentualPorTrade
                        else -> 0.0
                    }
                    lucroTotal += saldoDisponivel * (ganhoRealPercentual.toFloat() / 100)

                    when (result) {
                        TradeResult.GAIN -> {
                            acertos++
                            perdasSeguidas = 0
                            registrarPerformance(ticker.symbol, true)
                            tradesExecutados.add(BacktestTradeResult(ticker.symbol, "GAIN", currentPrice, tpPrice, slPrice))
                        }
                        TradeResult.LOSS -> {
                            erros++
                            perdasSeguidas++
                            registrarPerformance(ticker.symbol, false)
                            tradesExecutados.add(BacktestTradeResult(ticker.symbol, "LOSS", currentPrice, tpPrice, slPrice))

                            if (perdasSeguidas >= maxLossesSeguidos) {
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

    /**
     * ✔️ Muito mais proteção contra falsos sinais
     * ✔️ Antispoofing + Antiliquidez falsa + Wall detection
     * ✔️ Confirmação só se houver força real após o rompimento
     * ✔️ Filtros de volatilidade extrema
     * ✔️ Filtro de suporte/resistência + pressão de compra
     * ✔️ Controle de score com ajuste histórico
     * */
    suspend fun fetchSafeAnalysis(): List<MultiTFResult> {
        val tickers = RetrofitInstance.api.getTickers().filter { it.symbol.endsWith("USDT") }.take(100)
        val btcTrend = getBTCTendency()
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

            // ✅ Filtro de Volume Explosivo
            if (!isVolumeExploding(parsedCandles)) continue

            // Filtro: Suporte/Resistência
            if (isNearSupportOrResistance(parsedCandles, price)) continue

            // Filtro: Volatilidade (ATR)
            val atr = calculateATR(parsedCandles)
            val dynamicATRLimit = calculateDynamicATRLimit(parsedCandles)
            if (atr > price * dynamicATRLimit) continue

            // Filtro: Order Book - Buy Pressure
            val buyPressure = getOrderBookPressure(ticker.symbol)
            if (buyPressure < 120f) continue

            // Filtros de manipulação de mercado:
            if (detectWall(ticker.symbol)) continue
            if (detectSpoofing(ticker.symbol)) continue
            if (detectLiquidityVoid(ticker.symbol)) continue

            // Filtro: Confirmação de Rompimento com continuação real
            val resistanceLevel = parsedCandles.takeLast(30).maxOfOrNull { it.high } ?: continue
            val breakoutIndex = parsedCandles.size - 2  // Penúltima vela como candle de rompimento
            if (!isBreakoutConfirmed(parsedCandles, breakoutIndex, resistanceLevel)) continue

            val hasBadNews = checkNegativeNews(ticker.symbol)
            if (hasBadNews) continue

            val score = estimateAIScoreWithEMA(closes, rsi, bullishCount)
            val finalScore = adjustScoreBasedOnHistory(ticker.symbol, score)

            if (finalScore >= 8 && rsi in 50f..68f) {
                val (tpPercent, slPercent) = determineSafeRiskReward(finalScore)
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
                        trend = estimateConsistencyAI(finalScore, rsi, bullishCount),
                        consistency = estimateConsistency(ticker),
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


    private fun adjustScoreBasedOnHistory(symbol: String, originalScore: Int): Int {
        val (acertos, total) = performancePorPar[symbol] ?: Pair(0, 0)
        if (total < 5) return originalScore // Só ajusta se tiver pelo menos 5 sinais históricos

        val taxaAcerto = (acertos.toFloat() / total) * 100

        return when {
            taxaAcerto >= 70 -> originalScore + 1 // Recompensa histórico bom
            taxaAcerto < 50 -> originalScore - 1 // Penaliza histórico ruim
            else -> originalScore
        }.coerceIn(0, 10)
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

    private suspend fun getBTCTendency(): String {
        val btcCandlesRaw = getCandlesForTicker("BTCUSDT") ?: return "NEUTRA"
        val btcCandles = parseCandles(btcCandlesRaw)

        if (btcCandles.size < 21) return "NEUTRA"

        val closes = btcCandles.map { it.close }
        val ema9 = calculateEMA(closes, 9)
        val ema21 = calculateEMA(closes, 21)

        val rsi = calculateRSI(closes)
        val bullishCandles = btcCandles.takeLast(10).count { it.close > it.open }

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

    private fun calculateEMA(closes: List<Float>, period: Int): List<Float> {
        val ema = mutableListOf<Float>()
        val k = 2f / (period + 1)
        var previousEMA = closes.take(period).average().toFloat()

        for (price in closes.drop(period)) {
            val currentEMA = (price - previousEMA) * k + previousEMA
            ema.add(currentEMA)
            previousEMA = currentEMA
        }
        return ema
    }


    private fun estimateAIScoreSnapshot(closes: List<Float>, rsi: Float?, bullishCount: Int): Int {
        val change = if (closes.size >= 2) {
            ((closes.last() - closes[closes.size - 2]) / closes[closes.size - 2]) * 100
        } else 0f

        var score = 0
        if (change in 2f..5f) score += 3
        if (change > 5f) score += 4
        if (rsi != null && rsi in 55f..68f) score += 2
        if (bullishCount >= 3) score += 2
        return score.coerceAtMost(10)
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

    private fun estimateAIScoreWithEMA(
        closes: List<Float>,
        rsi: Float?,
        bullishCount: Int
    ): Int {
        var score = 0

        // 1 - Variação de preço nas últimas 2 horas
        val change = if (closes.size >= 2) {
            ((closes.last() - closes[closes.size - 2]) / closes[closes.size - 2]) * 100
        } else 0f

        if (change in 2f..5f) score += 3
        if (change > 5f) score += 4

        // 2 - Volume (já faz parte dos seus filtros, pode manter se quiser)

        // 3 - RSI
        if (rsi != null && rsi in 55f..68f) score += 2

        // 4 - Candles de Alta
        if (bullishCount >= 3) score += 2

        // 5 - EMA9 > EMA21 (Tendência positiva de curto prazo)
        val ema9 = calculateEMA(closes, 9)
        val ema21 = calculateEMA(closes, 21)
        if (ema9.isNotEmpty() && ema21.isNotEmpty() && ema9.last() > ema21.last()) {
            score += 2
        }

        return score.coerceAtMost(10)
    }

    /**
     * ✅ Suporte/Resistência
     * ✅ Cálculo de lucro real por candle
     * ✅ Filtro de candle saudável
     * */
    private fun isNearSupportOrResistance(candles: List<Candle>, currentPrice: Float): Boolean {
        val recentHighs = candles.takeLast(50).map { it.high }
        val recentLows = candles.takeLast(50).map { it.low }

        val resistance = recentHighs.maxOrNull() ?: return false
        val support = recentLows.minOrNull() ?: return false

        val distanciaResistencia = ((resistance - currentPrice) / currentPrice) * 100
        val distanciaSuporte = ((currentPrice - support) / currentPrice) * 100

        return distanciaResistencia <= 1.0f || distanciaSuporte <= 1.0f
    }

    private fun isHealthyCandle(candle: Candle): Boolean {
        val totalRange = candle.high - candle.low
        val bodySize = kotlin.math.abs(candle.close - candle.open)
        return totalRange > 0f && (bodySize / totalRange) >= 0.3f
    }

    fun exportTradesToJson(trades: List<BacktestTradeResult>): String {
        val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(trades)
    }

    fun exportTradesToCSV(trades: List<BacktestTradeResult>): String {
        val sb = StringBuilder()
        sb.append("Symbol,Result,EntryPrice,TPPrice,SLPrice\n")
        for (trade in trades) {
            sb.append("${trade.symbol},${trade.result},${trade.entryPrice},${trade.takeProfit},${trade.stopLoss}\n")
        }
        return sb.toString()
    }

    fun saveToFile(context: Context, fileName: String, content: String) {
        val file = File(context.filesDir, fileName)
        file.writeText(content)
    }

    //cálculo de ATR nas últimas 14 velas:
    fun calculateATR(candles: List<Candle>, period: Int = 14): Float {
        if (candles.size < period + 1) return 0f

        var atr = 0f
        for (i in 1..period) {
            val high = candles[candles.size - i].high
            val low = candles[candles.size - i].low
            val closePrev = candles[candles.size - i - 1].close
            val tr = maxOf(high - low, kotlin.math.abs(high - closePrev), kotlin.math.abs(low - closePrev))
            atr += tr
        }
        return atr / period
    }


    //função para checar tendência em outro timeframe:
    suspend fun isMultiTimeframeConfluence(symbol: String): Boolean {
        val h1Candles = getCandlesForTicker(symbol, interval = "1h", limit = 50) ?: return false
        val h4Candles = getCandlesForTicker(symbol, interval = "4h", limit = 50) ?: return false
        val h1Trend = calcularTendenciaGeral(parseCandles(h1Candles))
        val h4Trend = calcularTendenciaGeral(parseCandles(h4Candles))
        return h1Trend == "ALTISTA" && h4Trend == "ALTISTA"
    }

    fun calcularTendenciaGeral(candles: List<Candle>): String {
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

    suspend fun getOrderBookPressure(symbol: String): Float {
        val response = RetrofitInstance.api.getOrderBook(symbol, 10)
        if (response.isSuccessful) {
            val book = response.body() ?: return 0f
            val buy = book.bids.sumOf { it[1].toDoubleOrNull() ?: 0.0 }
            val sell = book.asks.sumOf { it[1].toDoubleOrNull() ?: 0.0 }
            return if (sell == 0.0) 0f else ((buy / sell) * 100).toFloat()
        }
        return 0f
    }

    suspend fun detectWall(symbol: String): Boolean {
        val orderBook = RetrofitInstance.api.getOrderBook(symbol, 20).body() ?: return false
        val bigBuyWall = orderBook.bids.any { it[1].toDoubleOrNull() ?: 0.0 > 100_000 }
        val bigSellWall = orderBook.asks.any { it[1].toDoubleOrNull() ?: 0.0 > 100_000 }
        return bigBuyWall || bigSellWall
    }

    suspend fun detectSpoofing(symbol: String): Boolean {
        val orderBook = RetrofitInstance.api.getOrderBook(symbol, 20).body() ?: return false
        val spoofThreshold = 50_000.0
        val largeOrders = orderBook.bids.count { it[1].toDoubleOrNull() ?: 0.0 > spoofThreshold } +
                orderBook.asks.count { it[1].toDoubleOrNull() ?: 0.0 > spoofThreshold }
        return largeOrders >= 4  // Exemplo: 4 ou mais grandes ordens de cada lado pode indicar spoofing
    }

    suspend fun detectLiquidityVoid(symbol: String): Boolean {
        val orderBook = RetrofitInstance.api.getOrderBook(symbol, 20).body() ?: return false
        val totalBuy = orderBook.bids.sumOf { it[1].toDoubleOrNull() ?: 0.0 }
        val totalSell = orderBook.asks.sumOf { it[1].toDoubleOrNull() ?: 0.0 }
        return totalBuy < 10_000 && totalSell < 10_000
    }

    /**
     * ✔️ Elimina rompimentos falsos (false breakouts)
     * ✔️ Sinal só entra com continuação real de mercado
     * ✔️ Reduz drasticamente as entradas em "fake pumps" ou "shadow breaks"
     * ✔️ Aumenta a precisão no pós-rompimento
     * */
    fun isBreakoutConfirmed(candles: List<Candle>, currentIndex: Int, breakoutLevel: Float): Boolean {
        if (currentIndex + 1 >= candles.size) return false  // Não tem candle futuro para confirmar

        val breakoutCandle = candles[currentIndex]
        val nextCandle = candles[currentIndex + 1]

        // Confirmar se houve rompimento de verdade
        val brokeAbove = breakoutCandle.close > breakoutLevel

        // Confirmar se o candle seguinte foi de continuação forte
        val bodySize = kotlin.math.abs(nextCandle.close - nextCandle.open)
        val totalRange = nextCandle.high - nextCandle.low
        val isStrongBullish = nextCandle.close > nextCandle.open && totalRange > 0f && (bodySize / totalRange) > 0.6f

        return brokeAbove && isStrongBullish
    }

    private fun calculateDynamicATRLimit(candles: List<Candle>): Float {
        if (candles.size < 20) return 0.03f  // Padrão se não tiver dados suficientes

        // Calcula a variação percentual de cada candle (close-to-close)
        val variations = candles.windowed(2, 1).map { (prev, curr) ->
            if (prev.close != 0f)
                kotlin.math.abs((curr.close - prev.close) / prev.close) * 100
            else 0f
        }

        val mean = variations.average().toFloat()
        val variance = variations.map { (it - mean).pow(2) }.average().toFloat()
        val stddev = sqrt(variance)

        // Agora define o limite máximo de ATR baseado no desvio padrão
        return when {
            stddev < 1f -> 0.02f  // Mercado calmo → tolera até 2%
            stddev in 1f..2f -> 0.03f
            stddev in 2f..3f -> 0.04f
            else -> 0.05f  // Mercado muito volátil → aceita até 5%
        }
    }

    private fun isVolumeExploding(candles: List<Candle>): Boolean {
        if (candles.size < 21) return false

        val recentVolumes = candles.takeLast(21).dropLast(1).map { it.volume }
        val averageVolume = recentVolumes.average().toFloat()
        val currentVolume = candles.last().volume

        return currentVolume >= averageVolume * 1.5f
    }

    /**
     * ✔️ Adapta o Stop Loss ao comportamento real de volatilidade daquele ativo
     * ✔️ Protege contra "Stop prematuro" em ativos que têm range diário mais largo
     * ✔️ Evita SL muito apertado em momentos de alta oscilação
     * */
    private fun calculateDynamicStopLoss(candles: List<Candle>, atrMultiplier: Float = 1.5f): Float {
        val atr = calculateATR(candles)
        return atr * atrMultiplier
    }

    suspend fun checkNegativeNews(symbol: String): Boolean {
        return try {
            val coinId = symbolId(symbol)
            val response = CoinGeckoInstance.api.getStatusUpdates(coinId)

            if (response.isSuccessful) {
                val newsList = response.body()?.status_updates.orEmpty()
                val negativeKeywords = listOf("hack", "exploit", "scam", "rug pull", "regulation", "ban", "exploit", "lawsuit")

                newsList.any { news ->
                    negativeKeywords.any { keyword ->
                        news.description.contains(keyword, ignoreCase = true)
                    }
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false  // Se der erro na API, por segurança não bloquear
        }
    }


    fun symbolId(symbol: String): String {
        return when {
            symbol.startsWith("BTC") -> "bitcoin"
            symbol.startsWith("ETH") -> "ethereum"
            symbol.startsWith("BNB") -> "binancecoin"
            symbol.startsWith("XRP") -> "ripple"
            symbol.startsWith("ADA") -> "cardano"
            else -> "bitcoin" // fallback
        }
    }

    fun getTradeRecommendation(): String {
        val now = java.util.Calendar.getInstance()
        val hour = now.get(java.util.Calendar.HOUR_OF_DAY)
        val dayOfWeek = now.get(java.util.Calendar.DAY_OF_WEEK)

        val isWeekend = dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY

        return when {
            isWeekend -> "Finais de semana: Evite Day Trade. Se for operar, prefira Swing Trade."
            hour in 9..16 -> "Horário de mercado aberto: Bom momento para Day Trade!"
            hour in 16..20 -> "Fim de tarde: Momento ok para Swing Trade de curto prazo."
            hour in 20..23 || hour in 0..8 -> "Mercado com menor liquidez agora: Melhor evitar operações."
            else -> "Horário indefinido: Avalie o mercado antes de operar."
        }
    }

    suspend fun getSmartTradeRecommendation(symbol: String): String {
        try {
            val now = java.util.Calendar.getInstance()
            val hour = now.get(java.util.Calendar.HOUR_OF_DAY)
            val dayOfWeek = now.get(java.util.Calendar.DAY_OF_WEEK)
            val isWeekend = dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY

            val candlesRaw = getCandlesForTicker(symbol, interval = "1h", limit = 10) ?: return "Sem dados suficientes para o par $symbol."

            val candles = parseCandles(candlesRaw)
            if (candles.size < 5) return "Poucos candles disponíveis para análise de $symbol."

            // 1. Volume Médio das últimas 5 velas
            val averageVolume = candles.takeLast(5).map { it.volume }.average()

            // 2. Volatilidade média (Range Alto-Baixo)
            val averageRange = candles.takeLast(5).map { it.high - it.low }.average()

            // 3. Condições de Mercado
            val isVolumeHigh = averageVolume > 500_000f  // Ajuste esse valor conforme seu ativo
            val isVolatilityHigh = averageRange > 0.015f  // Exemplo: 1,5% de range médio

            return when {
                isWeekend -> {
                    if (isVolumeHigh) "Finais de semana com volume alto: Swing Trade pode ser uma boa."
                    else "Finais de semana e volume baixo: Melhor evitar operações."
                }
                hour in 9..16 && isVolumeHigh && isVolatilityHigh -> "Mercado ativo e volátil: Bom momento para Day Trade no par $symbol."
                hour in 16..20 && isVolumeHigh -> "Fim de tarde com bom volume: Swing Trade de curto prazo indicado no par $symbol."
                hour in 20..23 || hour in 0..8 -> "Mercado com liquidez reduzida nesse horário. Evite operações no par $symbol."
                else -> "Condições neutras: Analise o gráfico antes de decidir operar o par $symbol."
            }

        } catch (e: Exception) {
            return "Erro ao gerar recomendação: ${e.message}"
        }
    }

    fun getTradeRecommendationLive(symbol: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val now = java.util.Calendar.getInstance()
                val hour = now.get(java.util.Calendar.HOUR_OF_DAY)
                val dayOfWeek = now.get(java.util.Calendar.DAY_OF_WEEK)
                val isWeekend = dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY

                val candlesRaw = getCandlesForTicker(symbol, interval = "1h", limit = 10)
                if (candlesRaw.isNullOrEmpty()) {
                    onResult("Sem dados suficientes para o par $symbol.")
                    return@launch
                }

                val candles = parseCandles(candlesRaw)
                if (candles.size < 5) {
                    onResult("Poucos candles disponíveis para análise de $symbol.")
                    return@launch
                }

                // Análise de volume médio nas últimas 5 velas
                val averageVolume = candles.takeLast(5).map { it.volume }.average()

                // Análise de volatilidade média (range alto-baixo nas últimas 5 velas)
                val averageRange = candles.takeLast(5).map { it.high - it.low }.average()

                val isVolumeHigh = averageVolume > 500_000f  // Ajuste conforme seu ativo
                val isVolatilityHigh = averageRange > 0.015f  // Exemplo: range médio de 1,5%

                val recommendation = when {
                    isWeekend -> {
                        if (isVolumeHigh) "Finais de semana com volume alto: Swing Trade pode ser uma boa."
                        else "Finais de semana e volume baixo: Melhor evitar operações."
                    }
                    hour in 9..16 && isVolumeHigh && isVolatilityHigh -> "Mercado ativo e volátil: Bom momento para Day Trade no par $symbol."
                    hour in 16..20 && isVolumeHigh -> "Fim de tarde com bom volume: Swing Trade de curto prazo indicado no par $symbol."
                    hour in 20..23 || hour in 0..8 -> "Mercado com liquidez reduzida nesse horário. Evite operações no par $symbol."
                    else -> "Condições neutras: Analise o gráfico antes de decidir operar o par $symbol."
                }

                onResult(recommendation)

            } catch (e: Exception) {
                onResult("Erro ao gerar recomendação: ${e.message}")
            }
        }
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









