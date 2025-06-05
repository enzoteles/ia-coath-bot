package br.com.coin_project_ia_bot.presentation.fragments.dashboard

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import br.com.coin_project_ia_bot.R
import br.com.coin_project_ia_bot.Ticker
import br.com.coin_project_ia_bot.databinding.ItemCoinBinding
import br.com.coin_project_ia_bot.presentation.utils.formatAsCurrency

class DashboardAdapter(private var analyses: List<TickerAnalysis>) :
    RecyclerView.Adapter<DashboardAdapter.TickerViewHolder>() {

    var context: Context? = null

    inner class TickerViewHolder(private val binding: ItemCoinBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(analyses: TickerAnalysis) {
            val ticker = analyses.ticker
            val vol = formatAsCurrency(ticker.quoteVolume)
            binding.symbol.text = ticker.symbol
            binding.lastPrice.text = "Preço: $${ticker.lastPrice}"
            binding.change.text = "Variação: ${ticker.priceChangePercent}% (${ticker.priceChange})"
            binding.volume.text = "Volume: $vol USDT"
            binding.highLow.text = "Máx/Mín: ${ticker.highPrice} / ${ticker.lowPrice}"
            binding.bidAsk.text =
                "Compra: ${ticker.bidPrice} (${ticker.bidQty})  |  Venda: ${ticker.askPrice} (${ticker.askQty})"

            // Novos campos
            binding.score.text = "Score: ${analyses.score}/10"
            binding.rsi.text = "RSI: ${"%.1f".format(analyses.rsi ?: 0f)}"
            binding.bullishCount.text = "Candles de Alta: ${analyses.bullishCount}"

            val change = ticker.priceChangePercent.toFloatOrNull() ?: 0f
            val price = ticker.lastPrice.toFloatOrNull() ?: 0f
            val high = ticker.highPrice.toFloatOrNull() ?: 0f
            val low = ticker.lowPrice.toFloatOrNull() ?: 0f
            val rangePercent = if (price != 0f) (high - low) / price else 0f


            // Status visual via ícone
            when {

                analyses.score in 7..10 &&
                        (analyses.rsi ?: 0f) in 55f..70f &&
                        analyses.bullishCount >= 3 &&
                        ticker.volume.toFloatOrNull()?.let { it > 500_000 } == true &&
                        analyses.change in 2f..5f -> {
                    binding.statusIcon.setImageResource(R.drawable.ic_pump_star)
                    binding.statusIcon.visibility = View.VISIBLE
                    val imageView = binding.statusIcon // ou findViewById(R.id.imageViewIcon)
                    val color = ContextCompat.getColor(
                        context,
                        R.color.colorGreen
                    ) // Pode ser colorGreen, etc.
                    imageView.setColorFilter(color)
                    binding.llmain.visibility = View.VISIBLE
                }

                change > 5 && rangePercent > 0.03 -> {
                    binding.statusIcon.setImageResource(R.drawable.ic_pump)
                    binding.statusIcon.visibility = View.VISIBLE
                    val imageView = binding.statusIcon // ou findViewById(R.id.imageViewIcon)
                    val color = ContextCompat.getColor(
                        context,
                        R.color.colorGreen
                    ) // Pode ser colorGreen, etc.
                    imageView.setColorFilter(color)
                    binding.llmain.visibility = View.VISIBLE
                }

                change in 2.0..5.0 -> {
                    binding.statusIcon.setImageResource(R.drawable.ic_upward)
                    binding.statusIcon.visibility = View.VISIBLE
                    val imageView = binding.statusIcon // ou findViewById(R.id.imageViewIcon)
                    val color = ContextCompat.getColor(
                        context,
                        R.color.colorYellow
                    ) // Pode ser colorGreen, etc.
                    imageView.setColorFilter(color)
                    binding.llmain.visibility = View.GONE
                }

                else -> {
                    binding.llmain.visibility = View.GONE
                    binding.statusIcon.visibility = View.GONE
                }
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TickerViewHolder {
        val binding = ItemCoinBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = parent.context
        return TickerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TickerViewHolder, position: Int) {
        holder.bind(analyses[position])
    }

    override fun getItemCount(): Int = analyses.size

    fun updateList(newList: List<TickerAnalysis>) {
        analyses = newList
        notifyDataSetChanged()
    }
}
