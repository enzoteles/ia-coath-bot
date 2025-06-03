package br.com.coin_project_ia_bot.presentation.fragments.pump.alert

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import br.com.coin_project_ia_bot.R
import br.com.coin_project_ia_bot.domain.model.PumpTicker

class PumpAlertAdapter(private val tickers: List<PumpTicker>) :
    RecyclerView.Adapter<PumpAlertAdapter.PumpViewHolder>() {

    class PumpViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val symbol: TextView = view.findViewById(R.id.tvSymbol)
        val variation: TextView = view.findViewById(R.id.tvVariation)
        val card: CardView = view.findViewById(R.id.cardPump)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PumpViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pump_alert, parent, false)
        return PumpViewHolder(view)
    }

    override fun onBindViewHolder(holder: PumpViewHolder, position: Int) {
        val item = tickers[position]
        holder.symbol.text = item.symbol
        holder.variation.text = "Variação: %.2f%%".format(item.variation2h)
        holder.card.setCardBackgroundColor(Color.parseColor("#FFCDD2")) // vermelho claro
    }

    override fun getItemCount(): Int = tickers.size
}
