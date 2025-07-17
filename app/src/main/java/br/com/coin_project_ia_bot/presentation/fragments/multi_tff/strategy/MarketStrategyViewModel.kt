package br.com.coin_project_ia_bot.presentation.fragments.multi_tff.strategy


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.coin_project_ia_bot.presentation.utils.TickerAnalysis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MarketStrategyViewModel : ViewModel() {

    private val _volatilidadeAtual = MutableLiveData<String>()
    val volatilidadeAtual: LiveData<String> = _volatilidadeAtual

    private val _estrategiaAtiva = MutableLiveData<Estrategia>()
    val estrategiaAtiva: LiveData<Estrategia> = _estrategiaAtiva

    private val _perguntaGerada = MutableLiveData<String>()
    val perguntaGerada: LiveData<String> = _perguntaGerada

    private val _sinaisFiltrados = MutableLiveData<List<TickerAnalysis>>()
    val sinaisFiltrados: LiveData<List<TickerAnalysis>> = _sinaisFiltrados

    @RequiresApi(Build.VERSION_CODES.O)
    fun analisarMercadoEAtualizar(analises: List<TickerAnalysis>) {
        val condicao = MarketConditionDetector.detectarCondicao(analises)
        _volatilidadeAtual.value = condicao

        val estrategia = Estrategia.definirPorCondicao(condicao)
        _estrategiaAtiva.value = estrategia

        val sinais = SignalGenerator.gerar(analises, estrategia)
        _sinaisFiltrados.value = sinais

        /*viewModelScope.launch {
            val json = simularIAJsonEmTempoReal()
            val sinais = processarJsonDaIA(json)
            _sinaisFiltrados.value = sinais
        }*/

        _perguntaGerada.value = PerguntaGenerator.gerar2(estrategia, condicao)
    }

    fun gerarRespostaIAFake(): String {
        return """
        [
          {
            "ticker": "BTCUSDT",
            "operacao": "Scalp",
            "validadeMin": 30,
            "entrada": 118000,
            "tp": 119770,
            "sl": 117410,
            "tipoTP": "1.5%",
            "tipoSL": "0.5%",
            "confianca": 99,
            "slippageMaximoEstimado": 0.22,
            "justificativa": "EMA9 cruzou EMA21 no H1, RSI H1=65 e volume explosivo."
          },
          {
            "ticker": "ETHUSDT",
            "operacao": "Scalp",
            "validadeMin": 30,
            "entrada": 3400,
            "tp": 3451,
            "sl": 3383,
            "tipoTP": "1.5%",
            "tipoSL": "0.5%",
            "confianca": 98,
            "slippageMaximoEstimado": 0.19,
            "justificativa": "RSI H1=61, EMA cruzado, forte candle verde com volume crescente."
          },
          {
            "ticker": "XRPUSDT",
            "operacao": "Scalp",
            "validadeMin": 30,
            "entrada": 3.20,
            "tp": 3.25,
            "sl": 3.18,
            "tipoTP": "1.5%",
            "tipoSL": "0.5%",
            "confianca": 97,
            "slippageMaximoEstimado": 0.18,
            "justificativa": "Rompimento de resistência confirmado em 3.18 com volume 30% acima da média."
          }
        ]
    """.trimIndent()
    }

    suspend fun simularIAJsonEmTempoReal(): String {
        delay(1500) // simula chamada externa
        return gerarRespostaIAFake() // reusa o JSON acima
    }
}
