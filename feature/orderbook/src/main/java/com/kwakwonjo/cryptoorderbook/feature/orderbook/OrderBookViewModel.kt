package com.kwakwonjo.cryptoorderbook.feature.orderbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.IsNetworkAvailableUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.ObserveConnectivityUseCase
import com.kwakwonjo.cryptoorderbook.core.domain.usecase.ObserveOrderBookUseCase
import com.kwakwonjo.cryptoorderbook.core.model.ConnectivityStatus
import com.kwakwonjo.cryptoorderbook.core.model.ConnectionState
import com.kwakwonjo.cryptoorderbook.core.model.OrderBook
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = OrderBookViewModel.Factory::class)
class OrderBookViewModel @AssistedInject constructor(
    private val observeOrderBookUseCase: ObserveOrderBookUseCase,
    private val isNetworkAvailableUseCase: IsNetworkAvailableUseCase,
    observeConnectivityUseCase: ObserveConnectivityUseCase,
    @Assisted private val navKey: OrderBookNavKey,
) : ViewModel() {

    private val meta = OrderBookContract.Meta(
        market = navKey.market,
        marketLabel = navKey.label,
    )

    private var lastVisibleState: OrderBookContract.UiState =
        OrderBookContract.UiState.Loading(meta)

    private val refreshTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val connectivityStatus: StateFlow<ConnectivityStatus> = observeConnectivityUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = if (isNetworkAvailableUseCase()) {
                ConnectivityStatus.CONNECTED
            } else {
                ConnectivityStatus.DISCONNECTED
            },
        )

    val uiState: StateFlow<OrderBookContract.UiState> = connectivityStatus
        .flatMapLatest { status ->
            if (status == ConnectivityStatus.DISCONNECTED) {
                emptyFlow()
            } else {
                refreshTrigger
                    .onStart { emit(Unit) }
                    .flatMapLatest {
                        observeOrderBookUiState(
                            emitLoading = lastVisibleState !is OrderBookContract.UiState.Success,
                        )
                    }
            }
        }
        .onEach { state -> lastVisibleState = state }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = lastVisibleState,
        )

    fun retry() {
        refreshTrigger.tryEmit(Unit)
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: OrderBookNavKey): OrderBookViewModel
    }

    private fun observeOrderBookUiState(
        emitLoading: Boolean,
    ): Flow<OrderBookContract.UiState> = flow {
        if (emitLoading) {
            emit(OrderBookContract.UiState.Loading(meta))
        }

        emitAll(
            observeOrderBookUseCase(navKey.market)
                .conflate()
                .scan(initialAccumulator()) { accumulator, payload ->
                    accumulator.reduce(
                        payload = payload,
                        isConnected = connectivityStatus.value == ConnectivityStatus.CONNECTED,
                    )
                }
                .map(OrderBookAccumulator::toUiState)
                .catch {
                    if (connectivityStatus.value == ConnectivityStatus.CONNECTED) {
                        emit(
                            OrderBookContract.UiState.Error(
                                meta = meta,
                                type = OrderBookContract.ErrorType.SOCKET,
                            )
                        )
                    }
                }
        )
    }

    private fun initialAccumulator(): OrderBookAccumulator {
        return when (val state = lastVisibleState) {
            is OrderBookContract.UiState.Success -> OrderBookAccumulator(
                meta = meta,
                orderBook = state.orderBook,
                currentPrice = state.currentPrice,
                signedChangeRate = state.signedChangeRate,
            )

            else -> OrderBookAccumulator(meta = meta)
        }
    }

    private data class OrderBookAccumulator(
        val meta: OrderBookContract.Meta,
        val orderBook: OrderBook? = null,
        val currentPrice: Double? = null,
        val signedChangeRate: Double? = null,
        val errorType: OrderBookContract.ErrorType? = null,
    ) {
        fun reduce(
            payload: OrderBookPayload,
            isConnected: Boolean,
        ): OrderBookAccumulator = when (payload.connectionState) {
            ConnectionState.Error -> if (isConnected) {
                copy(errorType = OrderBookContract.ErrorType.SOCKET)
            } else {
                this
            }

            else -> copy(
                orderBook = payload.orderBook ?: orderBook,
                currentPrice = payload.ticker?.tradePrice ?: currentPrice,
                signedChangeRate = payload.ticker?.signedChangeRate ?: signedChangeRate,
                errorType = null,
            )
        }

        fun toUiState(): OrderBookContract.UiState {
            return when {
                errorType != null -> OrderBookContract.UiState.Error(
                    meta = meta,
                    type = errorType,
                )

                orderBook != null -> OrderBookContract.UiState.Success(
                    meta = meta,
                    orderBook = orderBook,
                    currentPrice = currentPrice,
                    signedChangeRate = signedChangeRate,
                )

                else -> OrderBookContract.UiState.Loading(meta)
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
