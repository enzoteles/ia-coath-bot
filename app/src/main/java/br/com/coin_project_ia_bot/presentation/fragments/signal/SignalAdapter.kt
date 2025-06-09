package br.com.coin_project_ia_bot.presentation.fragments.signal

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.coin_project_ia_bot.databinding.ItemSignalBinding
import br.com.coin_project_ia_bot.domain.model.SignalTicker

class SignalAdapter(private val signals: List<SignalTicker>) :
    RecyclerView.Adapter<SignalAdapter.SignalViewHolder>() {

    class SignalViewHolder(val binding: ItemSignalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SignalViewHolder {
        val binding = ItemSignalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SignalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SignalViewHolder, position: Int) {
        val signal = signals[position]
        with(holder.binding) {
            tvSymbol.text = signal.symbol
            tvChange.text = "Variação 2h: %.2f%%".format(signal.variation2h)
            tvRSI.text = "RSI: ${signal.rsi?.let { "%.1f".format(it) } ?: "N/A"}"
            tvBullish.text = "Bullish Candles: ${signal.bullishCount}"
            tvScore.text = "Score IA: ${signal.score}"
            tvConsistency.text = "Consistência: ${signal.consistency}"
            tvPrice.text = "Preço Atual: US$ %.2f".format(signal.lastPrice)
            tvTakeProfit.text = "Take Profit: US$ %.2f".format(signal.takeProfitPrice)
            tvStopLoss.text = "Stop Loss: US$ %.2f".format(signal.stopLossPrice)
        }
    }

    override fun getItemCount(): Int = signals.size
}
