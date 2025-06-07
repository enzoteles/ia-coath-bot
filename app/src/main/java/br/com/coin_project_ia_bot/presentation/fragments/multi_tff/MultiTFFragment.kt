package br.com.coin_project_ia_bot.presentation.fragments.multi_tff

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.coin_project_ia_bot.databinding.FragmentMultiTfBinding
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.DashboardViewModel
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.TickerAnalysis
import org.koin.androidx.viewmodel.ext.android.viewModel

class MultiTFFragment : Fragment() {

    private var _binding: FragmentMultiTfBinding? = null
    private val binding get() = _binding!!

    private val multiViewModel: MultiTFViewModel by viewModel()
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

        adapter = MultiTFAdapter()
        binding.rvMultiTF.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMultiTF.adapter = adapter

        /*multiViewModel.multiTFData.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.progressLoading.visibility = View.GONE
        }*/

        multiViewModel.multiTFData.observe(viewLifecycleOwner) { list ->
            val filteredList = list.filter { it.consistency == "📈 Boa Tendência" }
            adapter.submitList(filteredList)
            binding.progressLoading.visibility = View.GONE
            if(filteredList.isEmpty()) binding.tvMsg.visibility = View.VISIBLE
        }


        multiViewModel.analyzedTickers.observe(viewLifecycleOwner) { analysisList ->
            //binding.progressLoading.visibility = View.GONE
            Handler(Looper.getMainLooper()).postDelayed( {
                multiViewModel.analyzeInvestmentDay(analysisList)
            },600)
        }



        // Atualiza a cada X minutos (ex: 3 min)
        multiViewModel.startAutoUpdate(intervalMillis = 3 * 60 * 1000)
        Handler(Looper.getMainLooper()).postDelayed( {
            // Chamada após o app estar 100% pronto
            multiViewModel.fetchAndScoreTickers()
        },500)

        multiViewModel.investmentSignal.observe(viewLifecycleOwner) { signal ->
            binding.tvTitle.text = signal
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
