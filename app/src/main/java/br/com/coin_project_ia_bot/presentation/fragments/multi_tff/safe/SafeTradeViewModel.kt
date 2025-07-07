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

// ================= SafeTradeViewModel.kt (refatorado) ===================

class SafeTradeViewModel : ViewModel() {

    val safeSignals = MutableLiveData<List<MultiTFResult>>()
    val tradesExecutados = mutableListOf<BacktestTradeResult>()
    private val performancePorPar = mutableMapOf<String, Pair<Int, Int>>()

    private val saldoDisponivel = 500.0
    private val riscoMaximo = 2.0

    fun startSafeSignalAutoUpdate(intervalMillis: Long) {
        viewModelScope.launch {
            while (true) {
                val result = getLiveSafeSignals()
                safeSignals.postValue(result)
                delay(intervalMillis)
            }
        }
    }

    suspend fun runHistoricalBacktest(
        days: Int = 7,
        maxEntriesPerDay: Int = 5,
        riskPerTrade: Double = 2.0,
        maxConsecutiveLosses: Int = 3,
        minCandlesBetweenSamePair: Int = 5
    ): Pair<BacktestResult, List<BacktestTradeResult>> {
        return BacktestEngine.runBacktest(
            days = days,
            saldo = saldoDisponivel,
            riskPerTrade = riskPerTrade,
            maxEntriesPerDay = maxEntriesPerDay,
            maxConsecutiveLosses = maxConsecutiveLosses,
            minCandlesBetweenSamePair = minCandlesBetweenSamePair,
            tradesExecutados = tradesExecutados,
            performancePorPar = performancePorPar
        )
    }

    suspend fun getLiveSafeSignals(): List<MultiTFResult> {
        return SignalAnalyzer.fetchSafeAnalysis(
            saldoDisponivel = saldoDisponivel,
            riscoMaximo = riscoMaximo,
            performancePorPar = performancePorPar
        )
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