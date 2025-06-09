package br.com.coin_project_ia_bot.presentation.fragments.multi_tff

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.coin_project_ia_bot.databinding.ItemMultiTfBinding
import br.com.coin_project_ia_bot.domain.model.MultiTFResult

class MultiTFAdapter : ListAdapter<MultiTFResult, MultiTFAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<MultiTFResult>() {
        override fun areItemsTheSame(old: MultiTFResult, new: MultiTFResult) = old.symbol == new.symbol
        override fun areContentsTheSame(old: MultiTFResult, new: MultiTFResult) = old == new
    }
) {
    inner class ViewHolder(val binding: ItemMultiTfBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MultiTFResult) {
            binding.tvSymbol.text = item.symbol
            binding.tvChange.text = "Variação 1h: ${"%.2f".format(item.oneHourChange)}%"

            binding.tvTrend.text = item.trend
            binding.tvConsistency.text = "Consistência: ${item.consistency}"
            binding.tvScore.text = "Score IA: ${item.score}"

            val lastPrice = item.lastPrice ?: 0f
            val (tpPercent, slPercent) = when (item.score) {
                in 9..10 -> 0.05f to 0.02f
                in 7..8 -> 0.04f to 0.015f
                in 5..6 -> 0.025f to 0.01f
                else -> 0.02f to 0.01f
            }

            val takeProfitPrice = lastPrice * (1 + tpPercent)
            val stopLossPrice = lastPrice * (1 - slPercent)

            binding.tvTakeProfit.text = "🎯 Take Profit: ${(tpPercent * 100).toInt()}% (R$ ${"%.4f".format(takeProfitPrice)})"
            binding.tvStopLoss.text = "🛑 Stop Loss: ${(slPercent * 100).toInt()}% (R$ ${"%.4f".format(stopLossPrice)})"
            binding.lastPrice.text = "Last Price: R$ ${"%.4f".format(lastPrice)}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMultiTfBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
