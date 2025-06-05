package br.com.coin_project_ia_bot.presentation.fragments.dashboard.chart

import androidx.fragment.app.Fragment
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import br.com.coin_project_ia_bot.databinding.FragmentChartBinding
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.DashboardViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import org.koin.androidx.viewmodel.ext.android.viewModel

// ChartFragment.kt
class ChartFragment : Fragment() {

    private var _binding: FragmentChartBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.analyzedTickers.observe(viewLifecycleOwner) { tickers ->
            if (tickers.isNullOrEmpty()) return@observe

            val top = tickers.take(10)
            val scoreEntries = top.mapIndexed { index, t -> Entry(index.toFloat(), t.score.toFloat()) }
            val rsiEntries = top.mapIndexedNotNull { index, t ->
                t.rsi?.let { Entry(index.toFloat(), it) }
            }

            val scoreDataSet = LineDataSet(scoreEntries, "Score").apply {
                color = Color.BLUE
                setCircleColor(Color.BLUE)
            }

            val rsiDataSet = LineDataSet(rsiEntries, "RSI").apply {
                color = Color.GREEN
                setCircleColor(Color.GREEN)
            }

            val data = LineData(scoreDataSet, rsiDataSet)
            binding.lineChart.data = data
            binding.lineChart.description.text = "Pontuação e RSI das Top Moedas"
            binding.lineChart.invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
