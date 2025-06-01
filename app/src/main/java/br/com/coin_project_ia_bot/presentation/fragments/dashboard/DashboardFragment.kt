package br.com.coin_project_ia_bot.presentation.fragments.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.coin_project_ia_bot.MainViewModel
import androidx.fragment.app.viewModels
import br.com.coin_project_ia_bot.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    // Binding gerado automaticamente (ViewBinding)
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // Declare o Adapter aqui
    private lateinit var adapter: CoinCoachAdapter

    // Declare o ViewModel (você pode usar ViewModelProvider ou KTX)
    private val viewModel: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Infla o layout via ViewBinding
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configura RecyclerView com LayoutManager e Adapter vazio inicialmente
        binding.rvDashboard.layoutManager = LinearLayoutManager(requireContext())
        adapter = CoinCoachAdapter(emptyList())
        binding.rvDashboard.adapter = adapter

        // Observa os dados do ViewModel
        viewModel.tickersLiveData.observe(viewLifecycleOwner, Observer { scoredList ->
            if (scoredList.isNullOrEmpty()) {
                // Se quiser, exiba mensagem de nenhum dado
                Toast.makeText(requireContext(), "Nenhuma moeda pontuada no momento", Toast.LENGTH_SHORT).show()
            }
            // Atualiza o adapter com a nova lista
            adapter = CoinCoachAdapter(scoredList)
            binding.rvDashboard.adapter = adapter

            // Esconde o ProgressBar
            binding.progressLoading.visibility = View.GONE
        })

        // Exibe ProgressBar enquanto carrega
        binding.progressLoading.visibility = View.VISIBLE

        // Chama o ViewModel para buscar e pontuar os tickers
        viewModel.fetchAndScoreTickers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
