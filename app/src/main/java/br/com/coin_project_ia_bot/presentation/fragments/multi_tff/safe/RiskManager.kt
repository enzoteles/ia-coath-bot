package br.com.coin_project_ia_bot.presentation.fragments.multi_tff.safe

object RiskManager {

    /**
     * Calcula quanto investir com base no saldo e risco máximo permitido.
     *
     * @param saldoTotal Total em USDT que o usuário tem disponível
     * @param riscoPercentual Percentual máximo de perda (ex: 2%)
     * @param distanciaStop Percentual do stop loss (ex: 1.5%)
     * @return Valor em USDT a ser investido nessa operação
     */
    fun calcularEntradaIdeal(
        saldoTotal: Double,
        riscoPercentual: Double,
        distanciaStop: Double
    ): Double {
        if (distanciaStop == 0.0) return 0.0

        val perdaPermitida = saldoTotal * (riscoPercentual / 100.0)
        return perdaPermitida / (distanciaStop / 100.0)
    }
}
