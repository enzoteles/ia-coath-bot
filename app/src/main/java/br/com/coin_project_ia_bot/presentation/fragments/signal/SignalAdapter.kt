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
            tvRsi.text = "RSI: %.1f".format(signal.rsi ?: 0f)
            tvBullish.text = "Bullish Candles: ${signal.bullishCount}"
            tvScore.text = "Score IA: ${signal.score}/10"
            tvConsistency.text = signal.consistency

            val (tp, sl) = when (signal.score) {
                in 9..10 -> Pair("Lucro Alvo: 5% a 8%", "Stop: -2%")
                in 7..8 -> Pair("Lucro Alvo: 3% a 5%", "Stop: -1.5%")
                in 5..6 -> Pair("Lucro Alvo: 1.5% a 3%", "Stop: -1%")
                else -> Pair("Lucro Alvo: <1.5%", "Stop: -0.5%")
            }
            tvTargetProfit.text = tp
            tvStopLoss.text = sl
        }
    }

    override fun getItemCount(): Int = signals.size
}
