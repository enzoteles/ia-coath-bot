package br.com.coin_project_ia_bot.presentation.fragments.multi_tff.strategy

import br.com.coin_project_ia_bot.presentation.utils.TickerAnalysis

object SignalGenerator {

    /**
     * primeira versão
     * */
    fun gerar(analises: List<TickerAnalysis>, estrategia: Estrategia): List<TickerAnalysis> {
        return analises.filter { ticker ->
            val rsi = ticker.rsi ?: return@filter false
            val volume = ticker.ticker.quoteVolume.toFloatOrNull() ?: return@filter false
            val change = ticker.ticker.priceChangePercent.toFloatOrNull() ?: return@filter false

            rsi in estrategia.rsiRange &&
                    volume > 50_000_000 &&
                    ticker.bullishCount >= 3 &&
                    ticker.score >= estrategia.scoreMin &&
                    change >= estrategia.variacaoMin
        }.sortedByDescending { it.score }.take(30)
    }


}
