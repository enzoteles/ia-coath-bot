package br.com.coin_project_ia_bot.presentation.fragments.multi_tff.safe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.coin_project_ia_bot.R

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SafeTradeFragment : Fragment() {

    private lateinit var viewModel: SafeTradeViewModel
    private lateinit var adapter: SafeSignalsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_safe_trade, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[SafeTradeViewModel::class.java]
        adapter = SafeSignalsAdapter()

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerSafeSignals)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        viewModel.safeSignals.observe(viewLifecycleOwner) { sinais ->
            adapter.submitList(sinais)
        }

        // Atualização automática a cada minuto
        viewModel.startAutoUpdate(60_000L)

        // Botão de atualização manual
        val btnAtualizar = view.findViewById<Button>(R.id.btnAtualizarSinais)
        btnAtualizar.setOnClickListener {
            lifecycleScope.launch {
                val sinais = viewModel.fetchSafeAnalysis()
                adapter.submitList(sinais)
                Toast.makeText(requireContext(), "Sinais atualizados com sucesso!", Toast.LENGTH_SHORT).show()
            }
        }

        val btnBacktest = view.findViewById<Button>(R.id.btnBacktest)
        val txtResultado = view.findViewById<TextView>(R.id.txtResultadoBacktest)
        btnBacktest.setOnClickListener {
            lifecycleScope.launch {
                txtResultado.text = "Executando backtest, aguarde..."
                val result = viewModel.runBacktest()
                val resumo = """
            🔍 Backtest Finalizado:
            • Total de Trades: ${result.totalTrades}
            • Acertos: ${result.wins}
            • Erros: ${result.losses}
            • Acurácia: ${result.accuracy}%
            • Lucro Simulado: US$ ${"%.2f".format(result.profitUSDT)}
        """.trimIndent()
                txtResultado.text = resumo
            }
        }

    }
}
