package br.com.coin_project_ia_bot

import br.com.coin_project_ia_bot.presentation.fragments.multi_tff.MultiTimeframeViewModel
import br.com.coin_project_ia_bot.presentation.fragments.signal.SignalsViewModel
import br.com.coin_project_ia_bot.presentation.fragments.signal.manually.SharedPairsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<BinanceApi> { BinanceApiImpl() }
    viewModel { MainViewModel() }
    viewModel { MultiTimeframeViewModel() }
    viewModel { SharedPairsViewModel() }
    viewModel { SignalsViewModel(get(), get()) }

}