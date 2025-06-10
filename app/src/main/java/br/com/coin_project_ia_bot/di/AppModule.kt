package br.com.coin_project_ia_bot.di

import br.com.coin_project_ia_bot.data.api.BinanceApi
import br.com.coin_project_ia_bot.data.repository.BinanceApiImpl
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.DashboardViewModel
import br.com.coin_project_ia_bot.presentation.fragments.multi_tff.MultiTFViewModel
import br.com.coin_project_ia_bot.presentation.fragments.multi_tff.MultiTimeframeViewModel
import br.com.coin_project_ia_bot.presentation.fragments.pump.PumpViewModel
import br.com.coin_project_ia_bot.presentation.fragments.pump.alert.PumpAlertViewModel
import br.com.coin_project_ia_bot.presentation.fragments.recommend.RecommendViewModel
import br.com.coin_project_ia_bot.presentation.fragments.recommend.swing_top_coin.SwingCoinsViewModel
import br.com.coin_project_ia_bot.presentation.fragments.recommend.top_coin.TopCoinsViewModel
import br.com.coin_project_ia_bot.presentation.fragments.signal.SignalsViewModel
import br.com.coin_project_ia_bot.presentation.fragments.signal.manually.SharedPairsViewModel
import br.com.coin_project_ia_bot.presentation.utils.TrendAIAnalyzer
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<BinanceApi> { BinanceApiImpl() }
    viewModel { SharedPairsViewModel() }
    viewModel { DashboardViewModel() }
    viewModel { MultiTimeframeViewModel() }
    viewModel { MultiTFViewModel(get()) }
    viewModel { SignalsViewModel(get(), get()) }
    viewModel { PumpViewModel(get()) }
    single { PumpAlertViewModel(get()) }
    single { TrendAIAnalyzer(get()) }
    viewModel { RecommendViewModel(get(), get()) }
    viewModel { TopCoinsViewModel() }
    viewModel { SwingCoinsViewModel() }
}