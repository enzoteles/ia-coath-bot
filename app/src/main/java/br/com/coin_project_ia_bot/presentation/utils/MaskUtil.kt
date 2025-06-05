package br.com.coin_project_ia_bot.presentation.utils

import java.text.NumberFormat
import java.util.*

fun formatAsCurrency(value: Any?, locale: Locale = Locale.US): String {
    val number = when (value) {
        is String -> value.toDoubleOrNull()
        is Number -> value.toDouble()
        else -> null
    }

    return number?.let {
        NumberFormat.getCurrencyInstance(locale).format(it)
    } ?: ""
}
