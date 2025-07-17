package br.com.coin_project_ia_bot.presentation.fragments.multi_tff.strategy

data class Estrategia(
    val nome: String,
    val tp: Double,
    val sl: Double,
    val validadeMin: Int,
    val rsiRange: ClosedFloatingPointRange<Float>,
    val scoreMin: Int,
    val variacaoMin: Float
) {
    companion object {
        fun definirPorCondicao(condicao: String): Estrategia = when (condicao) {
            "Alta" -> Estrategia(
                nome = "Swing",
                tp = 0.07,
                sl = 0.02,
                validadeMin = 360,
                rsiRange = 55f..70f,
                scoreMin = 7,
                variacaoMin = 2.0f
            )
            "Queda" -> Estrategia(
                nome = "Reversão",
                tp = 0.04,
                sl = 0.01,
                validadeMin = 240,
                rsiRange = 45f..55f,
                scoreMin = 6,
                variacaoMin = 1.5f
            )
            "Lateral" -> Estrategia(
                nome = "Scalp",
                tp = 0.015,
                sl = 0.005,
                validadeMin = 30,
                rsiRange = 50f..60f,
                scoreMin = 6,
                variacaoMin = 1.0f
            )
            else -> Estrategia(
                nome = "Stable",
                tp = 0.0,
                sl = 0.0,
                validadeMin = 0,
                rsiRange = 0f..100f,
                scoreMin = 0,
                variacaoMin = 0f
            )
        }
    }
}

