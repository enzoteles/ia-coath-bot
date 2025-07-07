package br.com.coin_project_ia_bot.presentation.fragments.dashboard.star

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.coin_project_ia_bot.databinding.FragmentDashboardBinding
import br.com.coin_project_ia_bot.databinding.FragmentStarBinding
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.DashboardAdapter
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.DashboardViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class StarFragment : Fragment() {

    private var _binding: FragmentStarBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: DashboardAdapter

    private val mainViewModel: DashboardViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStarBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvDashboard.layoutManager = LinearLayoutManager(requireContext())
        adapter = DashboardAdapter(emptyList()) // Começa vazio
        binding.rvDashboard.adapter = adapter


        // Observa a nova lista com análise
        /*mainViewModel.analyzedTickers.observe(viewLifecycleOwner) { allTickers ->

            val highConfidenceList = allTickers
                .filter { it.consistency == "Alta Consistência ✅" }
                .sortedByDescending { it.score }

            if (highConfidenceList.isEmpty()) {
                binding.tvMsgErro.visibility = View.VISIBLE
                binding.rvDashboard.visibility = View.GONE
            } else {
                binding.tvMsgErro.visibility = View.GONE
                binding.rvDashboard.visibility = View.VISIBLE
                adapter.updateList(highConfidenceList)
            }
            binding.progressLoading.visibility = View.GONE
        }*/

        mainViewModel.analyzedTickers.observe(viewLifecycleOwner) { allTickers ->

            if (allTickers.isEmpty()) {
                binding.tvMsgErro.text = "⚠️ O mercado está em queda ou sem oportunidades seguras no momento. Sugerimos manter em Stablecoin (ex: USDT)."
                binding.tvMsgErro.visibility = View.GONE
                binding.rvDashboard.visibility = View.GONE
            } else {
                binding.tvMsgErro.text = ""
                binding.tvMsgErro.visibility = View.GONE
                binding.rvDashboard.visibility = View.GONE
                adapter.updateList(allTickers)
            }
            binding.progressLoading.visibility = View.GONE
        }

        mainViewModel.resumoEstrategia.observe(viewLifecycleOwner) { resumo ->
            binding.tvResumoEstrategia.text = resumo
            binding.tvResumoEstrategia.visibility = View.VISIBLE
            binding.layoutButtons.visibility = View.VISIBLE

        }


        binding.progressLoading.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed( {
            // Chamada após o app estar 100% pronto
            mainViewModel.fetchAndScoreTickers()
        },1000)

        /*binding.btnGerarPergunta.setOnClickListener {
            val pergunta = mainViewModel.gerarPerguntaParaIA(mainViewModel.analyzedTickers.value ?: emptyList())
            AlertDialog.Builder(requireContext())
                .setTitle("Pergunta para IA")
                .setMessage(pergunta)
                .setPositiveButton("OK", null)
                .show()
        }

        */


        binding.btnGerarPerguntaHedge.setOnClickListener {
            val pergunta = mainViewModel.gerarPerguntaDeNivel99Hedge(mainViewModel.analyzedTickers.value ?: emptyList())
            AlertDialog.Builder(requireContext())
                .setTitle("Pergunta de Nível 99%")
                .setMessage(pergunta)
                .setPositiveButton("OK", null)
                .show()
        }

        binding.btnPerguntaMutual.setOnClickListener {
            val pergunta = mainViewModel.gerarPerguntaNivel99IAMutual(mainViewModel.analyzedTickers.value ?: emptyList())
            AlertDialog.Builder(requireContext())
                .setTitle("Pergunta Nível 99% para IA")
                .setMessage(pergunta)
                .setPositiveButton("Copiar") { _, _ ->
                    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("PerguntaIA", pergunta)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(requireContext(), "Pergunta copiada!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Fechar", null)
                .show()
        }


        binding.btnGerarPerguntaEstrategia.setOnClickListener {
            val config = mainViewModel.configuracaoAtual.value
            val volatilidade = mainViewModel.volatilidadeAtual.value ?: "Indefinida"

            if (config == null) {
                Toast.makeText(requireContext(), "Configuração ainda não carregada.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val pergunta = mainViewModel.gerarPerguntaIAComBaseNaEstrategia(config, volatilidade)

            AlertDialog.Builder(requireContext())
                .setTitle("Pergunta para IA")
                .setMessage(pergunta)
                .setPositiveButton("Copiar") { _, _ ->
                    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Pergunta IA", pergunta)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(requireContext(), "Pergunta copiada com sucesso!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Fechar", null)
                .show()
        }

        binding.btnAbrirChatGPT.setOnClickListener {
            val config = mainViewModel.configuracaoAtual.value
            val volatilidade = mainViewModel.volatilidadeAtual.value ?: "Indefinida"

            if (config == null) {
                Toast.makeText(requireContext(), "A estratégia ainda não foi definida.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val pergunta = mainViewModel.gerarPerguntaIAComBaseNaEstrategia(config, volatilidade)
            val promptEncoded = Uri.encode(pergunta)
            val url = "https://chat.openai.com/?prompt=$promptEncoded"

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

