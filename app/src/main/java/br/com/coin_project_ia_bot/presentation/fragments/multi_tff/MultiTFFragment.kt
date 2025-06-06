package br.com.coin_project_ia_bot.presentation.fragments.multi_tff

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.coin_project_ia_bot.databinding.FragmentMultiTfBinding
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.TickerAnalysis

class MultiTFFragment : Fragment() {

    private var _binding: FragmentMultiTfBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MultiTFViewModel
    private lateinit var adapter: MultiTFAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMultiTfBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[MultiTFViewModel::class.java]
        adapter = MultiTFAdapter()
        binding.rvMultiTF.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMultiTF.adapter = adapter

        viewModel.multiTFData.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.progressLoading.visibility = View.GONE
               // viewModel.analyzeInvestmentDay(List<TickerAnalysis>)
        }

        viewModel.investmentSignal.observe(viewLifecycleOwner) { signal ->
            binding.tvTitle.text = signal
        }

        // Atualiza a cada X minutos (ex: 3 min)
        viewModel.startAutoUpdate(intervalMillis = 3 * 60 * 1000)



    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
