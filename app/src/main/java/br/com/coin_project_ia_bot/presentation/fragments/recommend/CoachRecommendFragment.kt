package br.com.coin_project_ia_bot.presentation.fragments.recommend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.coin_project_ia_bot.databinding.FragmentCoachRecommendBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class CoachRecommendFragment : Fragment() {

    private var _binding: FragmentCoachRecommendBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: RecommendAdapter
    private val viewModel: RecommendViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCoachRecommendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = RecommendAdapter(emptyList())
        binding.rvRecommend.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecommend.adapter = adapter

        viewModel.recommendations.observe(viewLifecycleOwner) { list ->
            binding.progressRecommend.visibility = View.GONE
            if (list.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Nenhuma recomendação no momento", Toast.LENGTH_SHORT).show()
            } else {
                adapter = RecommendAdapter(list)
                binding.rvRecommend.adapter = adapter
            }
        }

        binding.progressRecommend.visibility = View.VISIBLE
        viewModel.loadRecommendations()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
