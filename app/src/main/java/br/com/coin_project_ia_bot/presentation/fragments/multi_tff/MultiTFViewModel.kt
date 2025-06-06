package br.com.coin_project_ia_bot.presentation.fragments.multi_tff

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.coin_project_ia_bot.RetrofitInstance
import br.com.coin_project_ia_bot.Ticker
import br.com.coin_project_ia_bot.domain.model.MultiTFResult
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.TickerAnalysis
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.calculateRSI
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.countBullishCandles
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.getCandlesForTicker
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.getClosesForTicker
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.parseCandles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MultiTFViewModel : ViewModel() {

    val multiTFData = MutableLiveData<List<MultiTFResult>>()
    private val _investmentSignal = MutableLiveData<String>()
    val investmentSignal: LiveData<String> get() = _investmentSignal

    fun startAutoUpdate(intervalMillis: Long) {
        viewModelScope.launch {
            while (true) {
                val result = fetchMultiTFAnalysis()
                multiTFData.postValue(result)
                delay(intervalMillis)
            }
        }
    }

    private suspend fun fetchMultiTFAnalysis(): List<MultiTFResult> {
        val tickers = RetrofitInstance.api.getTickers()
        val usdtPairs = tickers.filter { it.symbol.endsWith("USDT") }

        val results = mutableListOf<MultiTFResult>()

        for (ticker in usdtPairs) {
            val closes = getClosesForTicker(ticker.symbol) // últimos fechamentos
            val candles = getCandlesForTicker(ticker.symbol) // últimos candles

            if (closes.isNotEmpty() && candles!!.isNotEmpty()) {
                val rsi = calculateRSI(closes)
                val bullishCount = countBullishCandles(parseCandles(candles))
                val score = estimateAIScore(ticker, rsi, bullishCount)
                val consistency = estimateConsistency(ticker)

                // Se atingir score alto, considerar como oportunidade
                if (score >= 7) {
                    val trend = estimateConsistencyAI(score, rsi, bullishCount)
                    results.add(MultiTFResult(ticker.symbol, ticker.priceChangePercent.toFloatOrNull() ?: 0f, trend, consistency, score, rsi, bullishCount))
                }
            }
        }

        return results.sortedByDescending { it.score }
    }

    private fun estimateAIScore(ticker: Ticker, rsi: Float?, bullishCount: Int): Int {
        val change = ticker.priceChangePercent.toFloatOrNull() ?: return 0
        val volume = ticker.volume.toFloatOrNull() ?: return 0
        var score = 0

        if (change in 2f..5f) score += 3
        if (change > 5f) score += 4
        if (volume > 1_000_000f) score += 2
        if (rsi != null && rsi in 55f..70f) score += 2
        if (bullishCount >= 3) score += 2

        return score.coerceAtMost(10)
    }

    private fun estimateConsistencyAI(score: Int, rsi: Float?, bullishCount: Int): String {
        return when {
            score >= 9 && rsi != null && rsi in 60f..70f && bullishCount >= 5 -> "🚀 Forte Tendência Confirmada"
            score in 7..8 && rsi != null && rsi in 55f..65f -> "📈 Boa Tendência"
            score in 5..6 -> "🔍 Potencial Emergente"
            else -> "⚠️ Baixa Confiabilidade"
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

    fun analyzeInvestmentDay(analysisList: List<TickerAnalysis>) {
        if (analysisList.isEmpty()) {
            _investmentSignal.value = "Sem dados suficientes para análise."
            return
        }

        val goodOpportunities = analysisList.filter {
            it.score in 7..10 &&
                    (it.rsi ?: 0f) in 55f..70f &&
                    it.bullishCount >= 3 &&
                    it.ticker.volume.toFloatOrNull()?.let { vol -> vol > 500_000f } == true &&
                    it.change in 2f..5f
        }

        val ratio = goodOpportunities.size.toFloat() / analysisList.size

        _investmentSignal.value = when {
            ratio > 0.5 -> "🔥 Hoje é um bom dia para investir. Muitas moedas com tendência forte!"
            ratio > 0.25 -> "⚠️ Atenção: poucas boas oportunidades. Gerencie seu risco."
            else -> "❌ Hoje não é ideal para investir. Aguarde melhor momento."
        }
    }


}
