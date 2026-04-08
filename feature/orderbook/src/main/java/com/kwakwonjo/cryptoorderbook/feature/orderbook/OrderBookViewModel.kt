package com.kwakwonjo.cryptoorderbook.feature.orderbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.ObserveOrderBookUseCase
import com.kwakwonjo.cryptoorderbook.core.model.ConnectionState
import com.kwakwonjo.cryptoorderbook.core.model.OrderBook
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OrderBookViewModel @Inject constructor(
    private val observeOrderBookUseCase: ObserveOrderBookUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderBookUiState())
    val uiState: StateFlow<OrderBookUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var currentMarket: String? = null
    private var currentMarketLabel: String = ""

    fun start(market: String, marketLabel: String) {
        if (currentMarket == market && observeJob?.isActive == true) {
            return
        }

        currentMarket = market
        currentMarketLabel = marketLabel
        observeJob?.cancel()
        _uiState.value = OrderBookUiState(
            market = market,
            marketLabel = marketLabel,
            connectionState = ConnectionState.Connecting,
        )

        observeJob = viewModelScope.launch {
            observeOrderBookUseCase(market).conflate().collect { payload ->
                _uiState.value = _uiState.value.copy(
                    market = market,
                    marketLabel = marketLabel,
                    connectionState = payload.connectionState,
                    orderBook = payload.orderBook ?: _uiState.value.orderBook,
                    currentPrice = payload.ticker?.tradePrice ?: _uiState.value.currentPrice,
                    signedChangeRate = payload.ticker?.signedChangeRate ?: _uiState.value.signedChangeRate,
                    errorMessage = payload.errorMessage,
                )
            }
        }
    }

    fun retry() {
        val market = currentMarket ?: return
        start(market = market, marketLabel = currentMarketLabel)
    }

    fun stop() {
        observeJob?.cancel()
        observeJob = null
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}

data class OrderBookUiState(
    val market: String = "",
    val marketLabel: String = "",
    val connectionState: ConnectionState = ConnectionState.Idle,
    val currentPrice: Double? = null,
    val signedChangeRate: Double? = null,
    val orderBook: OrderBook? = null,
    val errorMessage: String? = null,
)

