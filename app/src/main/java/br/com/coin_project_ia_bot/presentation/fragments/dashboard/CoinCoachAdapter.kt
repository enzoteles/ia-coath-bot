package br.com.coin_project_ia_bot.presentation.fragments.dashboard

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.coin_project_ia_bot.Ticker
import br.com.coin_project_ia_bot.databinding.ItemCoinBinding

class CoinCoachAdapter(private var tickers: List<Ticker>) :
    RecyclerView.Adapter<CoinCoachAdapter.TickerViewHolder>() {

    inner class TickerViewHolder(private val binding: ItemCoinBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(ticker: Ticker) {
            binding.symbol.text = ticker.symbol
            binding.lastPrice.text = "Preço: $${ticker.lastPrice}"
            binding.change.text = "Variação: ${ticker.priceChangePercent}% (${ticker.priceChange})"
            binding.volume.text = "Volume: ${ticker.quoteVolume} USDT"
            binding.highLow.text = "Máx/Mín: ${ticker.highPrice} / ${ticker.lowPrice}"
            binding.bidAsk.text =
                "Compra: ${ticker.bidPrice} (${ticker.bidQty})  |  Venda: ${ticker.askPrice} (${ticker.askQty})"

            // Coloração condicional para pump e potencial
            val change = ticker.priceChangePercent.toFloatOrNull() ?: 0f
            val price = ticker.lastPrice.toFloatOrNull() ?: 0f
            val high = ticker.highPrice.toFloatOrNull() ?: 0f
            val low = ticker.lowPrice.toFloatOrNull() ?: 0f

            val rangePercent = if (price != 0f) (high - low) / price else 0f

            when {
                change > 5 && rangePercent > 0.03 -> {
                    // Pump
                    binding.llmain.setBackgroundColor(Color.parseColor("#FFCDD2")) // Vermelho claro
                }
                change in 2.0..5.0 -> {
                    // Potencial crescimento
                    binding.llmain.setBackgroundColor(Color.parseColor("#FFF9C4")) // Amarelo claro
                }
                else -> {
                    // Sem destaque
                    binding.llmain.setBackgroundColor(Color.TRANSPARENT)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TickerViewHolder {
        val binding = ItemCoinBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TickerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TickerViewHolder, position: Int) {
        holder.bind(tickers[position])
    }

    override fun getItemCount(): Int = tickers.size
}
