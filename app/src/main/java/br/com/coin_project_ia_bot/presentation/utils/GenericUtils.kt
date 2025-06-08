package br.com.coin_project_ia_bot.presentation.utils

fun determineRiskRanges(score: Int): Pair<String, String> {
    return when {
        score >= 9 -> Pair("5% - 8%", "2%")
        score in 7..8 -> Pair("3% - 5%", "1.5%")
        score in 5..6 -> Pair("1.5% - 3%", "1%")
        else -> Pair("?", "?")
    }
}
