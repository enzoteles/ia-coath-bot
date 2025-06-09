package br.com.coin_project_ia_bot.presentation.fragments.signal.manually

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import android.util.Log
import androidx.lifecycle.*
import br.com.coin_project_ia_bot.RetrofitInstance
import br.com.coin_project_ia_bot.presentation.MainActivity.Companion.USDT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val QTD_COIN = 100
class SharedPairsViewModel : ViewModel() {

    private val _selectedPairs = MutableLiveData<List<String>>()
    val selectedPairs: LiveData<List<String>> get() = _selectedPairs

    init {
        fetchTop50Pairs()
    }

    private fun fetchTop50Pairs() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitInstance.api.getTickers()
                val topPairs = response
                    .filter { it.symbol.endsWith(USDT) }
                    .sortedByDescending { it.volume.toFloatOrNull() ?: 0f }
                    .take(QTD_COIN)
                    .map { it.symbol }

                _selectedPairs.postValue(topPairs)

            } catch (e: Exception) {
                Log.e("SharedPairsViewModel", "Erro ao buscar pares: ${e.localizedMessage}")
                // fallback default
                _selectedPairs.postValue(
                    listOf("BTCUSDT", "ETHUSDT", "SOLUSDT", "XRPUSDT", "AVAXUSDT")
                )
            }
        }
    }

    fun setSelectedPairs(pairs: List<String>) {
        _selectedPairs.value = pairs
    }

    fun togglePair(pair: String) {
        val current = _selectedPairs.value?.toMutableList() ?: mutableListOf()
        if (current.contains(pair)) {
            current.remove(pair)
        } else {
            current.add(pair)
        }
        _selectedPairs.value = current
    }
}
