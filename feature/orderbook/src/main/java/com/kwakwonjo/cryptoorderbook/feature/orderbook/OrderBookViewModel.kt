package com.kwakwonjo.cryptoorderbook.feature.orderbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.ObserveOrderBookUseCase
import com.kwakwonjo.cryptoorderbook.core.model.ConnectionState
import com.kwakwonjo.cryptoorderbook.core.model.OrderBook
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = OrderBookViewModel.Factory::class)
class OrderBookViewModel @AssistedInject constructor(
    private val observeOrderBookUseCase: ObserveOrderBookUseCase,
    @Assisted private val navKey: OrderBookNavKey,
) : ViewModel() {

    private val _uiState = MutableStateFlow(connectingState())
    val uiState: StateFlow<OrderBookUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        observeOrderBook()
    }

    fun retry() {
        observeOrderBook(resetState = true)
    }

    override fun onCleared() {
        observeJob?.cancel()
        super.onCleared()
    }

    private fun observeOrderBook(resetState: Boolean = false) {
        observeJob?.cancel()
        if (resetState) {
            _uiState.value = connectingState()
        }

        observeJob = viewModelScope.launch {
            observeOrderBookUseCase(navKey.market)
                .conflate()
                .catch { throwable ->
                    _uiState.value = errorState(
                        throwable.message ?: DEFAULT_ERROR_MESSAGE,
                    )
                }
                .collect { payload ->
                    _uiState.value = when (payload.connectionState) {
                        ConnectionState.Error -> errorState(
                            payload.errorMessage ?: DEFAULT_ERROR_MESSAGE,
                        )

                        ConnectionState.Connecting -> connectingState()
                        ConnectionState.Idle -> idleState()
                        ConnectionState.Connected -> _uiState.value.copy(
                            market = navKey.market,
                            marketLabel = navKey.label,
                            connectionState = ConnectionState.Connected,
                            orderBook = payload.orderBook ?: _uiState.value.orderBook,
                            currentPrice = payload.ticker?.tradePrice ?: _uiState.value.currentPrice,
                            signedChangeRate = payload.ticker?.signedChangeRate ?: _uiState.value.signedChangeRate,
                            errorMessage = null,
                        )
                    }
                }
        }
    }

    private fun idleState(): OrderBookUiState = OrderBookUiState(
        market = navKey.market,
        marketLabel = navKey.label,
        connectionState = ConnectionState.Idle,
    )

    private fun connectingState(): OrderBookUiState = OrderBookUiState(
        market = navKey.market,
        marketLabel = navKey.label,
        connectionState = ConnectionState.Connecting,
    )

    private fun errorState(message: String): OrderBookUiState = OrderBookUiState(
        market = navKey.market,
        marketLabel = navKey.label,
        connectionState = ConnectionState.Error,
        errorMessage = message,
    )

    @AssistedFactory
    interface Factory {
        fun create(navKey: OrderBookNavKey): OrderBookViewModel
    }

    private companion object {
        const val DEFAULT_ERROR_MESSAGE = "WebSocket 연결에 실패했습니다."
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
