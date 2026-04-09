package com.kwakwonjo.cryptoorderbook.feature.orderbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.IsNetworkAvailableUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.ObserveConnectivityUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.ObserveOrderBookUseCase
import com.kwakwonjo.cryptoorderbook.core.model.ConnectionState
import com.kwakwonjo.cryptoorderbook.core.model.NetworkAvailability
import com.kwakwonjo.cryptoorderbook.core.model.OrderBookPayload
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = OrderBookViewModel.Factory::class)
class OrderBookViewModel @AssistedInject constructor(
    private val observeOrderBookUseCase: ObserveOrderBookUseCase,
    isNetworkAvailableUseCase: IsNetworkAvailableUseCase,
    observeConnectivityUseCase: ObserveConnectivityUseCase,
    @Assisted private val navKey: OrderBookNavKey,
) : ViewModel() {
    private val meta = OrderBookContract.Meta(
        market = navKey.market,
        marketLabel = navKey.label,
    )

    private val initialNetworkAvailability = if (isNetworkAvailableUseCase()) {
        NetworkAvailability.CONNECTED
    } else {
        NetworkAvailability.DISCONNECTED
    }

    private val refreshTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val networkAvailability: StateFlow<NetworkAvailability> = observeConnectivityUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = initialNetworkAvailability,
        )

    private val orderBookFlow: Flow<OrderBookPayload> = refreshTrigger
        .onStart {
            if (initialNetworkAvailability == NetworkAvailability.CONNECTED) {
                emit(Unit)
            }
        }
        .flatMapLatest {
            observeOrderBookUseCase(navKey.market)
        }
        .scan(OrderBookPayload(connectionState = ConnectionState.Connecting)) { previous, payload ->
            // 새로 들어온 데이터가 null이여도 이전 호가 정보 유지
            previous.merge(payload)
        }

    val uiState: StateFlow<OrderBookContract.UiState> = combine(
        orderBookFlow,
        networkAvailability,
    ) { payload, networkAvailability ->
        OrderBookContract.UiState(
            meta = meta,
            content = payload.toContent(),
            uiStatus = when {
                networkAvailability == NetworkAvailability.DISCONNECTED -> OrderBookContract.UiStatus.OFFLINE
                payload.connectionState == ConnectionState.Error -> OrderBookContract.UiStatus.SOCKET_ERROR
                payload.orderBook == null -> OrderBookContract.UiStatus.INITIAL_LOADING
                else -> OrderBookContract.UiStatus.IDLE
            },
        )
    }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = initialUiState(),
        )

    fun refresh() {
        if (networkAvailability.value == NetworkAvailability.CONNECTED) {
            refreshTrigger.tryEmit(Unit)
        }
    }

    fun retry() {
        refresh()
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: OrderBookNavKey): OrderBookViewModel
    }

    private fun OrderBookPayload.merge(
        payload: OrderBookPayload,
    ): OrderBookPayload = copy(
        connectionState = payload.connectionState,
        orderBook = payload.orderBook ?: orderBook,
        ticker = payload.ticker ?: ticker,
        errorMessage = payload.errorMessage,
    )

    private fun OrderBookPayload.toContent(): OrderBookContract.Content? {
        val currentOrderBook = orderBook ?: return null

        return OrderBookContract.Content(
            orderBook = currentOrderBook,
            currentPrice = ticker?.tradePrice,
            signedChangeRate = ticker?.signedChangeRate,
        )
    }

    private fun initialUiState(): OrderBookContract.UiState = OrderBookContract.UiState(
        meta = meta,
        content = null,
        uiStatus = if (initialNetworkAvailability == NetworkAvailability.CONNECTED) {
            OrderBookContract.UiStatus.INITIAL_LOADING
        } else {
            OrderBookContract.UiStatus.OFFLINE
        },
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
