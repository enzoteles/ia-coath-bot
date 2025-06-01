package br.com.coin_project_ia_bot.presentation.fragments.multi_tff

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.coin_project_ia_bot.databinding.ItemMultiTfBinding

class MultiTFAdapter : ListAdapter<MultiTFResult, MultiTFAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<MultiTFResult>() {
        override fun areItemsTheSame(old: MultiTFResult, new: MultiTFResult) = old.symbol == new.symbol
        override fun areContentsTheSame(old: MultiTFResult, new: MultiTFResult) = old == new
    }
) {
    inner class ViewHolder(val binding: ItemMultiTfBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MultiTFResult) {
            binding.tvSymbol.text = item.symbol
            binding.tvChange.text = "Variação 1h: ${item.oneHourChange}%"
            binding.tvTrend.text = item.trend
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
