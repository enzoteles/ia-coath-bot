package br.com.coin_project_ia_bot.presentation.fragments.multi_tff.safe

import br.com.coin_project_ia_bot.RetrofitInstance
import br.com.coin_project_ia_bot.data.api.CoinGeckoInstance

object MarketDepthAnalyzer {
    suspend fun getOrderBookPressure(symbol: String): Float {
        val response = RetrofitInstance.api.getOrderBook(symbol, 10)
        if (response.isSuccessful) {
            val book = response.body() ?: return 0f
            val buy = book.bids.sumOf { it[1].toDoubleOrNull() ?: 0.0 }
            val sell = book.asks.sumOf { it[1].toDoubleOrNull() ?: 0.0 }
            return if (sell == 0.0) 0f else ((buy / sell) * 100).toFloat()
        }
        return 0f
    }

    suspend fun detectWall(symbol: String): Boolean {
        val orderBook = RetrofitInstance.api.getOrderBook(symbol, 20).body() ?: return false
        val bigBuyWall = orderBook.bids.any { it[1].toDoubleOrNull() ?: 0.0 > 100_000 }
        val bigSellWall = orderBook.asks.any { it[1].toDoubleOrNull() ?: 0.0 > 100_000 }
        return bigBuyWall || bigSellWall
    }

    suspend fun detectSpoofing(symbol: String): Boolean {
        val orderBook = RetrofitInstance.api.getOrderBook(symbol, 20).body() ?: return false
        val spoofThreshold = 50_000.0
        val largeOrders = orderBook.bids.count { it[1].toDoubleOrNull() ?: 0.0 > spoofThreshold } +
                orderBook.asks.count { it[1].toDoubleOrNull() ?: 0.0 > spoofThreshold }
        return largeOrders >= 4
    }

    suspend fun detectLiquidityVoid(symbol: String): Boolean {
        val orderBook = RetrofitInstance.api.getOrderBook(symbol, 20).body() ?: return false
        val totalBuy = orderBook.bids.sumOf { it[1].toDoubleOrNull() ?: 0.0 }
        val totalSell = orderBook.asks.sumOf { it[1].toDoubleOrNull() ?: 0.0 }
        return totalBuy < 10_000 && totalSell < 10_000
    }

    suspend fun checkNegativeNews(symbol: String): Boolean {
        return try {
            val coinId = symbolId(symbol)
            val response = CoinGeckoInstance.api.getStatusUpdates(coinId)
            if (response.isSuccessful) {
                val newsList = response.body()?.status_updates.orEmpty()
                val negativeKeywords = listOf("hack", "exploit", "scam", "rug pull", "regulation", "ban", "lawsuit")
                newsList.any { news ->
                    negativeKeywords.any { keyword ->
                        news.description.contains(keyword, ignoreCase = true)
                    }
                }
            } else false
        } catch (e: Exception) {
            false
        }
    }

    fun symbolId(symbol: String): String {
        return when {
            symbol.startsWith("BTC") -> "bitcoin"
            symbol.startsWith("ETH") -> "ethereum"
            symbol.startsWith("BNB") -> "binancecoin"
            symbol.startsWith("XRP") -> "ripple"
            symbol.startsWith("ADA") -> "cardano"
            else -> "bitcoin"
        }
    }
}
