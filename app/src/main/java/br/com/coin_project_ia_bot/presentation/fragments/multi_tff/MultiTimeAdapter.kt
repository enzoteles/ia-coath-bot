package br.com.coin_project_ia_bot.presentation.fragments.multi_tff

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.coin_project_ia_bot.databinding.ItemMultiTimeBinding

class MultiTimeAdapter(private val items: List<MultiTimeTicker>) :
    RecyclerView.Adapter<MultiTimeAdapter.MultiTimeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultiTimeViewHolder {
        val binding = ItemMultiTimeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MultiTimeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MultiTimeViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class MultiTimeViewHolder(private val binding: ItemMultiTimeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(ticker: MultiTimeTicker) {
            binding.tvSymbol.text = ticker.symbol
            binding.tvChange1m.text = "1m: ${ticker.change1m}%"
            binding.tvChange5m.text = "5m: ${ticker.change5m}%"
            binding.tvChange10m.text = "30m: ${ticker.change30m}%"
            binding.tvChange1h.text = "1h: ${ticker.change1h}%"

            // Cores para variação
            fun getColor(value: Float): Int {
                return when {
                    value >= 1f -> Color.parseColor("#C8E6C9") // verde claro
                    value <= -1f -> Color.parseColor("#FFCDD2") // vermelho claro
                    else -> Color.TRANSPARENT
                }
            }

            binding.tvChange1m.setBackgroundColor(getColor(ticker.change1m))
            binding.tvChange5m.setBackgroundColor(getColor(ticker.change5m))
            binding.tvChange10m.setBackgroundColor(getColor(ticker.change30m))
            binding.tvChange1h.setBackgroundColor(getColor(ticker.change1h))

            // Tendência consistente
            binding.tvTrend.text = if (ticker.consistentUpTrend) "Tendência de alta consistente" else "Tendência indefinida"
        }
    }
}
