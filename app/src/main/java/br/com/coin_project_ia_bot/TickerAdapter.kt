package br.com.coin_project_ia_bot

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.coin_project_ia_bot.databinding.ItemTickerBinding

class TickerAdapter(private val list: List<Ticker>) : RecyclerView.Adapter<TickerAdapter.TickerViewHolder>() {

    inner class TickerViewHolder(private val binding: ItemTickerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(ticker: Ticker) {
            binding.tvSymbol.text = ticker.symbol
            binding.tvPrice.text = "Preço: $${ticker.lastPrice}"
            binding.tvChange.text = "Variação: ${ticker.priceChangePercent}% (${ticker.priceChange})"
            binding.tvVolume.text = "Volume: ${ticker.quoteVolume} USDT"
            binding.tvHighLow.text = "Máx/Mín: ${ticker.highPrice} / ${ticker.lowPrice}"
            binding.tvBidAsk.text = "Compra: ${ticker.bidPrice} (${ticker.bidQty})  |  Venda: ${ticker.askPrice} (${ticker.askQty})"


            // Cor de fundo de acordo com o score
            when (calculateScore(ticker)) {
                10 -> binding.llmain.setBackgroundColor(Color.parseColor("#FFCDD2")) // Vermelho claro o pump
                5 -> binding.llmain.setBackgroundColor(Color.parseColor("#FFF9C4")) // Amarelo claro possível crescimento
                else -> binding.llmain.setBackgroundColor(Color.WHITE)
            }
        }

        fun calculateScore(ticker: Ticker): Int {
            val change = ticker.priceChangePercent.toFloatOrNull() ?: return 0
            val volume = ticker.volume.toFloatOrNull() ?: return 0
            val price = ticker.lastPrice.toFloatOrNull() ?: return 0
            val priceRange = (ticker.highPrice.toFloatOrNull() ?: 0f) - (ticker.lowPrice.toFloatOrNull() ?: 0f)

            return when {
                change >= 2 && change <= 5 && volume > 500000 -> 5
                change > 5 && priceRange / price > 0.03 -> 10 // Pump acelerado
                else -> 0
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TickerViewHolder {
        val binding = ItemTickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TickerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TickerViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int = list.size
}
