package br.com.coin_project_ia_bot.presentation.fragments.signal.manually

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedPairsViewModel : ViewModel() {

    private val _selectedPairs = MutableLiveData<List<String>>().apply {
        value = listOf("BTCUSDT", "ETHUSDT", "SOLUSDT", "XRPUSDT", "AVAXUSDT")
    }
    val selectedPairs: LiveData<List<String>> = _selectedPairs

    fun setSelectedPairs(pairs: List<String>) {
        _selectedPairs.value = pairs
    }

    fun togglePair(pair: String) {
        val current = _selectedPairs.value ?: setOf()
        _selectedPairs.value = if (current.contains(pair)) {
            current - pair
        } else {
            current + pair
        }
    }

}

