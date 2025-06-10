package br.com.coin_project_ia_bot.presentation.fragments.recommend

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.coin_project_ia_bot.BinanceApi
import br.com.coin_project_ia_bot.domain.model.Recommendation
import br.com.coin_project_ia_bot.presentation.utils.TrendAIAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecommendViewModel(
    private val api: BinanceApi,
    private val trendAnalyzer: TrendAIAnalyzer
) : ViewModel() {

    private val _recommendations = MutableLiveData<List<Recommendation>>()
    val recommendations: LiveData<List<Recommendation>> = _recommendations

    fun loadRecommendations() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tickers = api.getTickers().filter { it.symbol.endsWith("USDT") }
                val top3 = tickers.mapNotNull { ticker ->
                    val volume = ticker.quoteVolume.toFloatOrNull() ?: return@mapNotNull null
                    val change = ticker.priceChangePercent.toFloatOrNull() ?: 0f
                    if (volume < 1_000_000 || change < 1f) return@mapNotNull null

                    val trend = trendAnalyzer.analyze(ticker.symbol)
                    val reason = "Volume ↑, mudança: $change%, tendência: $trend"
                    Recommendation(ticker.symbol, change, reason)
                }.sortedByDescending { it.changePercent }.take(3)

                _recommendations.postValue(top3)
            } catch (e: Exception) {
                e.printStackTrace()
                _recommendations.postValue(emptyList())
            }
        }
    }
}
