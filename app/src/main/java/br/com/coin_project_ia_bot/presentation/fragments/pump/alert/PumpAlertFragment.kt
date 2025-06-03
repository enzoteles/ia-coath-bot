package br.com.coin_project_ia_bot.presentation.fragments.pump.alert

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.coin_project_ia_bot.databinding.FragmentPumpAlertBinding
import br.com.coin_project_ia_bot.presentation.fragments.pump.PumpAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel

class PumpAlertFragment : Fragment() {

    private var _binding: FragmentPumpAlertBinding? = null
    private val binding get() = _binding!!

    private val pumpAlertviewModel: PumpAlertViewModel by viewModel()
    private lateinit var adapter: PumpAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPumpAlertBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = PumpAdapter(emptyList())
        binding.rvPumpAlerts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPumpAlerts.adapter = adapter

        binding.progressPump.visibility = View.VISIBLE

        pumpAlertviewModel.alerts.observe(viewLifecycleOwner) { list ->
            binding.progressPump.visibility = View.GONE
            if (list.isNullOrEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvPumpAlerts.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.rvPumpAlerts.visibility = View.VISIBLE
                adapter = PumpAdapter(list)
                binding.rvPumpAlerts.adapter = adapter
            }
        }

        pumpAlertviewModel.checkPumps(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
