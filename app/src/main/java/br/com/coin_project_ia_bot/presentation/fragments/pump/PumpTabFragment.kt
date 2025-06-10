package br.com.coin_project_ia_bot.presentation.fragments.pump

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import br.com.coin_project_ia_bot.databinding.FragmentPumpTabBinding
import br.com.coin_project_ia_bot.presentation.fragments.pump.alert.PumpAlertFragment
import com.google.android.material.tabs.TabLayoutMediator

class PumpTabFragment: Fragment() {

    private var _binding: FragmentPumpTabBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPumpTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragments = listOf(
            PumpFragment(),
            PumpAlertFragment()
        )
        val titles = listOf("Pump Notification", "Pump ALert")

        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = fragments.size
            override fun createFragment(position: Int) = fragments[position]
        }

        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}