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
        holder.binding.tvSymbol.text = signal.symbol
        holder.binding.tvChange.text = "Variação em 2h: %.2f%%".format(signal.variation2h)
    }

    override fun getItemCount(): Int = signals.size
}
