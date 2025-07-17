package br.com.coin_project_ia_bot.data.model

import androidx.lifecycle.viewmodel.CreationExtras
const val EMPTY = ""
const val EMPTY_INT = 0
const val EMPTY_LONG = 0L

data class Ticker(
    val askPrice: String = EMPTY,
    val askQty: String = EMPTY,
    val bidPrice: String = EMPTY,
    val bidQty: String = EMPTY,
    val closeTime: Long = EMPTY_LONG,
    val count: Int = EMPTY_INT,
    val firstId: String = EMPTY,
    val highPrice: String = EMPTY,
    val lastId: String = EMPTY,
    val lastPrice: String = EMPTY,
    val lastQty: String = EMPTY,
    val lowPrice: String = EMPTY,
    val openPrice: String = EMPTY,
    val openTime: Long = EMPTY_LONG,
    val prevClosePrice: String = EMPTY,
    val priceChange: String = EMPTY,
    val priceChangePercent: String = EMPTY,
    val quoteVolume: String = EMPTY,
    val symbol: String = EMPTY,
    val volume: String = EMPTY,
    val weightedAvgPrice: String = EMPTY,
)
