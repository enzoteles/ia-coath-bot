package br.com.coin_project_ia_bot.presentation.fragments.multi_tff.safe

object PerformanceTracker {
    fun registrarPerformance(symbol: String, foiGain: Boolean, performancePorPar: MutableMap<String, Pair<Int, Int>>) {
        val (acertosAnteriores, totalAnterior) = performancePorPar[symbol] ?: Pair(0, 0)
        val novosAcertos = if (foiGain) acertosAnteriores + 1 else acertosAnteriores
        performancePorPar[symbol] = Pair(novosAcertos, totalAnterior + 1)
    }
}
