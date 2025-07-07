package br.com.coin_project_ia_bot.presentation.fragments.multi_tff.safe

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.coin_project_ia_bot.R

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SafeTradeFragment : Fragment() {

    private lateinit var adapter: SafeSignalsAdapter

    val viewModel: SafeTradeViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_safe_trade, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SafeSignalsAdapter()

        setupRecyclerView()
        observeViewModel()
        // Atualização automática a cada minuto
        viewModel.startSafeSignalAutoUpdate(120_000)

        lifecycleScope.launch {
            val (resultado, trades) = viewModel.runHistoricalBacktest()
            Log.d("Backtest", "Lucro total: ${resultado.totalTrades}")
        }

    }

    private fun setupRecyclerView() {
        adapter = SafeSignalsAdapter()
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerSafeSignals)
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        recyclerView?.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.safeSignals.observe(viewLifecycleOwner) { signals ->
            adapter.submitList(signals)
        }
    }
}
