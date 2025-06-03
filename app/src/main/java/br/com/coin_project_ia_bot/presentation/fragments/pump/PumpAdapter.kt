package br.com.coin_project_ia_bot.presentation.fragments.pump

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.coin_project_ia_bot.R
import br.com.coin_project_ia_bot.domain.model.PumpTicker

class PumpAdapter(private val pumps: List<PumpTicker>) : RecyclerView.Adapter<PumpAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val symbol = view.findViewById<TextView>(R.id.tvSymbol)
        val change = view.findViewById<TextView>(R.id.tvChange)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pump, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = pumps.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = pumps[position]
        holder.symbol.text = item.symbol
        holder.change.text = "🚀 +%.2f%% nas últimas 2h".format(item.variation2h)
    }
}
