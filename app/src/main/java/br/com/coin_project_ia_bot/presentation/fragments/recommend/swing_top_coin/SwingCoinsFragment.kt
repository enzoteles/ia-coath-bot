package br.com.coin_project_ia_bot.presentation.fragments.recommend.swing_top_coin

import br.com.coin_project_ia_bot.databinding.FragmentSwingCoinsBinding
import br.com.coin_project_ia_bot.presentation.fragments.recommend.top_coin.TopCoinsAdapter

// SwingCoinsFragment.kt
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import org.koin.androidx.viewmodel.ext.android.viewModel

class SwingCoinsFragment : Fragment() {

    private var _binding: FragmentSwingCoinsBinding? = null
    private val binding get() = _binding!!
    private val swingViewModel: SwingCoinsViewModel by viewModel()
    private lateinit var adapter: TopCoinsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSwingCoinsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        swingViewModel.getTop3ForSwing()
    }

    private fun setupRecyclerView() {
        adapter = TopCoinsAdapter()
        binding.recyclerSwingCoins.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSwingCoins.adapter = adapter
    }

    private fun observeViewModel() {
        swingViewModel.swingCoins.observe(viewLifecycleOwner) { coins ->
            adapter.submitList(coins)
            binding.tvMessage.text = if (coins.isEmpty()) "Nenhuma oportunidade para swing trade." else "Top 3 moedas para Swing Trade"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
