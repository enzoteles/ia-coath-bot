package br.com.coin_project_ia_bot.presentation.fragments.dashboard.chart

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import br.com.coin_project_ia_bot.databinding.FragmentChartBinding
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.DashboardViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter

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

    override fun onResume() {
        super.onResume()
        viewModel.fetchAndScoreTickers()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChartStyle()

        viewModel.analyzedTickers.observe(viewLifecycleOwner) { tickers ->
            if (tickers.isNullOrEmpty()) {
                Log.w("ChartFragment", "Sem dados para exibir")
                binding.lineChart.clear()
                binding.lineChart.setNoDataText("Sem dados disponíveis.")
                binding.progressLoading.visibility = View.VISIBLE
                binding.lineChart.visibility = View.GONE
                return@observe
            }

            val top = tickers.take(10)
            Log.d("ChartFragment", "Renderizando ${top.size} moedas")

            val scoreEntries = top.mapIndexed { index, t ->
                Log.d("ChartFragment", "Score ${t.ticker.symbol}: ${t.score}")
                Entry(index.toFloat(), t.score)
            }

            val rsiEntries = top.mapIndexedNotNull { index, t ->
                t.rsi?.let {
                    Log.d("ChartFragment", "RSI ${t.ticker.symbol}: $it")
                    Entry(index.toFloat(), it)
                }
            }

            val scoreDataSet = LineDataSet(scoreEntries, "Score").apply {
                setDrawValues(true)
                valueTextSize = 10f
                valueFormatter = object : ValueFormatter() {
                    override fun getPointLabel(entry: Entry?): String {
                        return entry?.y?.toString() ?: ""
                    }
                }
            }
            val rsiDataSet = buildDataSet(rsiEntries, "RSI", Color.GREEN)

            Log.d("ChartFragment", "ScoreEntries: ${scoreDataSet.entryCount}, RSIEntries: ${rsiDataSet.entryCount}")

            binding.lineChart.apply {
                data = LineData(scoreDataSet, rsiDataSet)
                xAxis.valueFormatter = IndexAxisValueFormatter(top.map { it.symbol })
                xAxis.labelCount = top.size
                xAxis.labelRotationAngle = -45f
                invalidate()
                binding.progressLoading.visibility = View.GONE
                binding.lineChart.visibility = View.VISIBLE
            }
        }

    }

    private fun buildDataSet(entries: List<Entry>, label: String, color: Int): LineDataSet {
        return LineDataSet(entries, label).apply {
            setDrawValues(true)
            setDrawCircles(true)
            setCircleColor(color)
            setColor(color)
            lineWidth = 2f
            circleRadius = 4f
            valueTextSize = 10f
        }
    }

    private fun setupChartStyle() {
        binding.lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)
            axisRight.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
            }

            axisLeft.apply {
                granularity = 1f
                setDrawGridLines(true)
            }

            legend.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
