package br.com.coin_project_ia_bot.presentation.fragments.multi_tff


/**
Ótimo! Vamos criar o fragmento chamado MultiTimeframeFragment, que exibirá:
A variação percentual de cada moeda nos tempos: 1 min, 5 min, 10 min e 1 hora.
Detecção de 3 candles de alta consecutivos como indicador de tendência consistente.
 */
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.coin_project_ia_bot.databinding.FragmentMultiTfBinding

class MultiTimeFragment : Fragment() {

    private var _binding: FragmentMultiTfBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MultiTimeAdapter
    private val viewModel: MultiTimeframeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMultiTfBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvMultiTF.layoutManager = LinearLayoutManager(requireContext())
        adapter = MultiTimeAdapter(emptyList())
        binding.rvMultiTF.adapter = adapter

        // Observa os dados
        viewModel.multiTimeData.observe(viewLifecycleOwner, Observer { list ->
            if (list.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Sem dados de tendência.", Toast.LENGTH_SHORT).show()
            } else {
                adapter = MultiTimeAdapter(list)
                binding.rvMultiTF.adapter = adapter
            }
            binding.progressLoading.visibility = View.GONE
        })

        // Carrega os dados
        binding.progressLoading.visibility = View.VISIBLE
        viewModel.loadMultiTimeDataFromTopSymbols()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
