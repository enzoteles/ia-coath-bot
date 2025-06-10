package br.com.coin_project_ia_bot.presentation.fragments.recommend.top_coin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.coin_project_ia_bot.databinding.FragmentTopCoinsBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class TopCoinsFragment : Fragment() {

    private var _binding: FragmentTopCoinsBinding? = null
    private val binding get() = _binding!!
    private val topCoinViewModel: TopCoinsViewModel by viewModel()
    private lateinit var adapter: TopCoinsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTopCoinsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        topCoinViewModel.getTop3ForInvestment()
    }

    private fun setupRecyclerView() {
        adapter = TopCoinsAdapter()
        binding.recyclerTopCoins.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTopCoins.adapter = adapter
    }

    private fun observeViewModel() {
        topCoinViewModel.topCoins.observe(viewLifecycleOwner) { coins ->
            adapter.submitList(coins)
            binding.tvMessage.text = if (coins.isEmpty()) "Nenhuma moeda com alta confiabilidade." else "Top 3 criptos para hoje"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}