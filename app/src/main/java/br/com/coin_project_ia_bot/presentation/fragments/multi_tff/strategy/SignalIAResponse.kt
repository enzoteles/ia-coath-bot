package br.com.coin_project_ia_bot.presentation.fragments.multi_tff.strategy

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import br.com.coin_project_ia_bot.data.model.Ticker
import br.com.coin_project_ia_bot.presentation.utils.TickerAnalysis

data class SignalIAResponse(
    val ticker: String,
    val operacao: String,
    val validadeMin: Int,
    val entrada: Float,
    val tp: Float,
    val sl: Float,
    val tipoTP: String,
    val tipoSL: String,
    val confianca: Int,
    val slippageMaximoEstimado: Float,
    val justificativa: String
)

fun fromSignalIA(response: SignalIAResponse, tickersMap: Map<String, TickerAnalysis>): TickerAnalysis {
    val tickerReal = tickersMap[response.ticker] ?: Ticker(symbol = response.ticker)

    return TickerAnalysis(
        ticker = tickerReal as Ticker,
        score = response.confianca.toFloat() / 10f,
        rsi = null,
        bullishCount = 3,
        change = ((response.tp - response.entrada) / response.entrada) * 100,
        consistency = "Alta",
        operationType = response.operacao,
        slippageMaximoEstimado = response.slippageMaximoEstimado
    )
}


fun processarJsonDaIA(json: String, tickersMap: Map<String, TickerAnalysis>): List<TickerAnalysis> {
    return try {
        val gson = Gson()
        val tipo = object : TypeToken<List<SignalIAResponse>>() {}.type
        val sinaisIA: List<SignalIAResponse> = gson.fromJson(json, tipo)

        sinaisIA
            .map { fromSignalIA(it, tickersMap) }
            .filter { it.slippageMaximoEstimado != null && it.slippageMaximoEstimado!! <= 0.25f && it.score >= 8f }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

