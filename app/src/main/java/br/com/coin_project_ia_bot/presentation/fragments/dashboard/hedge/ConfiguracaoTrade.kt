package br.com.coin_project_ia_bot.presentation.fragments.dashboard.hedge

data class ConfiguracaoTrade(
    val tipo: String,               // Swing, Day Trade, Scalp
    val takeProfit: Double,         // Ex: 0.045 = 4.5%
    val stopLoss: Double,           // Ex: 0.015 = 1.5%
    val tempoExpiracao: Int         // Em minutos
)
