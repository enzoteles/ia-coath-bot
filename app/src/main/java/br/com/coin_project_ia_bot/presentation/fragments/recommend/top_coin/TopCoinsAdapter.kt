package br.com.coin_project_ia_bot.presentation.fragments.recommend.top_coin

import br.com.coin_project_ia_bot.domain.model.SignalTicker

// TopCoinsAdapter.kt
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.coin_project_ia_bot.databinding.ItemTopCoinBinding

class TopCoinsAdapter :
    ListAdapter<SignalTicker, TopCoinsAdapter.TopCoinViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopCoinViewHolder {
        val binding = ItemTopCoinBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TopCoinViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TopCoinViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TopCoinViewHolder(private val binding: ItemTopCoinBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(signal: SignalTicker) {
            binding.apply {
                tvSymbol.text = signal.symbol
                tvScore.text = "Score IA: ${signal.score}/10"
                tvRsiBullish.text = "RSI: %.1f | Bullish: %d".format(signal.rsi, signal.bullishCount)
                tvTpSl.text = "🎯 TP: ${signal.takeProfitPrice} | 🛑 SL: ${signal.stopLossPrice}"
                tvInvest.text = "Sugestão: até %.0f%% do capital".format(signal.investmentPercent * 100)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SignalTicker>() {
            override fun areItemsTheSame(oldItem: SignalTicker, newItem: SignalTicker) =
                oldItem.symbol == newItem.symbol

            override fun areContentsTheSame(oldItem: SignalTicker, newItem: SignalTicker) =
                oldItem == newItem
        }
    }
}
