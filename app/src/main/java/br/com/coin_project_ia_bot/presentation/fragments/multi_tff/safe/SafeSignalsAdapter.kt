package br.com.coin_project_ia_bot.presentation.fragments.multi_tff.safe

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import br.com.coin_project_ia_bot.R
import br.com.coin_project_ia_bot.domain.model.MultiTFResult

class SafeSignalsAdapter : RecyclerView.Adapter<SafeSignalsAdapter.ViewHolder>() {

    private val items = mutableListOf<MultiTFResult>()

    fun submitList(newList: List<MultiTFResult>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val symbol = view.findViewById<TextView>(R.id.symbolText)
        val tp = view.findViewById<TextView>(R.id.tpText)
        val sl = view.findViewById<TextView>(R.id.slText)
        val entry = view.findViewById<TextView>(R.id.entryValueText)
        val copyBtn = view.findViewById<Button>(R.id.copyButton)

        fun bind(item: MultiTFResult) {
            symbol.text = item.symbol
            tp.text = item.takeProfit
            sl.text = item.stopLoss
            entry.text = "Entrada ideal: US$ ${"%.2f".format(item.idealEntryValue)}"
            copyBtn.setOnClickListener {
                val texto = "TP: ${item.takeProfitValue}, SL: ${item.stopLossValue}"
                val clipboard = itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("OCO", texto))
                Toast.makeText(itemView.context, "Copiado!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_safe, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])
}
