package br.com.coin_project_ia_bot.presentation.fragments.multi_tff.strategy

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object PerguntaGenerator {

    @RequiresApi(Build.VERSION_CODES.O)
    fun gerar(estrategia: Estrategia, condicao: String): String {
        val nowUTC = Instant.now().atZone(ZoneOffset.UTC)
        val timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(nowUTC)

        return """
Com base na condição atual de mercado com volatilidade $condicao e estratégia ativa de ${estrategia.nome}, me indique 3 criptomoedas com par USDT listadas na Binance ideais para esse perfil operacional, com os seguintes parâmetros:

• Take Profit alvo: ${"%.2f".format(estrategia.tp * 100)}%
• Stop Loss: ${"%.2f".format(estrategia.sl * 100)}%
• Validade máxima do sinal: ${estrategia.validadeMin} minutos

Critérios obrigatórios:
- Volume 24h acima de 50 milhões USD (informe o valor exato)
- RSI entre ${estrategia.rsiRange.start.toInt()} e ${estrategia.rsiRange.endInclusive.toInt()} (M15 e H1)
- Mínimo de 3 candles de alta consecutivos (M15)
- Cruzamento EMA 9 > EMA 21 (H1)
- Rompimento confirmado de resistência anterior no H1 (informe o valor da resistência)
- Volume explosivo nos últimos 30 minutos (30%+ da média M15)

Para cada criptomoeda, informe:
1. Preço atual (USDT)
2. Entrada sugerida, TP e SL com base em suporte/resistência
3. Tipo ideal de operação (${estrategia.nome})
4. Volume 24h exato
5. RSI exato (M15 e H1)
6. Nível de confiança (%) com base técnica/preditiva
7. Justificativa técnica em até 3 linhas
8. Timestamp da análise: $timestamp UTC

A resposta deve ser clara, técnica e imediatamente aplicável.
""".trimIndent()
    }

    fun gerar2(estrategia: Estrategia, condicao: String): String {
        return """
    Sou um sistema de bot institucional que opera em criptomoedas com pontos de entrada e saída baseados em fatores técnicos e de liquidez. Com base nos dados abaixo (Ticker, preço atual, volume 24h, RSI M15/H1, EMA9/EMA21 H1, 3 candles M15, ATR H1, volatilidade, eventos macro):

    1. Identifique até 3 ativos USDT na Binance com maior probabilidade (> 99%) de gerar trades bem-sucedidos.
    2. Para cada ativo, informe:
       - Tipo de operação (Scalp, Day‑Trade ou Swing), prazo e validade máxima (em minutos),
       - Nível sugerido de entrada e preços de TP (${estrategia.tp * 100}%) e SL (${estrategia.sl * 100}%) de acordo com suporte/ resistência ou ATR,
       - Justificativa técnica executável em 3 linhas,
       - Confiança estimada (%).
    3. Use modelos numéricos (ATR, volatilidade, volume, slippage estimado) e filtre eventos macro (FOMC, CPI). Informe o campo `"slippageMaximoEstimado"` com o valor percentual estimado de desvio entre entrada planejada e execução real para ordens de até 100k USD.
    4. Responda em JSON seguindo este formato:
    
    [
      {
        "ticker": "BTCUSDT",
        "operacao": "${estrategia.nome}",
        "validadeMin": ${estrategia.validadeMin},
        "entrada": 118000,
        "tp": 126260,
        "sl": 115640,
        "tipoTP": "${(estrategia.tp * 100)}%",
        "tipoSL": "${(estrategia.sl * 100)}%",
        "confianca": 99,
        "slippageMaximoEstimado": 0.25,
        "justificativa": "EMA9 cruzou EMA21 no H1, RSI H1=65 e explosão de volume +40%, rompimento de resistência em 118k confirmada."
      }
    ]
  """.trimIndent()
    }

}


