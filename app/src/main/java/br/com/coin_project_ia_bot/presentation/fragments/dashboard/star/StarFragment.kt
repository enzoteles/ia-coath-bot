package br.com.coin_project_ia_bot.presentation.fragments.dashboard.star

import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.coin_project_ia_bot.databinding.FragmentDashboardBinding
import br.com.coin_project_ia_bot.databinding.FragmentStarBinding
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.DashboardAdapter
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.DashboardViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class StarFragment : Fragment() {

    private var _binding: FragmentStarBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: DashboardAdapter

    private val mainViewModel: DashboardViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvDashboard.layoutManager = LinearLayoutManager(requireContext())
        adapter = DashboardAdapter(emptyList()) // Começa vazio
        binding.rvDashboard.adapter = adapter

        // Observa a nova lista com análise
        mainViewModel.analyzedTickers.observe(viewLifecycleOwner) { analysisList ->
            if (analysisList.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Nenhuma moeda com pontuação", Toast.LENGTH_SHORT).show()
            } else {
                adapter.updateList(analysisList)
            }

            binding.progressLoading.visibility = View.GONE
        }

        binding.progressLoading.visibility = View.VISIBLE
        mainViewModel.fetchAndScoreTickers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

