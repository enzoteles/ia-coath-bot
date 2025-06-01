package br.com.coin_project_ia_bot.presentation.fragments.signal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.coin_project_ia_bot.databinding.FragmentSignalsBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class SignalsFragment : Fragment() {

    private var _binding: FragmentSignalsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: SignalAdapter
    private val singalsViewModel: SignalsViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSignalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.rvSignals.layoutManager = LinearLayoutManager(requireContext())
        adapter = SignalAdapter(emptyList())
        binding.rvSignals.adapter = adapter

        singalsViewModel.signals.observe(viewLifecycleOwner) { list ->
            adapter = SignalAdapter(list)
            binding.rvSignals.adapter = adapter
        }

        singalsViewModel.fetchSignals() // inicia a análise
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
