package com.kwakwonjo.cryptoorderbook.feature.orderbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.IsNetworkAvailableUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.ObserveConnectivityUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.ObserveOrderBookUseCase
import com.kwakwonjo.cryptoorderbook.core.model.ConnectionState
import com.kwakwonjo.cryptoorderbook.core.model.NetworkAvailability
import com.kwakwonjo.cryptoorderbook.core.domain.model.OrderBookEvent
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
import kotlinx.coroutines.flow.flowOf
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
    private val marketInfo = OrderBookContract.MarketInfo(
        market = navKey.market,
        marketType = navKey.marketType,
        koreanName = navKey.koreanName,
    )

    private val refreshTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val networkAvailability: StateFlow<NetworkAvailability> = observeConnectivityUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = if (isNetworkAvailableUseCase()) {
                NetworkAvailability.CONNECTED
            } else {
                NetworkAvailability.DISCONNECTED
            },
        )

    private val orderBookFlow: Flow<OrderBookEvent> = combine(
        refreshTrigger.onStart { emit(Unit) },
        networkAvailability
    ) { _, availability -> availability }
        .flatMapLatest { availability ->
            if (availability == NetworkAvailability.CONNECTED) {
                observeOrderBookUseCase(navKey.market, OrderBookUnit)
            } else {
                flowOf(OrderBookEvent(connectionState = ConnectionState.Error))
            }
        }
        .scan(OrderBookEvent(connectionState = ConnectionState.Connecting)) { previous, payload ->
            // 새로 들어온 데이터가 null이여도 이전 호가 정보 유지
            previous.updateWith(payload)
        }

    val uiState: StateFlow<OrderBookContract.UiState> = combine(
        orderBookFlow,
        networkAvailability,
    ) { payload, availability ->
        OrderBookContract.UiState(
            marketInfo = marketInfo,
            orderBookData = payload.toContent(),
            uiStatus = when {
                payload.connectionState == ConnectionState.Error -> OrderBookContract.UiStatus.SOCKET_ERROR
                payload.orderBook == null -> OrderBookContract.UiStatus.INITIAL_LOADING
                // 오프라인도 IDLE 상태로 유지
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

    private fun OrderBookEvent.updateWith(
        payload: OrderBookEvent,
    ): OrderBookEvent = copy(
        connectionState = payload.connectionState,
        orderBook = payload.orderBook ?: orderBook,
        ticker = payload.ticker ?: ticker,
        errorMessage = payload.errorMessage,
    )

    private fun OrderBookEvent.toContent(): OrderBookContract.OrderBookData? {
        val currentOrderBook = orderBook ?: return null

        return OrderBookContract.OrderBookData(
            orderBook = currentOrderBook,
            currentPrice = ticker?.tradePrice,
            signedChangeRate = ticker?.signedChangeRate,
        )
    }

    private fun initialUiState() = OrderBookContract.UiState(
        marketInfo = marketInfo,
        orderBookData = null,
        uiStatus = OrderBookContract.UiStatus.INITIAL_LOADING,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
