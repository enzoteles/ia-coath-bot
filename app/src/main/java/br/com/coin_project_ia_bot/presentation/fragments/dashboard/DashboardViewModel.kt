package br.com.coin_project_ia_bot.presentation.fragments.dashboard

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.coin_project_ia_bot.RetrofitInstance
import br.com.coin_project_ia_bot.domain.model.Candle
import br.com.coin_project_ia_bot.presentation.MainActivity.Companion.USDT
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.hedge.ConfiguracaoTrade
import br.com.coin_project_ia_bot.presentation.utils.TickerAnalysis
import br.com.coin_project_ia_bot.presentation.utils.analyzeTicker
import br.com.coin_project_ia_bot.presentation.utils.getCandlesForTicker
import br.com.coin_project_ia_bot.presentation.utils.getClosesForTicker
import br.com.coin_project_ia_bot.presentation.utils.parseCandles
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt

const val QTD_COIN = 100
class DashboardViewModel : ViewModel() {

    private val _analyzedTickers = MutableLiveData<List<TickerAnalysis>>()
    val analyzedTickers: LiveData<List<TickerAnalysis>> = _analyzedTickers

    //isso é lógica pela hedge
    private val _resumoEstrategia = MutableLiveData<String>()
    val resumoEstrategia: LiveData<String> = _resumoEstrategia

    private val _configuracaoAtual = MutableLiveData<ConfiguracaoTrade>()
    val configuracaoAtual: LiveData<ConfiguracaoTrade> = _configuracaoAtual

    private val _volatilidadeAtual = MutableLiveData<String>()
    val volatilidadeAtual: LiveData<String> = _volatilidadeAtual


    fun fetchAndScoreTickers(minScore: Int = 7) {
        viewModelScope.launch {
            try {
                val tickers = RetrofitInstance.api.getTickers()
                    .filter { it.symbol.endsWith(USDT) }
                    .take(QTD_COIN)

                val analyses = coroutineScope {
                    tickers.map { ticker ->
                        async {
                            try {
                                val closes = getClosesForTicker(ticker.symbol)
                                val candles = getCandlesForTicker(ticker.symbol)
                                if (closes != null && candles != null) {
                                    val parsedCandles = parseCandles(candles)
                                    val analysis = analyzeTicker(ticker, closes, parsedCandles)
                                    if (analysis?.score!! >= minScore) analysis else null
                                } else null
                            } catch (e: Exception) {
                                Log.w("TickerFail", "Erro ao analisar ${ticker.symbol}: ${e.message}")
                                null
                            }
                        }
                    }.awaitAll().filterNotNull()
                }

                _analyzedTickers.value = analyses.sortedByDescending { it.score }

                if (analyses.isNotEmpty()) {
                    val candles = getCandlesForTicker(analyses.first().ticker.symbol)
                    if (candles != null) {
                        val volatilidade = detectarVolatilidade(parseCandles(candles))
                        val config = configurarTradePorVolatilidade(volatilidade)
                        val resumo = gerarResumoOperacional(volatilidade, config)
                        _resumoEstrategia.postValue(resumo)
                        _volatilidadeAtual.postValue(volatilidade)
                        _configuracaoAtual.postValue(config)
                    }
                }





            } catch (e: Exception) {
                Log.e("ViewModel", "Falha geral: ${e.message}")
            }
        }
    }

    fun gerarPerguntaDeNivel99Hedge(tickers: List<TickerAnalysis>): String {
        val candidatos = tickers.filter {
            val volume = it.ticker.quoteVolume.toFloatOrNull() ?: 0f
            val rsi = it.rsi ?: 0f
            val change = it.change
            val bullish = it.bullishCount

            volume > 50_000_000 &&
                    rsi in 55f..70f &&
                    bullish >= 3 &&
                    change in 2f..6f
        }.take(3)

        if (candidatos.isEmpty()) return "⚠️ Nenhuma moeda atende aos critérios de alta precisão no momento."

        val detalhes = candidatos.joinToString("\n\n") { analysis ->
            val symbol = analysis.ticker.symbol
            val price = analysis.ticker.lastPrice
            val rsi = "%.1f".format(analysis.rsi ?: 0f)
            val change = "%.2f".format(analysis.change)
            val volume = "%.1fM".format((analysis.ticker.quoteVolume.toFloatOrNull() ?: 0f) / 1_000_000)
            val tipo = analysis.operationType
            val entrada = "%.3f".format(price.toFloatOrNull() ?: 0f)
            val tp = "%.3f".format((price.toFloatOrNull() ?: 0f) * 1.045)
            val sl = "%.3f".format((price.toFloatOrNull() ?: 0f) * 0.98)
            val confianca = when {
                analysis.score >= 9 -> "Alta (95%)"
                analysis.score >= 8 -> "Média-Alta (90%)"
                else -> "Moderada (80%)"
            }

            """
        ▶️ $symbol
        • Preço atual: $entrada
        • RSI (H1): $rsi
        • Variação: $change%
        • Volume: $volume USDT
        • Tipo sugerido: $tipo
        • Entrada ideal: $entrada
        • Take Profit: $tp
        • Stop Loss: $sl
        • Confiança baseada em análise técnica: $confianca
        """.trimIndent()
        }

        return """
        🔎 Análise técnica avançada concluída.

        Com base nos indicadores RSI, candles de alta, volume, rompimento técnico e padrões estatísticos, selecionei as 3 criptomoedas com maior probabilidade de valorização entre 3% e 5% nas próximas 3 a 6 horas.

        ✅ Critérios considerados:
        - Volume 24h > 50 milhões USD
        - RSI entre 55 e 70 (H1)
        - Múltiplos candles de alta (M15)
        - Rompimento de resistência (H1)
        - Cruzamento EMA 9/21
        - Volume crescente nas últimas horas

        📈 Moedas com maior potencial de trade de curto prazo:

        $detalhes

        ℹ️ Indicações baseadas em score, consistência, liquidez e padrões operacionais de bots.
    """.trimIndent()
    }

    fun gerarPerguntaNivel99IAMutual(tickers: List<TickerAnalysis>): String {
        val candidatos = tickers.filter {
            val volume = it.ticker.quoteVolume.toFloatOrNull() ?: 0f
            val rsi = it.rsi ?: 0f
            val change = it.change
            val bullish = it.bullishCount

            volume > 50_000_000 &&
                    rsi in 55f..70f &&
                    bullish >= 3 &&
                    change in 2f..6f
        }.take(3)

        val moedas = if (candidatos.isEmpty()) "⚠️ Nenhuma moeda atende aos critérios de precisão no momento."
        else candidatos.joinToString("\n\n") { analysis ->
            val symbol = analysis.ticker.symbol
            val price = analysis.ticker.lastPrice
            val rsi = "%.1f".format(analysis.rsi ?: 0f)
            val change = "%.2f".format(analysis.change)
            val volume = "%.1fM".format((analysis.ticker.quoteVolume.toFloatOrNull() ?: 0f) / 1_000_000)
            val tipo = analysis.operationType
            val entrada = "%.3f".format(price.toFloatOrNull() ?: 0f)
            val tp = "%.3f".format((price.toFloatOrNull() ?: 0f) * 1.045)
            val sl = "%.3f".format((price.toFloatOrNull() ?: 0f) * 0.98)
            val confianca = when {
                analysis.score >= 9 -> "Alta (95%)"
                analysis.score >= 8 -> "Média-Alta (90%)"
                else -> "Moderada (80%)"
            }

            """
        ▶️ $symbol
        • Preço atual: $entrada
        • RSI (H1): $rsi
        • Variação: $change%
        • Volume: $volume USDT
        • Tipo: $tipo
        • Entrada ideal: $entrada
        • Take Profit: $tp
        • Stop Loss: $sl
        • Confiança técnica: $confianca
        • Justificativa: RSI alto, volume crescente e sequência de candles de alta
        """.trimIndent()
        }

        return """
        📊 Pergunta nível institucional para IA (Precisão 99%)

        Com base em análise técnica quantitativa, predição estatística e padrões de comportamento de preço, identifique 3 criptomoedas com par USDT na Binance com probabilidade superior a 90% de valorização entre 3% e 5% nas próximas 3 a 6 horas, ideais para operações de curto prazo (scalp ou day trade). Utilize os critérios:

        • Volume 24h > 50 milhões USD
        • RSI entre 55 e 70 (M15 e H1)
        • Mínimo de 3 candles de alta consecutivos (M15)
        • Rompimento de resistência (H1)
        • Cruzamento EMA 9 > EMA 21 (H1)
        • Volume explosivo nos últimos 30 minutos
        • Liquidez para ordens > 10 mil USD sem slippage

        Resultados filtrados:
        $moedas
    """.trimIndent()
    }

    private fun detectarVolatilidade(candles: List<Candle>): String {
        val closes = candles.map { it.close }
        val media = closes.average()
        val desvio = sqrt(closes.map { (it - media).pow(2) }.average())

        return when {
            desvio < 1.5 -> "Baixa"
            desvio < 3.5 -> "Média"
            else -> "Alta"
        }
    }

    private fun configurarTradePorVolatilidade(volatilidade: String): ConfiguracaoTrade {
        return when (volatilidade) {
            "Baixa" -> ConfiguracaoTrade("Swing", takeProfit = 0.06, stopLoss = 0.02, tempoExpiracao = 360)
            "Média" -> ConfiguracaoTrade("Day Trade", takeProfit = 0.045, stopLoss = 0.015, tempoExpiracao = 180)
            "Alta" -> ConfiguracaoTrade("Scalp", takeProfit = 0.025, stopLoss = 0.01, tempoExpiracao = 30)
            else -> ConfiguracaoTrade("Indefinido", 0.0, 0.0, 0)
        }
    }

    private fun gerarResumoOperacional(volatilidade: String, config: ConfiguracaoTrade): String {
        val estrategia = when (config.tipo) {
            "Swing" -> "Foco em rompimentos de consolidação. Entrada com confirmação e paciência na saída."
            "Day Trade" -> "Operações de algumas horas com sinais técnicos de força e volume."
            "Scalp" -> "Entrada e saída rápida com alvos curtos e proteção total contra reversão."
            else -> "Nenhuma estratégia definida."
        }

        return """
        📊 Volatilidade: $volatilidade
        
        ✅ Estratégia ativa: ${config.tipo}
        $estrategia

        🎯 Parâmetros:
        • Take Profit: ${"%.2f".format(config.takeProfit * 100)}%
        • Stop Loss: ${"%.2f".format(config.stopLoss * 100)}%
        • Validade do sinal: ${config.tempoExpiracao} minutos
    """.trimIndent()
    }


    fun gerarPerguntaIAComBaseNaEstrategia(config: ConfiguracaoTrade, volatilidade: String): String {
        return """
        Com base na condição atual de mercado com volatilidade $volatilidade e estratégia ativa de ${config.tipo}, me indique 3 criptomoedas com par USDT listadas na Binance ideais para esse perfil operacional, com os seguintes parâmetros:

        • Take Profit alvo: ${"%.2f".format(config.takeProfit * 100)}%
        • Stop Loss: ${"%.2f".format(config.stopLoss * 100)}%
        • Validade máxima do sinal: ${config.tempoExpiracao} minutos

        Critérios obrigatórios:

        - Volume 24h acima de 50 milhões USD (informe o valor exato)
        - RSI entre 55 e 70 nos timeframes M15 e H1 (com valor exato)
        - Mínimo de 3 candles de alta consecutivos em M15
        - Cruzamento EMA 9 > EMA 21 em H1
        - Rompimento confirmado de resistência anterior no H1 (informe o valor de resistência ocasião do rompimento)
        - Volume nos últimos 30 minutos pelo menos 30% superior à média M15

        Para cada uma das 3 criptomoedas, informe obrigatoriamente:
        1. Preço atual (em USDT)
        2. Entrada sugerida, TP e SL com níveis, baseados em suporte/resistência
        3. Tipo ideal de operação (Swing)
        4. Volume 24h exato
        5. RSI exato (M15 e H1)
        6. Nível de confiança (%) com base em backtest ou padrão estatístico
        7. Justificativa técnica com até 3 linhas
        8. Timestamp da análise (data e hora UTC)

        A resposta deve ser objetiva, técnica e imediatamente aplicável para execução.
    """.trimIndent()
    }


    //DETECTAR MERCADO
    fun detectarCenarioDeMercado(globalData: List<TickerAnalysis>): String {
        val btc = globalData.find { it.ticker.symbol == "BTCUSDT" } ?: return "Indefinido"
        val eth = globalData.find { it.ticker.symbol == "ETHUSDT" } ?: return "Indefinido"
        val mediaChange = (btc.change + eth.change) / 2

        return when {
            mediaChange >= 2.5 -> "Alta"
            mediaChange <= -2.5 -> "Baixa"
            else -> "Lateral"
        }
    }

}
