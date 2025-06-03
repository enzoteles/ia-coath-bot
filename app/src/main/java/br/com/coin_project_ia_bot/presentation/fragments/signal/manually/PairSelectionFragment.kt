package br.com.coin_project_ia_bot.presentation.fragments.signal.manually

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import br.com.coin_project_ia_bot.databinding.FragmentPairSelectionBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

// 1. Tela de seleção de pares com checkboxes

class PairSelectionFragment : Fragment() {

    private var _binding: FragmentPairSelectionBinding? = null
    private val binding get() = _binding!!

    // Compartilhado com SignalsViewModel
    private val pairViewModel: SharedPairsViewModel by viewModel()

    // Lista fixa de pares populares — você pode substituir por uma lista dinâmica
    private val pares = listOf(
        "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT",
        "DOGEUSDT", "AVAXUSDT", "MATICUSDT", "LTCUSDT", "ADAUSDT"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPairSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Recupera os pares selecionados previamente
        val selecionados = pairViewModel.selectedPairs.value ?: emptyList()

        // Adiciona dinamicamente os CheckBoxes
        pares.forEach { par ->
            val checkBox = CheckBox(requireContext()).apply {
                text = par
                isChecked = selecionados.contains(par)
            }
            binding.pairsContainer.addView(checkBox)
        }

        // Ação do botão Salvar
        binding.btnSalvarPares.setOnClickListener {
            val paresSelecionados = mutableListOf<String>()

            for (i in 0 until binding.pairsContainer.childCount) {
                val view = binding.pairsContainer.getChildAt(i)
                if (view is CheckBox && view.isChecked) {
                    paresSelecionados.add(view.text.toString())
                }
            }

            // Atualiza o ViewModel compartilhado
            pairViewModel.setSelectedPairs(paresSelecionados)

            Toast.makeText(requireContext(), "Pares atualizados com sucesso", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
