package br.com.coin_project_ia_bot.presentation.fragments.recommend

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.coin_project_ia_bot.databinding.ItemRecommendationBinding
import br.com.coin_project_ia_bot.domain.model.Recommendation

class RecommendAdapter(private val list: List<Recommendation>) :
    RecyclerView.Adapter<RecommendAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemRecommendationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecommendationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.tvSymbol.text = item.symbol
        holder.binding.tvChange.text = "Variação: %.2f%%".format(item.changePercent)
        holder.binding.tvReason.text = item.reason
    }

    override fun getItemCount(): Int = list.size
}
