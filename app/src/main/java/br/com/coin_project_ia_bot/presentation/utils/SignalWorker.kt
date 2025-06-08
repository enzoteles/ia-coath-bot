package br.com.coin_project_ia_bot.presentation.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.coin_project_ia_bot.BinanceApiImpl
import br.com.coin_project_ia_bot.domain.model.SignalTicker
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.*
import br.com.coin_project_ia_bot.presentation.fragments.signal.TelegramNotifier
import br.com.coin_project_ia_bot.presentation.fragments.signal.CoinAnalyzer

class SignalWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val api = BinanceApiImpl()
    private val analyzer = CoinAnalyzer(api)

    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        val pares = listOf("BTCUSDT", "ETHUSDT", "SOLUSDT", "XRPUSDT", "AVAXUSDT")
        val sinais = mutableListOf<SignalTicker>()

        for (symbol in pares) {
            try {
                val candles2h = analyzer.getCandles(symbol, "1m", 120)
                val candlesParsed = candles2h
                val change = variationPercent(candlesParsed)

                if (change in 3f..10f && isVolumeIncreasing(candlesParsed)) {
                    val rsi = calculateRSI(candlesParsed.map { it.close }) ?: continue
                    val bullishCount = countBullishCandles(candlesParsed)

                    if (rsi in 55f..70f && bullishCount >= 3) {
                        val h1 = analyzer.getCandles(symbol, "1h", 3)
                        val m15 = analyzer.getCandles(symbol, "15m", 4)

                        if (isUptrend(h1) && isUptrend(m15)) {
                            val consistency = estimateConsistencyAI(rsi.toInt(), bullishCount.toFloat(), change.toInt())
                            val score = estimateAIScore(rsi, bullishCount, change, extractVolume(candlesParsed))
                            val (tp, sl) = determineRiskRanges(score)

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
                                takeProfitRange = tp,
                                stopLoss = sl
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
                                🎯 Lucro Alvo: <b>$tp</b>
                                🛑 Stop: <b>$sl</b>
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
}
