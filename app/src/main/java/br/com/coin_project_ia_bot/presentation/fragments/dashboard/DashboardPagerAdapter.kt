package br.com.coin_project_ia_bot.presentation.fragments.dashboard

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.chart.ChartFragment
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.star.StarFragment

// DashboardPagerAdapter.kt
class DashboardPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> StarFragment()
            1 -> ChartFragment()
            else -> throw IllegalArgumentException("Invalid tab position")
        }
    }
}
