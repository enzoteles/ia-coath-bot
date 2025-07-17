package br.com.coin_project_ia_bot.presentation.fragments.multi_tff.strategy

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import br.com.coin_project_ia_bot.databinding.FragmentMarketStrategyBinding
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.DashboardAdapter
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.DashboardViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class MarketStrategyFragment : Fragment() {

    private var _binding: FragmentMarketStrategyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MarketStrategyViewModel by viewModels()
    private val dashboardViewModel: DashboardViewModel by sharedViewModel()
    private lateinit var adapter: DashboardAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarketStrategyBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DashboardAdapter(emptyList())
        binding.rvSinais.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSinais.adapter = adapter

        binding.btnGerarEstrategia.setOnClickListener {
            dashboardViewModel.fetchAndScoreTickers()
            dashboardViewModel.analyzedTickers.observe(viewLifecycleOwner) { tickers ->
                //principal logica
                viewModel.analisarMercadoEAtualizar(tickers)

                //lógica segundária
                /*val tickersReal = dashboardViewModel.analyzedTickers.value.orEmpty().associateBy { it.symbol }

                val json = viewModel.gerarRespostaIAFake() // ou simularIAJsonEmTempoReal()
                val sinaisFiltrados = processarJsonDaIA(json, tickersReal)

                viewModel.analisarMercadoEAtualizar(sinaisFiltrados)*/

            }
        }

        binding.progressLoading.visibility = View.VISIBLE

        viewModel.sinaisFiltrados.observe(viewLifecycleOwner, Observer {
            adapter.updateList(it)
            binding.progressLoading.visibility = View.GONE
        })

        viewModel.perguntaGerada.observe(viewLifecycleOwner, Observer { pergunta ->
            if (pergunta.isNotBlank()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Pergunta para IA")
                    .setMessage(pergunta)
                    .setPositiveButton("Copiar") { _, _ ->
                        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Pergunta", pergunta)
                        clipboard.setPrimaryClip(clip)
                    }
                    .setNegativeButton("Fechar", null)
                    .show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
