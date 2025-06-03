package br.com.coin_project_ia_bot.presentation.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.coin_project_ia_bot.BinanceApiImpl
import br.com.coin_project_ia_bot.presentation.fragments.signal.CoinAnalyzer
import br.com.coin_project_ia_bot.domain.model.SignalTicker
import br.com.coin_project_ia_bot.presentation.fragments.signal.TelegramNotifier

class SignalWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val api = BinanceApiImpl() // Sua implementação da API
    private val analyzer = CoinAnalyzer(api)

    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        val pares = listOf("BTCUSDT", "ETHUSDT", "SOLUSDT", "XRPUSDT", "AVAXUSDT")
        val sinais = mutableListOf<SignalTicker>()

        for (symbol in pares) {
            try {
                println("🔍 Analisando $symbol...")

                val candles2h = analyzer.getCandles(symbol, "1m", 120)
                val change = analyzer.variationPercent(candles2h)

                if (change in 3f..5f && analyzer.isVolumeIncreasing(candles2h)) {
                    val h1 = analyzer.getCandles(symbol, "1h", 3)
                    val m15 = analyzer.getCandles(symbol, "15m", 4)

                    if (analyzer.isUptrend(h1) && analyzer.isUptrend(m15)) {
                        val signal = SignalTicker(symbol, change)
                        sinais.add(signal)

                        val msg = """
                            🚨 <b>Sinal de Compra Detectado</b>
                            💰 Par: <b>${signal.symbol}</b>
                            💸 Variação 2h: <b>%.2f%%</b>
                            🔎 Tendência confirmada em H1 e M15
                        """.trimIndent().format(signal.variation2h)

                        try {
                            TelegramNotifier.sendMessage(msg)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            println("❌ Falha ao enviar alerta do Telegram para $symbol")
                        }
                    } else {
                        println("↘️ $symbol: Tendência não confirmada")
                    }
                } else {
                    println("❌ $symbol: sem variação/volume suficiente")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                println("🔴 Erro ao processar $symbol: ${e.localizedMessage}")
            }
        }

        val duration = (System.currentTimeMillis() - startTime) / 1000
        println("✅ Fim da execução do SignalWorker. ${sinais.size} sinais encontrados em ${duration}s")

        return Result.success()
    }
}
