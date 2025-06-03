package br.com.coin_project_ia_bot.presentation.fragments.pump

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.coin_project_ia_bot.databinding.FragmentPumpBinding
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class PumpFragment : Fragment() {

    private var _binding: FragmentPumpBinding? = null
    private val binding get() = _binding!!
    private val pumpViewModel: PumpViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPumpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.rvPump.layoutManager = LinearLayoutManager(requireContext())

        pumpViewModel.pumpList.observe(viewLifecycleOwner) {
            binding.rvPump.adapter = PumpAdapter(it)
        }

        pumpViewModel.fetchPumpCoins(listOf("BTCUSDT", "ETHUSDT", "SOLUSDT", "XRPUSDT", "AVAXUSDT"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
