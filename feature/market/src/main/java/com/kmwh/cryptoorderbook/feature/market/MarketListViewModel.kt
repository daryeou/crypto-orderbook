package com.kmwh.cryptoorderbook.feature.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmwh.cryptoorderbook.core.domain.usecase.ObserveMarketSummariesUseCase
import com.kmwh.cryptoorderbook.core.model.MarketSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MarketListViewModel @Inject constructor(
    private val observeMarketSummariesUseCase: ObserveMarketSummariesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MarketListUiState>(MarketListUiState.Loading)
    val uiState: StateFlow<MarketListUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    fun startPolling() {
        if (pollingJob?.isActive == true) return

        pollingJob = viewModelScope.launch {
            _uiState.value = MarketListUiState.Loading

            observeMarketSummariesUseCase()
                .catch { throwable ->
                    _uiState.value = MarketListUiState.Error(
                        throwable.message ?: DEFAULT_ERROR_MESSAGE,
                    )
                }
                .collect { markets ->
                    _uiState.value = MarketListUiState.Success(markets)
                }
        }
    }

    fun retry() {
        stopPolling()
        startPolling()
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onCleared() {
        stopPolling()
        super.onCleared()
    }

    private companion object {
        const val DEFAULT_ERROR_MESSAGE = "마켓 목록을 불러오지 못했습니다."
    }
}

sealed interface MarketListUiState {
    data object Loading : MarketListUiState
    data class Success(val markets: List<MarketSummary>) : MarketListUiState
    data class Error(val message: String) : MarketListUiState
}
