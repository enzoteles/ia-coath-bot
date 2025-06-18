package br.com.coin_project_ia_bot.presentation.utils


import br.com.coin_project_ia_bot.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object OrderBookAnalyzer {

    suspend fun getBuyPressureRatio(symbol: String, limit: Int = 10): Float {
        return withContext(Dispatchers.IO) {
            val response = RetrofitInstance.api.getOrderBook(symbol, limit)
            if (response.isSuccessful) {
                val orderBook = response.body() ?: return@withContext 0f
                val totalBuy = orderBook.bids.sumOf { it[1].toDoubleOrNull() ?: 0.0 }
                val totalSell = orderBook.asks.sumOf { it[1].toDoubleOrNull() ?: 0.0 }
                return@withContext if (totalSell == 0.0) 0f else ((totalBuy / totalSell) * 100).toFloat()
            }
            0f
        }
    }

    suspend fun getSpreadPercent(symbol: String): Float {
        return withContext(Dispatchers.IO) {
            val response = RetrofitInstance.api.getOrderBook(symbol, 5)
            if (response.isSuccessful) {
                val orderBook = response.body() ?: return@withContext 0f
                val bestBid = orderBook.bids.firstOrNull()?.get(0)?.toFloatOrNull() ?: return@withContext 0f
                val bestAsk = orderBook.asks.firstOrNull()?.get(0)?.toFloatOrNull() ?: return@withContext 0f
                if (bestBid == 0f) return@withContext 0f
                return@withContext ((bestAsk - bestBid) / bestBid) * 100
            }
            0f
        }
    }

    suspend fun detectSellWall(symbol: String, limit: Int = 20, wallMultiplier: Float = 2.0f): Boolean {
        return withContext(Dispatchers.IO) {
            val response = RetrofitInstance.api.getOrderBook(symbol, limit)
            if (response.isSuccessful) {
                val orderBook = response.body() ?: return@withContext false
                val avgAskVolume = orderBook.asks.map { it[1].toFloatOrNull() ?: 0f }.average().toFloat()
                val biggestAskVolume = orderBook.asks.maxOfOrNull { it[1].toFloatOrNull() ?: 0f } ?: 0f
                return@withContext biggestAskVolume > (avgAskVolume * wallMultiplier)
            }
            false
        }
    }

    suspend fun detectBuyWall(symbol: String, limit: Int = 20, wallMultiplier: Float = 2.0f): Boolean {
        return withContext(Dispatchers.IO) {
            val response = RetrofitInstance.api.getOrderBook(symbol, limit)
            if (response.isSuccessful) {
                val orderBook = response.body() ?: return@withContext false
                val avgBidVolume = orderBook.bids.map { it[1].toFloatOrNull() ?: 0f }.average().toFloat()
                val biggestBidVolume = orderBook.bids.maxOfOrNull { it[1].toFloatOrNull() ?: 0f } ?: 0f
                return@withContext biggestBidVolume > (avgBidVolume * wallMultiplier)
            }
            false
        }
    }

    suspend fun getOrderBookImbalance(symbol: String, limit: Int = 10): Float {
        return withContext(Dispatchers.IO) {
            val response = RetrofitInstance.api.getOrderBook(symbol, limit)
            if (response.isSuccessful) {
                val orderBook = response.body() ?: return@withContext 0f
                val totalBuy = orderBook.bids.sumOf { it[1].toDoubleOrNull() ?: 0.0 }
                val totalSell = orderBook.asks.sumOf { it[1].toDoubleOrNull() ?: 0.0 }
                val total = totalBuy + totalSell
                if (total == 0.0) return@withContext 0f
                return@withContext ((totalBuy - totalSell) / total).toFloat() * 100
            }
            0f
        }
    }

    // Spoofing Detection: detectar se grandes ordens surgiram e sumiram rapidamente
    private val recentOrderBooks = mutableMapOf<String, List<List<String>>>()

    suspend fun detectSpoofing(symbol: String): Boolean {
        val response = RetrofitInstance.api.getOrderBook(symbol, 20)
        if (!response.isSuccessful) return false
        val currentOrderBook = response.body()?.bids ?: return false

        val lastSnapshot = recentOrderBooks[symbol]
        recentOrderBooks[symbol] = currentOrderBook

        if (lastSnapshot == null) return false

        // Se uma ordem grande (ex: > 50k USD) sumiu de um snapshot para o outro => possível spoofing
        for (i in lastSnapshot.indices) {
            val oldQty = lastSnapshot.getOrNull(i)?.get(1)?.toDoubleOrNull() ?: 0.0
            val newQty = currentOrderBook.getOrNull(i)?.get(1)?.toDoubleOrNull() ?: 0.0
            val price = lastSnapshot.getOrNull(i)?.get(0)?.toDoubleOrNull() ?: continue

            val notionalOld = oldQty * price
            val notionalNew = newQty * price

            if (notionalOld >= 50_000 && notionalNew < (notionalOld * 0.2)) {
                return true // Grande sumiço repentino
            }
        }

        return false
    }

    // Sudden Liquidity Void: se o book ficou subitamente "vazio" (poucas ordens no topo)
    suspend fun detectLiquidityVoid(symbol: String): Boolean {
        val response = RetrofitInstance.api.getOrderBook(symbol, 10)
        if (!response.isSuccessful) return false
        val orderBook = response.body() ?: return false

        val totalBuyLevels = orderBook.bids.size
        val totalSellLevels = orderBook.asks.size

        // Exemplo: se tiver menos de 3 níveis de bid ou ask ativos => vazio
        return (totalBuyLevels < 3 || totalSellLevels < 3)
    }

    suspend fun detectOrderWall(symbol: String, threshold: Double = 100000.0): Boolean {
        val response = RetrofitInstance.api.getOrderBook(symbol, 20)
        if (!response.isSuccessful) return false

        val orderBook = response.body() ?: return false

        val maxBuyWall = orderBook.bids.maxOfOrNull { it[1].toDoubleOrNull() ?: 0.0 } ?: 0.0
        val maxSellWall = orderBook.asks.maxOfOrNull { it[1].toDoubleOrNull() ?: 0.0 } ?: 0.0

        return maxBuyWall > threshold || maxSellWall > threshold
    }

    suspend fun detectSpoofing(symbol: String, imbalanceThreshold: Float = 4f): Boolean {
        val response = RetrofitInstance.api.getOrderBook(symbol, 20)
        if (!response.isSuccessful) return false

        val orderBook = response.body() ?: return false

        val totalBuy = orderBook.bids.sumOf { it[1].toDoubleOrNull() ?: 0.0 }
        val totalSell = orderBook.asks.sumOf { it[1].toDoubleOrNull() ?: 0.0 }

        if (totalSell == 0.0 || totalBuy == 0.0) return false

        val ratio = totalBuy / totalSell
        return ratio >= imbalanceThreshold || ratio <= (1 / imbalanceThreshold)
    }

    suspend fun detectLiquidityVoid(symbol: String, minTotalDepth: Double = 5000.0): Boolean {
        val response = RetrofitInstance.api.getOrderBook(symbol, 20)
        if (!response.isSuccessful) return true  // Se não conseguiu pegar o book, assume risco de liquidez

        val orderBook = response.body() ?: return true

        val totalBuy = orderBook.bids.sumOf { it[1].toDoubleOrNull() ?: 0.0 }
        val totalSell = orderBook.asks.sumOf { it[1].toDoubleOrNull() ?: 0.0 }
        val totalDepth = totalBuy + totalSell

        return totalDepth < minTotalDepth
    }




}
