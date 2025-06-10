package br.com.coin_project_ia_bot.presentation.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.coin_project_ia_bot.BinanceApiImpl
import br.com.coin_project_ia_bot.domain.model.SignalTicker
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.*
import br.com.coin_project_ia_bot.presentation.fragments.signal.CoinAnalyzer
import br.com.coin_project_ia_bot.presentation.fragments.signal.manually.SharedPairsViewModel

class SignalWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val sharedPairs: SharedPairsViewModel
) : CoroutineWorker(context, workerParams) {

    private val api = BinanceApiImpl()
    private val analyzer = CoinAnalyzer(api)

    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        //val pares = listOf("BTCUSDT", "ETHUSDT", "SOLUSDT", "XRPUSDT", "AVAXUSDT")
        val pares = sharedPairs.selectedPairs.value?.toList().orEmpty()
        val sinais = mutableListOf<SignalTicker>()

        for (symbol in pares) {
            try {
                val candles2hRaw = analyzer.getCandles(symbol, "1m", 120)
                val candlesParsed = candles2hRaw

                if (candlesParsed.isEmpty()) continue

                val change = variationPercent(candlesParsed)

                if (change in 3f..10f && isVolumeIncreasing(candlesParsed)) {
                    val rsi = calculateRSI(candlesParsed.map { it.close }) ?: continue
                    val bullishCount = countBullishCandles(candlesParsed)

                    if (rsi in 55f..70f && bullishCount >= 3) {
                        val h1 = analyzer.getCandles(symbol, "1h", 3)
                        val m15 =analyzer.getCandles(symbol, "15m", 4)

                        if (isUptrend(h1) && isUptrend(m15)) {
                            val consistency = estimateConsistencyAI(rsi.toInt(), bullishCount.toFloat(), change.toInt())
                            val score = estimateAIScore(rsi, bullishCount, change, extractVolume(candlesParsed))
                            val currentPrice = candlesParsed.last().close

                            val (tpPercent, slPercent) = determineRiskRanges(score)
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
                                trend = consistency,
                                oneHourChange = 0f,
                                takeProfitRange = "%.4f".format(takeProfitPrice),
                                stopLoss = "%.4f".format(stopLossPrice),
                                lastPrice = currentPrice,
                                takeProfitPrice = takeProfitPrice.toString(),
                                stopLossPrice = stopLossPrice.toString()
                            )

                            sinais.add(signal)

                            val msg = """
                                🚨 <b>Sinal de Compra Detectado</b>
                                💰 Par: <b>${signal.symbol}</b>
                                📈 Variação 2h: <b>${"%.2f".format(change)}%</b>
                                📊 RSI: <b>${"%.1f".format(rsi)}</b>
                                ✨ Bullish Count: <b>$bullishCount</b>
                                🔹 Consistência: <b>${consistency}</b>
                                ⭐ Score IA: <b>$score /10</b>
                                🎯 Lucro Alvo: <b>${signal.takeProfitRange}</b>
                                🛑 Stop: <b>${signal.stopLoss}</b>
                                ✅ Confirmado em H1 e M15
                            """.trimIndent()

                            TelegramNotifier.sendMessage(msg)
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                println("🔴 Erro ao processar $symbol: ${e.localizedMessage}")
            }
        }

        val duration = (System.currentTimeMillis() - startTime) / 1000
        println("✅ Fim da execução. ${sinais.size} sinais encontrados em ${duration}s")

        return Result.success()
    }

    private fun determineRiskRanges(score: Int): Pair<Float, Float> {
        return when {
            score >= 9 -> 0.065f to 0.02f
            score in 7..8 -> 0.04f to 0.015f
            score in 5..6 -> 0.022f to 0.01f
            else -> 0.0f to 0.0f
        }
    }
}
